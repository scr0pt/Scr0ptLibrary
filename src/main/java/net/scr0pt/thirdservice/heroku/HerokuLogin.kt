package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.DriverManager
import org.bson.types.ObjectId


fun randomAppname(prefix: String? = null): String {
    var appName = (if (prefix == null) "bruce-" else prefix) + ObjectId().toString()
    while (appName.length > 30) appName = appName.substring(0, appName.length - 1)
    return appName.toLowerCase()
}

fun main() {
    changeHerokuEmail()
}

fun changeHerokuEmail() {
    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"
    val infinityMail = InfinityMail(gmailUsername)

    val emails = arrayListOf<String>()
    herokuCollection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }
    var doc: org.bson.Document? = null
    do {
        doc = herokuCollection.random(
                Filters.and(
                        Filters.exists("original_email", false),
                        Filters.not(
                                Filters.eq("email", "/.edu.vn/")
                        )
                )
        )
        if (doc != null) {
            val email = doc.getString("email")
            val password = doc.getString("password")

            if (email.replace(".", "").substringBeforeLast("@") == infinityMail.username.replace(".", "").substringBeforeLast("@")) return@changeHerokuEmail

            var newEmail: String? = null
            do {
                val newImail = infinityMail.getNext() ?: return@changeHerokuEmail
                newEmail = newImail.fullAddress
            } while (newEmail == null || newImail.username == infinityMail.username || emails.contains(newEmail) || herokuCollection.countDocuments(org.bson.Document("email", newEmail)) > 0)
            emails.add(newEmail)


            val driver = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = true)

            val herokuDashboardPage = HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.GO_TO_ACCOUNT).apply {
                onPageDetectOnce = {
                    herokuCollection.updateOne(
                            org.bson.Document("email", email),
                            Updates.set("cookies", driver.cookieStr)
                    )
                }
            }
            val pageManager = PageManager(driver, "https://id.heroku.com/login")
            pageManager.gmail = Gmail(gmailUsername, gmailPassword)
            pageManager.gmail?.onEvent(
                    MailReceiveEvent(
                            key = "noreply@heroku.com",
                            validator = { mail ->
                                mail.receivedDate > pageManager.startTime &&
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
                                    println("confirmLink: $confirmLink")
                                    driver.get(confirmLink)
                                    pageManager.gmail?.logout()
                                    herokuDashboardPage.onPageDetect = {
                                        pageManager.isSuccess = true
                                    }
                                }
                            },
                            once = false,
                            new = true,
                            fetchContent = true
                    )
            )
            pageManager.gmail?.connect()

            pageManager.addPageList(
                    arrayListOf(
                            HerokuLoginPage(email, password),
                            HerokuLoginAuthCallbackPage(),
                            herokuDashboardPage,
                            HerokuAccountConfirmPasswordPage(password),
                            HerokuAccountPage(action = HerokuAccountPage.HerokuAccountAction.CHANGE_EMAIL(newEmail)),
                            HerokuAccountConfirmPasswordDonePage()
                    )
            )
            pageManager.run {
                if (it is Response.OK) {
                    println("Set original_email to $email ")
                    println("Set email to $newEmail ")
                    herokuCollection.updateOne(org.bson.Document("email", email), Updates.combine(
                            Updates.set("original_email", email),
                            Updates.set("email", newEmail)
                    ))
                }
                println("changeHerokuEmail response $it")
                driver.close()
            }
        } else {
            Thread.sleep(60000)
        }
    } while (true)
}


fun main22() {
//    val collaboratorEmail = "alphahoai@gmail.com"
    val collaboratorEmail = "brucealmighty5daeae612ce20558@gmail.com"
    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

//    herokuCollection.random(org.bson.Document())?.let {
    herokuCollection.find().forEach {
        if (it.getList("collaborators", String::class.java)?.contains(collaboratorEmail) == true) return@forEach
        Thread.sleep(15000)

        val email = it.getString("email")
        val password = it.getString("password")

        println("$email ~ $password")
        var appName = randomAppname()
        val driver = DriverManager(driverType = DriverManager.BrowserType.Chrome)
        PageManager(driver, "https://id.heroku.com/login")
                .apply {
                    addPageList(arrayListOf(
                            HerokuLoginPage(email, password) {
                                println("HerokuLoginPage success")
                            }, HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CREATE_NEW_APP_IF_NOT_APP) {
                        println("HerokuDashboardPage click first app")
                    }
                    ))

                    run { pageresponse ->
                        println(pageresponse)
                        if (pageresponse is Response.OK) {

//                            driver.get(driver.url + "/settings")
//                            if (it.getString("appName").startsWith("bruce") == false) {
//                                driver.clickFirstEl("#app-rename-input")
//                                driver.sendKeysFirstEl(appName, "input.input-reset.lh-copy")
//                                driver.clickFirstEl(".__hk-inline-edit-submit-button__.async-button")
//                                driver.waitUntilUrlChange()
//                            }

                            driver.get(driver.url + "/access")
                            driver.clickFirstEl("button", equals = "Add collaborator")

                            driver.sendKeysFirstEl(collaboratorEmail, "input", filter = { el -> el.getAttribute("placeholder") == "user@domain.com" })
                            driver.clickFirstEl("button", equals = "Save changes")
                            driver.findEls(".collaborator-item", contains = collaboratorEmail)
//                            val nameOfApp = driver.url.substringBeforeLast("/access").substringAfterLast("/")
                            herokuCollection.updateOne(
                                    org.bson.Document("email", email),
                                    Updates.push("collaborators", collaboratorEmail)
                            )
//                            herokuCollection.updateOne(
//                                    org.bson.Document("email", email),
//                                    Updates.set("appName", nameOfApp)
//                            )
                            Thread.sleep(5000)
                            driver.close()
                            isFinish = true
                        }
                    }

                    while (!isFinish) Thread.sleep(5000)
                }
    }

}


