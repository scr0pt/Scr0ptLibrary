package net.scr0pt.thirdservice.outlook

import net.scr0pt.selenium.MicrosoftResponse
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import org.apache.commons.lang3.RandomUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select

/**
 * Created by Long
 * Date: 11/8/2019
 * Time: 10:16 PM
 */


class OutlookRegisterEnterEmailPage(
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun onWaiting(pageStatus: PageStatus): Response? {
        //Enter the email address in the format someone@example.com.
        val errorMessage = pageStatus.doc?.selectFirst("div[role=\"alert\"] .alert.alert-error")?.text()
        return when {
            errorMessage == "Someone already has this email address. Try another name or claim one of these that's available" -> MicrosoftResponse.REFISTER_EMAIL_ALREADY_REGISTED(msg = errorMessage)
            errorMessage == "Enter the email address in the format someone@example.com." -> MicrosoftResponse.REFISTER_ENTER_EMAIL_FORMAT_ERROR(msg = errorMessage)
            errorMessage != null -> MicrosoftResponse.REFISTER_ENTER_EMAIL_ERROR(msg = errorMessage)
            else -> null
        }
    }

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

class OutlookRegisterEnterPhoneNumberPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus) = MicrosoftResponse.REFISTER_ENTER_PHONE_NUMBER_PAGE()

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.live.com/signup")
                    && pageStatus.title == "Add security info"
                    && pageStatus.html.contains("When you need to prove you're you or a change is made to your account, we'll use your security info to contact you.")
                    && pageStatus.html.contains("Send a code to this phone number")
                    && pageStatus.equalsText(".form-group.template-input label", "Country code")
                    && pageStatus.equalsText(".form-group.template-input label", "Phone number")
                    && pageStatus.equalsText(".form-group.template-input label", "Enter the access code")
}

class MicrosoftAccountPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://account.microsoft.com") &&
                    pageStatus.title == "Microsoft account | Home"
}

class MicrosoftAccountLoginEnterEmailPage(
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    private val emailInputSelector = ".form-group .placeholderContainer input[type=\"email\"][name=\"loginfmt\"]"

    override fun onWaiting(pageStatus: PageStatus): Response? {
        val errorMessage = pageStatus.doc?.selectFirst("div[role=\"alert\"] .alert.alert-error")?.text()
        if (errorMessage == "That Microsoft account doesn't exist. Enter a different account or get a new one.") {
            return MicrosoftResponse.LOGIN_ACC_DOESNT_EXIST(msg = errorMessage)
        }

        return null
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(email, emailInputSelector)
        pageStatus.driver.clickFirstEl(".button-container input[type=\"submit\"]")
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://login.live.com/login.srf")
                    && pageStatus.title == "Sign in to your Microsoft account"
                    && pageStatus.equalsText("#loginHeader", "Sign in")
                    && pageStatus.contain(emailInputSelector, filter = { it.attr("aria-hidden") != "true" })
}

class MicrosoftAccountLoginEnterPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    private val passwordInputSelector = ".form-group .placeholderContainer input[type=\"password\"][name=\"passwd\"]"

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, passwordInputSelector)
        pageStatus.driver.clickFirstEl(".button-container input[type=\"submit\"]")
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://login.live.com/login.srf")
                    && pageStatus.title == "Sign in to your Microsoft account"
                    && pageStatus.equalsText("#loginHeader", "Enter password")
                    && pageStatus.contain(passwordInputSelector, filter = { it.attr("aria-hidden") != "true" })
}

class MicrosoftAccountLoginAccountLockedPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    private val nextBtnSelector = ".position-buttons .button-container input#StartAction[type=\"submit\"]"

    override fun action(pageStatus: PageStatus) = MicrosoftResponse.ACCOUNT_HAS_BEEN_SUSPENDED()

    //    override fun action(pageStatus: PageStatus): Response {
//        pageStatus.driver.clickFirstEl(nextBtnSelector)
//        return Response.WAITING()
//    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://account.live.com/Abuse")
                    && pageStatus.title == "Your account has been temporarily suspended"
                    && pageStatus.equalsText("#StartHeader", "Your account has been locked")
                    && pageStatus.contain(nextBtnSelector, filter = { it.attr("aria-hidden") != "true" })
}