package net.scr0pt.thirdservice.tunnelbear

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.Response
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.DriverManager
import org.bson.Document

/**
 * Created by Long
 * Date: 11/9/2019
 * Time: 12:25 PM
 */

fun main() {
    val tunnelBear = TunnelBear()
    tunnelBear.doRegisterAllEmails("vinhnguyen4h4@gmail.com", "eHK;HyL.e=2k1704FgqN")
}

class TunnelBear {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("vpn")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("tunnelbear-account")

    fun doRegisterAllEmails(gmailUsername: String, gmailPassword: String) {
        val emails = arrayListOf<String>()
        val infinityMail = InfinityMail(gmailUsername)
        collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }
        do {
            val email = infinityMail.getNext()?.fullAddress ?: break
            if (emails.contains(email)) continue
            emails.add(email)

            doRegister(email, "Hello_${System.currentTimeMillis()}", gmailUsername, gmailPassword)
        } while (true)
    }

    fun doRegister(email: String, password: String, gmailUsername: String, gmailPassword: String) {
        val driver = DriverManager(DriverManager.BrowserType.Firefox)
        val pageManager = PageManager(driver = driver, originUrl = "https://www.tunnelbear.com/account/signup")
        pageManager.addPageList(
                arrayListOf(
                        TunnelBearRegisterPage(email, password),
                        TunnelBearRegisterAccConfirmedPage()
                )
        )
        pageManager.gmail = Gmail(gmailUsername, gmailPassword).apply {
            onEvent(
                    MailReceiveEvent(
                            key = "on_tunnelbear_sender",
                            validator = { mail ->
                                mail.receivedDate > pageManager.startTime
                                        && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "noreply@tunnelbear.com")
                                        && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "TunnelBear - email confirmation")
                            },
                            callback = { mails ->
                                val mail = mails.firstOrNull {
                                    it.content?.contains(email) == true
                                            && it.contentDocumented?.selectFirst("a[href^='https://www.tunnelbear.com/core/verifyEmail?key=']") != null
                                }
                                mail?.contentDocumented?.selectFirst("a[href^='https://www.tunnelbear.com/core/verifyEmail?key=']")
                                        ?.attr("href")?.let { confirmLink ->
                                            pageManager.driver.get(confirmLink)
                                            this.logout()
                                        }
                            },
                            once = false,
                            new = true,
                            fetchContent = true
                    )
            )
            connect()
        }
        pageManager.run {
            if (it is Response.OK) {
                collection.insertOne(Document("email", email).append("password", password).append("acc_status", "initial"))
            }
            Thread.sleep(1000000)
        }
    }

}