class HerokuLoginPage(
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(email, "input#email") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(password, "input#password") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("button[value=\"Log In\"]") ?: return Response.NOT_FOUND_ELEMENT()

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Heroku | Login"
                    && pageStatus.url == "https://id.heroku.com/login"
}

class HerokuLoginAuthCallbackPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Heroku"
                    && pageStatus.url.startsWith("https://dashboard.heroku.com/auth/heroku/callback?code=")
}

class HerokuDashboardPage(
        val action: HerokuDashboardAction,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    enum class HerokuDashboardAction {
        CREATE_NEW_APP, CLICK_FIRST_APP, GO_TO_ACCOUNT, CREATE_NEW_APP_IF_NOT_APP
    }

    override fun action(pageStatus: PageStatus): Response {
        when (action) {
            HerokuDashboardAction.CREATE_NEW_APP -> pageStatus.driver.get("https://dashboard.heroku.com/new-app")
            HerokuDashboardAction.CLICK_FIRST_APP -> {
                pageStatus.doc?.selectFirst(".apps-list-item .items-baseline .ember-view span.near-black")?.let {
                    val appName = it.text()
                    println("appName: $appName")
                    pageStatus.driver.get("https://dashboard.heroku.com/apps/$appName")
                    return Response.OK()
                }
            }
            HerokuDashboardAction.CREATE_NEW_APP_IF_NOT_APP -> {
                if (pageStatus.notContain(".apps-list-item .items-baseline .ember-view span.near-black")) {
                    pageStatus.driver.get("https://dashboard.heroku.com/new-app")
                }
            }
            HerokuDashboardAction.GO_TO_ACCOUNT -> pageStatus.driver.get("https://dashboard.heroku.com/account")
        }

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Personal apps | Heroku" &&
                    pageStatus.url.startsWith("https://dashboard.heroku.com/apps")
}

class HerokuAccountPage(
        val action: HerokuAccountAction,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    open class HerokuAccountAction {
        class CHANGE_EMAIL(val newEmail: String) : HerokuAccountAction()
    }


    override fun action(pageStatus: PageStatus): Response {
        when (action) {
            is HerokuAccountAction.CHANGE_EMAIL -> {
                pageStatus.driver.clickFirstEl(".profile.edit-first label[for=\"new-e-mail\"] ~ hk-inline-edit button")
                pageStatus.driver.sendKeysFirstEl(action.newEmail, "input#new-e-mail")
                pageStatus.driver.clickFirstEl(".profile.edit-first label[for=\"new-e-mail\"] ~ hk-inline-edit .__hk-inline-edit-submit-button__")
            }
        }

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Account | Heroku" &&
                    pageStatus.url.startsWith("https://dashboard.heroku.com/account") &&
                    !pageStatus.equalsText(".modal-overlay .ember-modal-dialog .modal-box .modal-header", "Update Profile") &&
                    pageStatus.notContain(".hk-message.hk-message--success .lh-copy")
}

class HerokuAccountConfirmPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, ".modal-overlay .modal-box .modal-content input[type=\"password\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(".modal-overlay .modal-box .modal-footer button[type=\"submit\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Account | Heroku" &&
                    pageStatus.url.startsWith("https://dashboard.heroku.com/account") &&
                    pageStatus.equalsText(".modal-overlay .ember-modal-dialog .modal-box .modal-header", "Update Profile") &&
                    pageStatus.notContain(".hk-message.hk-message--success .lh-copy")
}

class HerokuAccountConfirmPasswordDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Account | Heroku" &&
                    pageStatus.url == "https://dashboard.heroku.com/account" &&
                    pageStatus.notContain(".modal-overlay .ember-modal-dialog .modal-box .modal-header") &&
                    pageStatus.equalsText(".hk-message.hk-message--success .lh-copy", "A confirmation link has been sent to your new email address and you must click on the link to complete the address change. A notice has also been sent to your old email address.")
}