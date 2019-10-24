package net.scr0pt.thirdservice.fembed

import net.scr0pt.bot.FembedPageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import org.bson.Document
import org.openqa.selenium.WebDriver
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.findElWait
import java.util.*

/**
 * Created by Long
 * Date: 10/19/2019
 * Time: 9:36 PM
 */

suspend fun main() {
    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"

    val mongoClient =
        MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("fembed")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("fembed-account")

    val infinityMail = InfinityMail(gmailUsername)
    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (collection.countDocuments(Document("email", email)) > 0L) continue

        val result = FakeProfile.getNewProfile()
        val first = result?.name?.first ?: continue
        val last = result?.name?.last ?: continue

        registerFembed(
            "${first} $last", email,
            "Bruce${System.currentTimeMillis()}",
            gmailUsername,
            gmailPassword,
            collection,
            Browser.chrome
        )
    } while (true)
}

class FembedRegisterPage(
    val name: String,
    val email: String,
    onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun watingResult(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): PageResponse? {

        if (doc.selectFirst(".notification.is-danger")?.text() == "This email is already registered with us, please use another one or try to reset password") {
            return FembedPageResponse.Email_Registered()
        }

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findElWait(1000, 60000, "input#display_name", jsoup = false).firstOrNull()?.sendKeys(name)
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 60000, "input#email_register", jsoup = false).firstOrNull()?.sendKeys(email)
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 60000, "button#register", jsoup = false).firstOrNull()?.click()
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): Boolean =
        currentUrl.startsWith("https://dash.fembed.com/auth/register") &&
                doc.selectFirst("#register_form .title")?.text() == "Free Register!"
}

class FembedThankYouForJoiningPage(
    onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): Boolean =
        currentUrl.startsWith("https://dash.fembed.com/auth/register") &&
                doc.selectFirst("#register_done .title")?.text() == "Thank You for joining." &&
                doc.selectFirst(".notification.is-danger") == null &&
                doc.selectFirst("#register_form .title")?.text() != "Free Register!"
}

class FembedActivatingSetPasswordPage(
    val password: String,
    onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findElWait(1000, 60000, "input#password", jsoup = false).firstOrNull()?.sendKeys(password)
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 60000, "input#password2", jsoup = false).firstOrNull()?.sendKeys(password)
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 60000, "button#verify", jsoup = false).firstOrNull()?.click()
            ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): Boolean =
        title == "Active my account - Fembed" &&
                doc.selectFirst(".container h3")?.text() == "Activating" &&
                doc.selectFirst(".container h4")?.text() == "Please set your password."
}

class FembedDashboardPage(
    onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.OK()
    }

    override fun _detect(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): Boolean =
        title == "Dashboard - Fembed" &&
                currentUrl.startsWith("https://dash.fembed.com") &&
                doc.selectFirst(".container h1.title")?.text() == "Dashboard"
}


class FembedWellcomeBackPage(
    onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: org.jsoup.nodes.Document, currentUrl: String, title: String): Boolean =
        currentUrl.startsWith("https://dash.fembed.com/auth/login") &&
                title == "Please login - Fembed" &&
                doc.selectFirst(".container .title")?.text() == "Welcome Back!"
}

//    val driver = object : HtmlUnitDriver(BrowserVersion.FIREFOX_60, true) {
//        override fun modifyWebClient(client: WebClient): WebClient {
//            val webClient = super.modifyWebClient(client)
//            // you might customize the client here
//            webClient.options.isCssEnabled = false
//            return webClient
//        }
//    }

suspend fun registerFembed(
    name: String,
    email: String,
    password: String,
    gmailUsername: String,
    gmailPassword: String,
    collection: MongoCollection<Document>,
    driver: WebDriver
) {
    println(email)
    val registerTime = System.currentTimeMillis()

    val pageManager = PageManager(
        arrayListOf(
            FembedRegisterPage(name, email) {
                println("FembedRegisterPage success")
                getVerifyEmail(
                    registerTime,
                    gmailUsername,
                    gmailPassword
                ) { confirmLink ->
                    driver.get(confirmLink)
                }
            },
            FembedThankYouForJoiningPage {
                println("FembedThankYouForJoiningPage success")
            },
            FembedActivatingSetPasswordPage(password) {
                println("FembedActivatingSetPasswordPage success")
                collection.insertOne(
                    Document("email", email).append("password", password).append(
                        "name",
                        name
                    ).append("created_at", Date()).append("updated_at", Date())
                )
            },
            FembedDashboardPage {
                println("FembedDashboardPage success")
            },
            FembedWellcomeBackPage {
                println("FembedWellcomeBackPage success")
            }
        ), driver,
        "https://dash.fembed.com/auth/register"
    )

    pageManager.run { pageResponse ->

        if (pageResponse is FembedPageResponse.Email_Registered) {
            collection.insertOne(
                Document("email", email).append("created_at", Date()).append("updated_at", Date())
            )
        }

        driver.close()

        Thread.sleep(5 * 60000)
        println(pageResponse)
    }
}

fun getVerifyEmail(registerTime: Long, gmailUsername: String, gmailPassword: String, onSuccess: (String) -> Unit) {
    Gmail(gmailUsername, gmailPassword).apply {
        onEvent(
            MailReceiveEvent(
                key = "ona1sender",
                validator = { mail ->
                    (mail.id ?: 0) > registerTime &&
                            Mail.CompareType.EQUAL_IGNORECASE.compare(
                                mail.from,
                                "noreply@notify.fembed.com"
                            )
                    Mail.CompareType.EQUAL_IGNORECASE.compare(
                        mail.subject,
                        "Thank for registering an account with us"
                    )
                },
                callback = callback@{ mails ->
                    for (mail in mails) {
                        mail.contentDocumented?.selectFirst("a[href*='https://dash.fembed.com/auth/verify?token']")
                            ?.attr("href")?.let { confirmLink ->
                                println(confirmLink)
                                this.logout()
                                onSuccess(confirmLink)
                                return@callback
                            }
                    }
                },
                once = false,
                new = true,
                fetchContent = true
            )
        )
    }


}

