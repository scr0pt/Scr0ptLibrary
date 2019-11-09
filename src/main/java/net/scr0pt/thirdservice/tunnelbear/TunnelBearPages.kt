package net.scr0pt.thirdservice.tunnelbear

import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.utils.webdriver.DriverElements

/**
 * Created by Long
 * Date: 11/9/2019
 * Time: 12:19 PM
 */
class TunnelBearRegisterPage(
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    val form = DriverElements.Form(
            inputs = arrayListOf(
                    "input[name=\"email\"]" to email,
                    "input[name=\"password\"]" to password
            ),
            submitBtn = "button[type=\"submit\"].submit-btn"
    )

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.executeScript("""
            var form = document.getElementById("signup-form");
            form.email.value = "${email}";
            form.password.value = "${password}";
            form.submit();
        """.trimIndent())
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = form.selectors.all { pageStatus.contain(it) }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url == "https://www.tunnelbear.com/account/signup"
                    && pageStatus.title == "Sign Up | TunnelBear"
}

class TunnelBearRegisterAccConfirmedPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url == "https://www.tunnelbear.com/account/confirmed"
                    && pageStatus.title == "TunnelBear: Secure VPN Service"
                    && pageStatus.equalsText("h1.green-text","Account Confirmed!")
}
