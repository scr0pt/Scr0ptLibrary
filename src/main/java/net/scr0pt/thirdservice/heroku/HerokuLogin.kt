package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Updates
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.*
import org.bson.types.ObjectId
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver


fun randomAppname(prefix: String? = null): String {
    var appName = (if (prefix == null) "bruce-" else prefix) + ObjectId().toString()
    while (appName.length > 30) appName = appName.substring(0, appName.length - 1)
    return appName
}


suspend fun main() {
    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"
    val infinityMail = InfinityMail(gmailUsername)

    herokuCollection.find().forEach { doc ->
        val email = doc.getString("email")
        val password = doc.getString("password")

        if (email.replace(".", "").substringBeforeLast("@") == infinityMail.username.replace(".", "").substringBeforeLast("@")) return@forEach

        var newEmail: String? = null
        do {
            val newImail = infinityMail.getNext() ?: return@forEach
            newEmail = newImail.fullAddress
        } while (newEmail == null || newImail.username == infinityMail.username || herokuCollection.countDocuments(org.bson.Document("email", newEmail)) > 0)

        val driver = Browser.chrome
        val gmail = Gmail(gmailUsername, gmailPassword)
        val startTime = System.currentTimeMillis()
        val herokuDashboardPage = HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.GO_TO_ACCOUNT) {
            println("HerokuDashboardPage click first app")
        }
        val pageManager = PageManager(arrayListOf(
                HerokuLoginPage(email, password) {
                    println("HerokuLoginPage success")
                }, herokuDashboardPage
                ,
                HerokuAccountPage(action = HerokuAccountPage.HerokuAccountAction.CHANGE_EMAIL(newEmail)) {
                    println("HerokuAccountPage success")
                },
                HerokuAccountConfirmPasswordDonePage() {
                    println("HerokuAccountConfirmPasswordDonePage success")
                }
        ), driver, "https://id.heroku.com/login")

        pageManager.pageList.add(HerokuAccountConfirmPasswordPage(password) {
            println("HerokuAccountConfirmPasswordPage success")
            installGmail(gmail, driver, pageManager, startTime) { confirmLink ->
                println("confirmLink: $confirmLink")
                driver.get(confirmLink)
                gmail.logout()
                herokuDashboardPage.onPageDetect = {
                    pageManager.success()
                }
            }
        })

        pageManager.run {
            if (it is PageResponse.OK) {
                herokuCollection.updateOne(org.bson.Document("email", email), Updates.combine(
                        Updates.set("original_email", email),
                        Updates.set("email", newEmail)
                ))
            }
            println("Login Heroku response $it")
            driver.close()
        }
    }
}

fun installGmail(gmail: Gmail, driver: ChromeDriver, pageManager: PageManager, startTime: Long, onSuccess: (String) -> Unit) {
    gmail.onEvent(
            MailReceiveEvent(
                    key = "ona1sender",
                    validator = { mail ->
                        (mail.id ?: 0) > startTime &&
                                Mail.CompareType.EQUAL_IGNORECASE.compare(
                                        mail.from,
                                        "noreply@heroku.com"
                                ) &&
                                Mail.CompareType.EQUAL_IGNORECASE.compare(
                                        mail.subject,
                                        "Confirm Heroku Account Email Change"
                                )
                    },
                    callback = { mails ->
                        val mail =
                                mails.firstOrNull { it.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/email/confirm/']") != null }
                        val confirmLink = mail?.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/email/confirm/']")?.attr("href")
                        if (confirmLink != null) {
                            onSuccess(confirmLink)
                        }
                    },
                    once = false,
                    new = true,
                    fetchContent = true
            )
    )
}


suspend fun main2() {
    val collaboratorEmail = "alphahoai@gmail.com"
//    val collaboratorEmail = "brucealmighty5daeae612ce205583fda39d5@gmail.com"
    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

//    herokuCollection.random(org.bson.Document())?.let {
    herokuCollection.find()?.forEach {
        if (it.getList("collaborators", String::class.java)?.contains(collaboratorEmail) == true) return@forEach

        val email = it.getString("email")
        val password = it.getString("password")

        println("$email ~ $password")
        var appName = randomAppname()
        val driver = Browser.chrome

        PageManager(arrayListOf(
                HerokuLoginPage(email, password) {
                    println("HerokuLoginPage success")
                }, HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CLICK_FIRST_APP) {
            println("HerokuDashboardPage click first app")
        }
        ), driver,
                "https://id.heroku.com/login")
                .run { pageresponse ->
                    println(pageresponse)
                    if (pageresponse is PageResponse.OK) {

                        driver.get(driver.currentUrlTrim + "/settings")
                        if (it.getString("appName").startsWith("bruce") == false) {
                            driver.findFirstElWait(1000, 60000, "#app-rename-input", jsoup = false)?.click()
                            driver.findFirstElWait(1000, 120000, "input.input-reset.lh-copy")?.let {
                                it.clear()
                                it.sendKeys(appName)
                            }
                            driver.findFirstElWait(1000, 120000, ".__hk-inline-edit-submit-button__.async-button")?.click()
                            driver.waitUntilUrlChange(1000, 120000)
                        }


                        driver.get(driver.currentUrlTrim!!.substringBeforeLast("/") + "/access")
                        val addCollaboratorBtns =
                                driver.findFirstElWait(1000, 60000, "button", jsoup = false, filter = { el -> el.text.trim().equals("Add collaborator", ignoreCase = true) })

                        addCollaboratorBtns?.click()
                        println("passwordConfirmInputs")
                        val passwordConfirmInputs = driver.findFirstElWait(1000, 60000, "input", jsoup = false, filter = { el -> el.getAttribute("placeholder") == "user@domain.com" })

                        passwordConfirmInputs?.sendKeys(collaboratorEmail)
                        println("saveChangesBtns")
                        val saveChangesBtns =
                                driver.findFirstElWait(1000, 60000, "button", jsoup = false, filter = { el -> el.text.trim().equals("Save changes", ignoreCase = true) })
                        saveChangesBtns?.click()

                        driver.findElWait(1000, 180000, ".collaborator-item", filter = { el -> el.text.contains(collaboratorEmail) })

                        val nameOfApp = driver.currentUrlTrim!!.substringBeforeLast("/access").substringAfterLast("/")
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.push("collaborators", collaboratorEmail)
                        )
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.set("appName", nameOfApp)
                        )
                        Thread.sleep(5000)
                        driver.close()
                    }
                }
    }

}


