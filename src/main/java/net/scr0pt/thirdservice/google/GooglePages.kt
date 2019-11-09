package net.scr0pt.thirdservice.google

import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.utils.webdriver.DriverElements

object GoogleConstants {
    val recoverEmailLoginVerify = "https://accounts.google.com/signin/v2/challenge/kpe"
    val phoneNumberLoginVerify = "https://accounts.google.com/signin/v2/challenge/iap"
    val googleSearch = "https://www.google.com/"
    val protectYourAccount = "https://myaccount.google.com/signinoptions/recovery-options-collection"
    val PlusPageSignUpIdvChallenge = "https://accounts.google.com/b/0/PlusPageSignUpIdvChallenge"
}

class LoginEnterEmailPage(val email: String, onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    private val emailSelector = "input#identifierId[type=\"email\"]"
    private val nextBtnSelector = "#identifierNext"

    override fun onWaiting(pageStatus: PageStatus): Response? {
        if (pageStatus.html.contains("Không thể tìm thấy Tài khoản Google của bạn")) {
            return GoogleResponse.NOT_FOUND_EMAIL()
        }
        return null
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.contain(emailSelector) && pageStatus.contain(nextBtnSelector)

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(email, emailSelector)
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(nextBtnSelector) ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }


    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst(headingTextSelector)?.text() ?: return false
        val headingSubtext = pageStatus.doc.selectFirst(headingSubtextSelector)?.text() ?: return false
        val emailInput = pageStatus.doc.selectFirst(emailSelector) ?: return false

        if (pageStatus.title != "Đăng nhập - Tài khoản Google") return false

        when (headingText) {
            "Chào mừng" -> {
                return headingSubtext == ""
            }
            "Đăng nhập" -> {
                return headingSubtext == "Sử dụng Tài khoản Google của bạn"
            }
            else -> return false
        }
    }
}

class LoginEnterPasswordPage(val password: String, onPageFinish: (() -> Unit)? = null) :
        GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst(headingTextSelector)?.text() ?: return false
        val forgotPassword = pageStatus.doc.selectFirst("#forgotPassword")?.text() ?: return false
        return (headingText == "Chào mừng" || headingText == "Đăng nhập")
                && forgotPassword == "Bạn quên mật khẩu?"
                && pageStatus.notContain(".dMArKd .ck6P8")
                && pageStatus.contain("input[name=\"password\"]")
                && pageStatus.contain("#passwordNext")
    }

    override fun onWaiting(pageStatus: PageStatus) = when {
        pageStatus.html.contains("Mật khẩu không chính xác.") ->
            GoogleResponse.INCORECT_PASSWORD()
        pageStatus.html.contains("Mật khẩu của bạn đã thay đổi") ->
            GoogleResponse.PASSWORD_CHANGED()
        else -> null
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, "input[name=\"password\"]") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("#passwordNext") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class WellcomeToNewAccount(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            pageStatus.contain("h1:contains(Chào mừng bạn đến với tài khoản mới)")
                    && pageStatus.title == "Tài khoản Google"
                    && pageStatus.contain("input#accept")

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("input#accept") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

class ChangePasswordFirstTime(newPassword: String, onPageFinish: (() -> Unit)? = null) :
        GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.contain("h1:contains(Thay đổi mật khẩu cho)")
                && pageStatus.title == "Đổi Mật khẩu"
    }

    val form = DriverElements.Form(
            inputs = arrayListOf(
                    "input#Password" to newPassword,
                    "input#ConfirmPassword" to newPassword
            ),
            submitBtn = "input#submit"
    )

    override fun action(pageStatus: PageStatus): Response {
        form.submit(pageStatus.driver)
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus): Boolean {
        return form.selectors.all { pageStatus.contain(it) }
    }
}

class EnterPasswordFirstTimeChanged(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        GooglePage(onPageFinish = onPageFinish) {
    private val passwordSelector = "input[name=\"password\"]"
    private val nextBtnSelector = "#passwordNext"

    override fun detect(pageStatus: PageStatus) =
            pageStatus.html.contains("Để tiếp tục, trước tiên, hãy xác minh danh tính của bạn")
                    && pageStatus.contain(passwordSelector)
                    && pageStatus.contain(nextBtnSelector)

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(newPassword, passwordSelector)
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(nextBtnSelector) ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

class ProtectYourAccount(
        val defaultAction: DEFAULT_ACTION = DEFAULT_ACTION.UPDATE,
        onPageFinish: (() -> Unit)? = null
) : GooglePage(onPageFinish = onPageFinish) {
    enum class DEFAULT_ACTION {
        UPDATE, DONE
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.notContain(headingTextSelector) &&
                pageStatus.notContain(headingSubtextSelector) &&
                pageStatus.equalsText(".mkCr7e .N4lOwd", "Bảo vệ tài khoản của bạn")
    }


    override fun action(pageStatus: PageStatus): Response {
        println(this::class.java.simpleName + ": action ~ $defaultAction")
        when (defaultAction) {
            DEFAULT_ACTION.UPDATE -> {
                pageStatus.driver.clickFirstEl("span", equals = "Cập nhật")
                        ?: return Response.NOT_FOUND_ELEMENT()
                return Response.WAITING()
            }
            DEFAULT_ACTION.DONE -> {
                pageStatus.driver.clickFirstEl("span", equals = "Xong")?.click()
                        ?: return Response.NOT_FOUND_ELEMENT()
                return Response.WAITING()
            }
        }
    }

}

class ProtectYourAccountUpdatePhone(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.notContain(headingTextSelector) &&
                pageStatus.notContain(headingSubtextSelector) &&
                pageStatus.equalsText(".mkCr7e .N4lOwd", "Xác minh số điện thoại của bạn")

    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".n6Gm2e > a:nth-child(1)") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class ProtectYourAccountUpdateRecoverEmail(private val recoverEmail: String, onPageFinish: (() -> Unit)? = null) :
        GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.notContain(headingTextSelector) &&
                pageStatus.notContain(headingSubtextSelector) &&
                pageStatus.equalsText(".mkCr7e .N4lOwd", "Thêm email khôi phục")
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(recoverEmail, "input.whsOnd") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(".yKBrKe .U26fgb") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class ProtectYourAccountUpdateRecoverEmailSuccess(onPageFinish: (() -> Unit)? = null) :
        GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.notContain(headingTextSelector) &&
                pageStatus.notContain(headingSubtextSelector) &&
                pageStatus.equalsText(".mkCr7e .N4lOwd", "Thành công!")
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".yKBrKe div[role=\"button\"].U26fgb") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

class GoogleSearch(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.contain("#main input[type=\"search\"][name=\"q\"]") ||
                pageStatus.contain("#searchform input[autocomplete=\"off\"][name=\"q\"]")
    }

    override fun isEndPage() = true
}

