package net.scr0pt.thirdservice.google

import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.Response
import net.scr0pt.utils.webdriver.DriverManager

/**
 * Created by Long
 * Date: 11/10/2019
 * Time: 8:46 PM
 */


fun loginGoogle(email: String, password: String, driver: DriverManager, onLoginSuccess: () -> Unit, onLoginFail: ((pageResponse: Response?) -> Unit)? = null, recoverEmail: String? = null) {
    println("loginGoogle: $email $password")
    PageManager(driver,
            "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
    ).apply {
        addPageList(arrayListOf(
                LoginEnterEmailPage(email),
                LoginEnterPasswordPage(password),
                ProtectYourAccount(defaultAction = ProtectYourAccount.DEFAULT_ACTION.DONE),
                VerifyItsYouPhoneNumber(),
                VerifyItsYouPhoneNumberRecieveMessage(),
                GoogleSearch(),
                VerifyItsYouPhoneDevice(),
                CantLoginForYou()
        ))

        recoverEmail?.let {
            addPage(VerifyItsYouRecoverEmail(it))
        }

        generalWatingResult = { pageStatus ->
            if ((pageStatus.doc?.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
                GoogleResponse.RECAPTCHA()
            } else Response.WAITING()
        }

        run { pageResponse ->
            if (pageResponse is Response.OK) {
                onLoginSuccess()
            } else {
                driver.close()
                onLoginFail?.let { it(pageResponse) }
            }
        }
    }
}