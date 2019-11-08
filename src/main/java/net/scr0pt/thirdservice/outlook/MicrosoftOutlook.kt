package net.scr0pt.thirdservice.outlook

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.selenium.MicrosoftResponse
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.Response
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfileV2
import net.scr0pt.utils.webdriver.DriverManager
import org.apache.commons.lang3.RandomUtils
import org.bson.Document
import java.util.*


fun main() {
    val microsoftOutlook = MicrosoftOutlook()

    microsoftOutlook.doRegister()
//    microsoftOutlook.doLoginAllAcc()
}

class MicrosoftOutlook {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("microsoft")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("microsoft-account")

    fun doLoginAllAcc() {
        collection.find(Filters.exists("acc_status", false)).forEach {
            val email = it.getString("email")
            val password = it.getString("password")
            doLogin(email, password)
        }
    }

    fun doLogin(email: String, password: String) {
        val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = true)
        PageManager(driverManager, "https://login.live.com/login.srf").apply {
            addPageList(arrayListOf(
                    MicrosoftAccountLoginEnterEmailPage(email),
                    MicrosoftAccountLoginEnterPasswordPage(password),
                    MicrosoftAccountLoginAccountLockedPage(),
                    MicrosoftAccountPage()
            ))
            run() {
                if (it is MicrosoftResponse.ACCOUNT_HAS_BEEN_SUSPENDED) {
                    collection.updateOne(Document("email", email), Updates.set("acc_status", "locked"))
                    driver.close()
                } else if (it is MicrosoftResponse.LOGIN_ACC_DOESNT_EXIST) {
                    val newEmail = email.substringBeforeLast("@") + if (email.endsWith("@hotmail.com")) "@outlook.com" else "@hotmail.com"
                    collection.updateOne(Document("email", email), Updates.set("email", newEmail))
                    driver.close()
                } else if (it is Response.OK) {
                    collection.updateOne(Document("email", email), Updates.combine(
                            Updates.set("acc_status", "active"),
                            Updates.set("cookies", driver.cookieStr)
                    ))
                    driver.close()
                }
            }
        }
    }

    fun doRegister(driverManager: DriverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = false)) {
        val result = FakeProfileV2.getNewProfile() ?: return
        val password = "TheOutlook22001@22"
        val firstName = result.firstName
        val lastName = result.lastName
        val username = result.username
        var email: String = if (username.contains("@")) {
            username
        } else {
            val domail = if (RandomUtils.nextBoolean()) "hotmail.com" else "outlook.com"
            "$username@$domail"
        }

        println("email: $email\npassword: $password\nfirstname: $firstName\nlastname: $lastName")

        PageManager(driverManager, "https://signup.live.com/signup").apply {
            addPageList(
                    arrayListOf(
                            OutlookRegisterEnterEmailPage(email),
                            OutlookRegisterEnterPasswordPage(password).also {
                                it.onPageDetect = {
                                    this.driver.doc?.selectFirst(".identityBanner .identity")?.text()?.let {
                                        email = it
                                        println("email: $email\npassword: $password\nfirstname: $firstName\nlastname: $lastName")
                                    }
                                }
                            },
                            OutlookRegisterEnterNamePage(firstName, lastName),
                            OutlookRegisterEnterBirthdatePage(),
                            OutlookRegisterEnterCaptchaPage(),
                            OutlookRegisterEnterPhoneNumberPage(),
                            MicrosoftAccountPage(actionType = MicrosoftAccountPage.ActionType.GOTO_INBOX).apply {
                                onPageDetect = {
                                    collection.insertOne(
                                            Document("email", email).append("password", password)
                                                    .append("firstname", firstName).append("lastname", lastName)
                                                    .append("created_at", Date()).append("updated_at", Date())
                                                    .append("acc_status", "initial")
                                                    .append("cookies", driver.cookieStr)
                                    )
                                }
                            },
                            MicrosoftAccountCreatingYourMailboxPage(),
                            MicrosoftAccountEmailInboxPage()
                    )
            )
            run { response ->
                println(response)

                when (response) {
                    is Response.OK -> {
                        collection.updateOne(Document("email", email),
                                Document("updated_at", Date())
                                        .append("cookies", driver.cookieStr)
                        )
                    }
                    is MicrosoftResponse.REFISTER_ENTER_EMAIL_ERROR -> {
                        println(response.msg)
                    }
                    is MicrosoftResponse.REFISTER_EMAIL_ALREADY_REGISTED -> {
                        doRegister(driverManager)
                        println(response.msg)
                    }
                    is MicrosoftResponse.REFISTER_ENTER_EMAIL_FORMAT_ERROR -> {
                        doRegister(driverManager)
                        println(response.msg)
                    }
                }

            }
        }
    }
}