//Veryfy it you action
class VerifyItsYouAction(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            pageStatus.title == "Đăng nhập - Tài khoản Google"
                    && !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                    pageStatus.equalsText(headingTextSelector, "Xác minh đó là bạn") &&
                    pageStatus.equalsText(headingSubtextSelector, "") &&
                    pageStatus.html.contains("Để bảo mật tài khoản của bạn, Google cần xác minh danh tính của bạn. Vui lòng đăng nhập lại để tiếp tục.") &&
                    pageStatus.equalsText("#identifierNext", "Tiếp theo")


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("#identifierNext span", equals = "Tiếp theo")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

//Veryfy phone number
class VerifyItsYouPhoneNumber(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                    pageStatus.equalsText(headingTextSelector, "Xác minh đó là bạn") &&
                    pageStatus.equalsText(headingSubtextSelector, "Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn. Tìm hiểu thêm") &&
                    pageStatus.contain("input#phoneNumberId")

    override fun action(pageStatus: PageStatus) = GoogleResponse.VERIFY_PHONE_NUMBER_DETECT()
}

//Nhận mã xác minh tại ••• ••• •• 68
//Gọi vào số điện thoại của bạn trong hồ sơ ••• ••• •• 68
//Xác nhận số điện thoại khôi phục của bạn
class VerifyItsYouPhoneNumberRecieveMessage(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                pageStatus.equalsText(headingTextSelector, "Xác minh đó là bạn") &&
                pageStatus.equalsText(headingSubtextSelector, "Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn. Tìm hiểu thêm") &&
                pageStatus.notContain("input#phoneNumberId") &&
                pageStatus.html.contains("Thử cách đăng nhập khác") &&
                (pageStatus.html.contains("Nhận mã xác minh tại") || pageStatus.html.contains("Gọi vào số điện thoại của bạn trong hồ sơ") || pageStatus.html.contains("Xác nhận số điện thoại khôi phục của bạn"))

    }

    override fun action(pageStatus: PageStatus) = GoogleResponse.VERIFY_PHONE_NUMBER_DETECT()

}

class VerifyItsYouRecoverEmail(private val recoverEmail: String, onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    private val recoverEmailSelector = "input#knowledge-preregistered-email-response"
    private val nextBtnSelector = "div[role=\"button\"] > span > span"

    override fun detect(pageStatus: PageStatus) =
            pageStatus.equalsText(headingTextSelector, "Xác minh đó là bạn")
                    && pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình")

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(recoverEmail, recoverEmailSelector)
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(nextBtnSelector, equals = "Tiếp theo")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.contain(recoverEmailSelector) && pageStatus.contain(nextBtnSelector)
}

class VerifyItsYouPhoneDevice(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            "Xác minh đó là bạn" == pageStatus.doc?.selectFirst(headingTextSelector)?.text()
                    && pageStatus.html.contains("Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn.")
                    && pageStatus.html.contains("Bạn có điện thoại của mình chứ?")
                    && pageStatus.html.contains("Google sẽ gửi thông báo đến điện thoại của bạn để xác minh đó là bạn")
                    && pageStatus.html.contains("Thử cách khác")

    override fun isEndPage() = true
}

class AccountDisable(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) = pageStatus.equalsText(headingTextSelector, "Đã vô hiệu hóa tài khoản")

    override fun isEndPage() = true
}


class CantLoginForYou(onPageFinish: (() -> Unit)? = null) : GooglePage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            pageStatus.equalsText(headingTextSelector, "Không thể đăng nhập cho bạn")
                    && pageStatus.html.contains("Google không thể xác minh tài khoản này thuộc về bạn.")
                    && pageStatus.html.contains("Hãy thử lại sau hoặc sử dụng Khôi phục tài khoản để được trợ giúp.")

    override fun action(pageStatus: PageStatus) = GoogleResponse.CANT_LOGIN_FOR_YOU()
}


abstract class GooglePage(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    val headingTextSelector = "#headingText"
    val headingSubtextSelector = "#headingSubtext"
}
