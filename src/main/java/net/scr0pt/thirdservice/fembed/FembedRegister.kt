package net.scr0pt.thirdservice.fembed

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.DriverManager
import org.bson.Document
import java.util.*

/**
 * Created by Long
 * Date: 10/19/2019
 * Time: 9:36 PM
 */

fun main() {
    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"

    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("fembed")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("fembed-account")

    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }

    val infinityMail = InfinityMail(gmailUsername)
    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (emails.contains(email)) continue
        emails.add(email)

        val result = FakeProfile.getNewProfile()
        val first = result?.name?.first ?: continue
        val last = result?.name?.last ?: continue

        registerFembed(
                "${first} $last", email,
                "Bruce${System.currentTimeMillis()}",
                gmailUsername,
                gmailPassword,
                collection
        )
    } while (true)
}

class FembedRegisterPage(
        val name: String,
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    override fun onWaiting(pageStatus: PageStatus): Response? {
        if (pageStatus.doc?.selectFirst(".notification.is-danger")?.text() == "This email is already registered with us, please use another one or try to reset password") {
            return FembedResponse.EMAIL_REGISTERED()
        }

        return null
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(name, "input#display_name") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(email, "input#email_register") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("button#register") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url?.startsWith("https://dash.fembed.com/auth/register") &&
                    pageStatus.doc?.selectFirst("#register_form .title")?.text() == "Free Register!"
}

class FembedThankYouForJoiningPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url?.startsWith("https://dash.fembed.com/auth/register") &&
                    pageStatus.title == "Register - Fembed" &&
                    pageStatus.doc?.selectFirst("#register_done .title")?.text() == "Thank You for joining." &&
                    pageStatus.doc.selectFirst(".notification.is-danger") == null &&
                    pageStatus.doc.selectFirst("#register_form .title")?.text() != "Free Register!"
}

class FembedActivatingSetPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, "input#password") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(password, "input#password2") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("button#verify") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Active my account - Fembed" &&
                    pageStatus.url.startsWith("https://dash.fembed.com/auth/verify?") &&
                    pageStatus.doc?.selectFirst(".container h3")?.text() == "Activating" &&
                    pageStatus.doc.selectFirst(".container h4")?.text() == "Please set your password."
}

class FembedDashboardPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun action(pageStatus: PageStatus) = Response.OK()

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Dashboard - Fembed" &&
                    pageStatus.url == "https://dash.fembed.com" &&
                    pageStatus.doc?.selectFirst(".container h1.title")?.text() == "Dashboard"
}


class FembedWellcomeBackPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url?.startsWith("https://dash.fembed.com/auth/login") &&
                    pageStatus.title == "Please login - Fembed" &&
                    pageStatus.doc?.selectFirst(".container .title")?.text() == "Welcome Back!"
}

fun registerFembed(
        name: String,
        email: String,
        password: String,
        gmailUsername: String,
        gmailPassword: String,
        collection: MongoCollection<Document>
) {
    println(email)
    val driver = DriverManager(driverType = DriverManager.BrowserType.chrome, driverHeadless = true)
    PageManager(driver,
            "https://dash.fembed.com/auth/register"
    ).apply {
        gmail = Gmail(gmailUsername, gmailPassword).apply {
            onEvent(
                    MailReceiveEvent(
                            key = "ona1sender",
                            validator = { mail ->
                                mail.receivedDate > startTime &&
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
                                                driver.get(confirmLink)
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

        addPageList(arrayListOf(
                FembedRegisterPage(name, email) {
                    println("FembedRegisterPage success")
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
        ))

        run { pageResponse ->
            if (pageResponse is FembedResponse.EMAIL_REGISTERED) {
                collection.insertOne(
                        Document("email", email).append("created_at", Date()).append("updated_at", Date())
                )
            }

            driver.close()
            Thread.sleep(6 * 60000)
            println(pageResponse)
        }
    }
}


