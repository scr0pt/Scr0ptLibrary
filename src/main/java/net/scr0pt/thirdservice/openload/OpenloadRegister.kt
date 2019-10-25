package net.scr0pt.thirdservice.openload

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.thirdservice.mlab.loginGoogle
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.Browser
import org.bson.Document
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.util.*


suspend fun main() {
    register()
}

suspend fun register() {
    val gmailUsername = "vanlethi74@gmail.com"
    val gmailPassword = "XinChaoVietNam@@2000"

    val infinityMail = InfinityMail(gmailUsername)
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("openload")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("openload-account")

    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }

    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (email.contains("vanlethi74")) continue

        if (emails.contains(email)) continue
        emails.add(email)


        val password = "XinChaoVietNam@1990"
        val driver = Browser.firefox
        driver.get("https://openload.co/register")
        driver.sendKeysFirstEl(email, "form#register-form input#registerform-email") ?: continue
        driver.sendKeysFirstEl(password, "form#register-form input#registerform-password") ?: continue
        driver.sendKeysFirstEl(password, "form#register-form input#registerform-passwordconfirm") ?: continue
        driver.clickFirstEl("form#register-form input#registerform-iagree") ?: continue
        driver.clickFirstEl("form#register-form iframe[src*='https://www.google.com/recaptcha']") ?: continue
        driver.waitUntilUrlChange()
        println(driver.url)

        collection.insertOne(
                Document("email", email).append("password", password).append(
                        "cookies",
                        driver.cookieStr
                ).append("temp_ban", null).append("created_at", Date()).append("updated_at", Date())
        )
        driver.close()
    } while (true)
}

fun login(
        openloadEmail: String,
        openloadPassword: String,
        gmailUsername: String,
        gmailPassword: String,
        collection: MongoCollection<Document>
) {//update cookie
//    val driver = Browser.firefox

    val driver = Browser.htmlUnitDriver

    driver.get("https://openload.co/login")
    try {
        driver.executeAsyncScript(
                """
                document.querySelector(".sign-in-button").click();
                setTimeout(function(){ 
                    document.querySelector("#loginform-email").value = "${openloadEmail}"
                    document.querySelector("#loginform-password").value = "${openloadPassword}";
                    document.querySelector("#login-form .submitcontainer button").click();
                }, 3000);
        """.trimIndent()
        )
    } catch (e: Exception) {
    }

    val gmail = Gmail(gmailUsername, gmailPassword).apply {
        onEvent(
                MailReceiveEvent(
                        key = "ona1sender",
                        validator = { mail ->
                            Mail.CompareType.EQUAL_IGNORECASE.compare(
                                    mail.from,
                                    "admin@openload.co"
                            )
                        },
                        callback = { mails ->
                            val mail =
                                    mails.firstOrNull { it.content?.contains("Please log in using your login code below:") == true }
                            val code = mail?.content?.substringAfter("Please log in using your login code below:")?.trim()
                                    ?.substringBefore(" ")?.trim()
                            if (code != null) {
                                try {
                                    driver.executeAsyncScript(
                                            """
                                    document.getElementById("loginform-loginkey").value = "${code}"
                                    setTimeout(function(){ 
                                        document.querySelector("#main #login-form .submitcontainer button").click();
                                    }, 3000);
                            """.trimIndent()
                                    )
                                } catch (e: Exception) {
                                }
                            }
                        },
                        once = false,
                        new = true,
                        fetchContent = true
                )
        )
    }

    driver.waitUntilUrlChange()
    println(driver.url)
    gmail.logout()

    collection.updateOne(
            Document("email", openloadEmail), Updates.combine(
            Updates.set("cookies", driver.cookieStr),
            Updates.set("updated_at", Date())
    )
    )
}