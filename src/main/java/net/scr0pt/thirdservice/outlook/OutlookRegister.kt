package net.scr0pt.thirdservice.outlook

import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.DriverManager
import java.util.*


suspend fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("microsoft")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("microsoft-account")


    val domail = if (RandomUtils.nextBoolean()) "hotmail.com" else "outlook.com"
    val email = "scr0pt" + RandomStringUtils.randomAlphabetic(15).toLowerCase() + "@" + domail
    outlookRegister(email = email, collection = collection)
}

suspend fun outlookRegister(email: String, collection: MongoCollection<org.bson.Document>) {
    val password = "TheOutlook22001@22"
    val result = FakeProfile.getNewProfile()
    val firstName = result?.name?.first ?: "Bruce"
    val lastName = result?.name?.last ?: "Lee"

    println("email: $email\npassword: $password\nfirstname: $firstName\nlastname: $lastName")

    PageManager(Browser.chrome, "https://signup.live.com/signup").apply {
        addPageList(
                arrayListOf(
                        OutlookRegisterEnterEmailPage(email) {
                            println("OutlookRegisterEnterEmailPage success")
                        },
                        OutlookRegisterEnterPasswordPage(password) {
                            println("OutlookRegisterEnterPasswordPage success")
                        },
                        OutlookRegisterEnterNamePage(firstName, lastName) {
                            println("OutlookRegisterEnterNamePage success")
                        },
                        OutlookRegisterEnterBirthdatePage() {
                            println("OutlookRegisterEnterBirthdatePage success")
                        },
                        OutlookRegisterEnterCaptchaPage() {
                            println("OutlookRegisterEnterCaptchaPage success")
                        },
                        MicrosoftAccountPage() {
                            println("MicrosoftAccountPage success")
                        }
                )
        )
        run { response ->
            println(response)

            if (response is PageResponse.OK) {
                collection.insertOne(
                        org.bson.Document("email", email).append("password", password).append(
                                "firstname",
                                firstName
                        ).append("lastname", lastName).append("created_at", Date()).append("updated_at", Date())
                )
            }

            Thread.sleep(20000)
//        outlookRegisterPageManager.driver.close()
            Thread.sleep(600000)
        }
    }
}


class OutlookRegisterEnterEmailPage(
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(email, "input#MemberName") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("#iSignupAction") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.live.com/signup") &&
                    title == "Create account" &&
                    doc.selectFirst("#CredentialsPageTitle")?.text() == "Create account" &&
                    doc.selectFirst("#phoneSwitch")?.text() == "Use a phone number instead" &&
                    doc.selectFirst("#MemberName")?.text() != null
}


class OutlookRegisterEnterPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(password, "input#PasswordInput") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("#iSignupAction") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.live.com/signup") &&
                    title == "Create a password" &&
                    doc.selectFirst("#ShowHidePasswordLabel")?.text() == "Show password" &&
                    doc.selectFirst("#PasswordTitle")?.text() == "Create a password"
}

class OutlookRegisterEnterNamePage(
        val firstName: String,
        val lastName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(firstName, "input#FirstName") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(lastName, "input#LastName") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("#iSignupAction") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.live.com/signup") &&
                    title == "What's your name?" &&
                    doc.selectFirst("#iPageTitle")?.text() == "What's your name?"
}

class OutlookRegisterEnterBirthdatePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")

        val BirthYear = Select(driver.findFirstEl(By.id("BirthYear")))
        BirthYear.selectByValue(RandomUtils.nextInt(1980, 2001).toString())//exclude 2001
        val BirthDay = Select(driver.findFirstEl(By.id("BirthDay")))
        BirthDay.selectByIndex(RandomUtils.nextInt(1, BirthDay.options.size))//exclude Day value empty
        val BirthMonth = Select(driver.findFirstEl(By.id("BirthMonth")))
        BirthMonth.selectByIndex(RandomUtils.nextInt(1, BirthMonth.options.size))//exclude Month value empty

        driver.clickFirstEl("#iSignupAction") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.live.com/signup") &&
                    title == "What's your birth date?" &&
                    doc.selectFirst("#iPageTitle")?.text() == "What's your birth date?"
}

class OutlookRegisterEnterCaptchaPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        Thread.sleep(10000)//10 seconds
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://signup.live.com/signup") &&
                    title == "Add security info" &&
                    doc.selectFirst("#wlspispHipInstructionContainer")?.text() == "Enter the characters you see"
}

class MicrosoftAccountPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://account.microsoft.com") &&
                    title == "Microsoft account | Home"
}