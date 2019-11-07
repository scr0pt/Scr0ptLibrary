package net.scr0pt.thirdservice.outlook

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.FakeProfileV2
import net.scr0pt.utils.webdriver.DriverManager
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import java.util.*


fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("microsoft")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("microsoft-account")


//    val domail = if (RandomUtils.nextBoolean()) "hotmail.com" else "outlook.com"
    val email = "scr0pt" + RandomStringUtils.randomAlphabetic(15).toLowerCase() + "@outlook.com"
    outlookRegister(email = email, collection = collection)
}

fun outlookRegister(email: String, collection: MongoCollection<org.bson.Document>) {
    val password = "TheOutlook22001@22"
    val result = FakeProfileV2.getNewProfile() ?: return
    val firstName = result.firstName
    val lastName = result.lastName

    println("email: $email\npassword: $password\nfirstname: $firstName\nlastname: $lastName")

    val driverManager = DriverManager(driverType = DriverManager.BrowserType.Chrome, driverHeadless = false)
    PageManager(driverManager, "https://signup.live.com/signup").apply {
        addPageList(
                arrayListOf(
                        OutlookRegisterEnterEmailPage(email) ,
                        OutlookRegisterEnterPasswordPage(password) ,
                        OutlookRegisterEnterNamePage(firstName, lastName),
                        OutlookRegisterEnterBirthdatePage(),
                        OutlookRegisterEnterCaptchaPage() ,
                        MicrosoftAccountPage()
                )
        )
        run { response ->
            println(response)

            if (response is Response.OK) {
                collection.insertOne(
                        org.bson.Document("email", email).append("password", password).append(
                                "firstname",
                                firstName
                        ).append("lastname", lastName).append("created_at", Date()).append("updated_at", Date())
                )
            }

            driver.get("https://outlook.live.com")

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
    override fun action(pageStatus: PageStatus): Response {

        pageStatus.driver.sendKeysFirstEl(email, "input#MemberName") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("#iSignupAction") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup") &&
                    pageStatus.title == "Create account" &&
                    pageStatus.equalsText("#CredentialsPageTitle", "Create account") &&
                    pageStatus.equalsText("#phoneSwitch", "Use a phone number instead") &&
                    pageStatus.contain("#MemberName")
}


class OutlookRegisterEnterPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {

        pageStatus.driver.sendKeysFirstEl(password, "input#PasswordInput") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("#iSignupAction") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup") &&
                    pageStatus.title == "Create a password" &&
                    pageStatus.equalsText("#ShowHidePasswordLabel", "Show password") &&
                    pageStatus.equalsText("#PasswordTitle", "Create a password")
}

class OutlookRegisterEnterNamePage(
        val firstName: String,
        val lastName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {

        pageStatus.driver.sendKeysFirstEl(firstName, "input#FirstName") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(lastName, "input#LastName") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("#iSignupAction") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup") &&
                    pageStatus.title == "What's your name?" &&
                    pageStatus.equalsText("#iPageTitle", "What's your name?")
}

class OutlookRegisterEnterBirthdatePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {


        val BirthYear = Select(pageStatus.driver.findFirstEl(By.id("BirthYear")))
        BirthYear.selectByValue(RandomUtils.nextInt(1980, 2001).toString())//exclude 2001
        val BirthDay = Select(pageStatus.driver.findFirstEl(By.id("BirthDay")))//except 31, 30
        BirthDay.selectByIndex(RandomUtils.nextInt(1, BirthDay.options.size - 2))//exclude Day value empty
        val BirthMonth = Select(pageStatus.driver.findFirstEl(By.id("BirthMonth")))
        BirthMonth.selectByIndex(RandomUtils.nextInt(1, BirthMonth.options.size))//exclude Month value empty

        pageStatus.driver.clickFirstEl("#iSignupAction") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup") &&
                    pageStatus.title == "What's your birth date?" &&
                    pageStatus.equalsText("#iPageTitle", "What's your birth date?")
}

class OutlookRegisterEnterCaptchaPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {

        pageStatus.driver.findFirstEl(".form-group.template-input", contains = "Enter the characters you see")?.findElement(By.tagName("input"))?.click()
        Thread.sleep(10000)//10 seconds
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup") &&
                    pageStatus.title == "Add security info" &&
                    pageStatus.equalsText("#wlspispHipInstructionContainer", "Enter the characters you see")
}

class MicrosoftAccountPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun action(pageStatus: PageStatus): Response {

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://account.microsoft.com") &&
                    pageStatus.title == "Microsoft account | Home"
}