class HerokuLoginPage(
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")

        driver.findElWait(1000, 120000, "input#email", jsoup = false).firstOrNull()?.sendKeys(email)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 120000, "input#password", jsoup = false).firstOrNull()?.sendKeys(password)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 120000, "button[value=\"Log In\"]", jsoup = false).firstOrNull()?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Heroku | Login" &&
                    currentUrl.startsWith("https://id.heroku.com/login")
}

class HerokuDashboardPage(
        val action: HerokuDashboardAction,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    enum class HerokuDashboardAction {
        CREATE_NEW_APP, CLICK_FIRST_APP, GO_TO_ACCOUNT
    }

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")

        when (action) {
            HerokuDashboardAction.CREATE_NEW_APP -> driver.get("https://dashboard.heroku.com/new-app")
            HerokuDashboardAction.CLICK_FIRST_APP -> {
                driver.document?.selectFirst(".apps-list-item .items-baseline .ember-view span.near-black")?.let {
                    val appName = it.text()
                    println("appName: $appName")
                    driver.get("https://dashboard.heroku.com/apps/$appName")
                    return PageResponse.OK()
                }
            }
            HerokuDashboardAction.GO_TO_ACCOUNT -> driver.get("https://dashboard.heroku.com/account")
        }

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Personal apps | Heroku" &&
                    currentUrl.startsWith("https://dashboard.heroku.com/apps")
}

class HerokuAccountPage(
        val action: HerokuAccountAction,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    open class HerokuAccountAction {
        class CHANGE_EMAIL(val newEmail: String) : HerokuAccountAction()
    }


    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")

        when (action) {
            is HerokuAccountAction.CHANGE_EMAIL -> {
                driver.findFirstElWait(1000, 120000, ".profile.edit-first label[for=\"new-e-mail\"] ~ hk-inline-edit button")?.click()
                driver.findFirstElWait(1000, 120000, "input#new-e-mail")?.let {
                    it.clear()
                    it.sendKeys(action.newEmail)
                }
                driver.findFirstElWait(1000, 120000, ".profile.edit-first label[for=\"new-e-mail\"] ~ hk-inline-edit .__hk-inline-edit-submit-button__")?.click()
            }
        }

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Account | Heroku" &&
                    currentUrl.startsWith("https://dashboard.heroku.com/account") &&
                    doc.selectFirst(".modal-overlay .ember-modal-dialog .modal-box .modal-header")?.text() != "Update Profile" &&
                    doc.selectFirst(".hk-message.hk-message--success .lh-copy") == null
}

class HerokuAccountConfirmPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(1000, 120000, ".modal-overlay .modal-box .modal-content input[type=\"password\"]")?.sendKeys(password)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(1000, 120000, ".modal-overlay .modal-box .modal-footer button[type=\"submit\"]")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Account | Heroku" &&
                    currentUrl.startsWith("https://dashboard.heroku.com/account") &&
                    doc.selectFirst(".modal-overlay .ember-modal-dialog .modal-box .modal-header")?.text() == "Update Profile" &&
                    doc.selectFirst(".hk-message.hk-message--success .lh-copy") == null
}

class HerokuAccountConfirmPasswordDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Account | Heroku" &&
                    currentUrl.startsWith("https://dashboard.heroku.com/account") &&
                    doc.selectFirst(".modal-overlay .ember-modal-dialog .modal-box .modal-header") == null &&
                    doc.selectFirst(".hk-message.hk-message--success .lh-copy")?.text() == "A confirmation link has been sent to your new email address and you must click on the link to complete the address change. A notice has also been sent to your old email address."
}