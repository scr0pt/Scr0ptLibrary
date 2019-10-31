package net.scr0pt.bot.google

import net.scr0pt.selenium.GoogleResponse
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.utils.webdriver.DriverElements
import org.jsoup.nodes.Document


class LoginEnterEmailPage(val email: String, onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun onWaiting(pageStatus: PageStatus): Response? {
        if (pageStatus.doc?.selectFirst(".LXRPh .GQ8Pzc")?.text() == "Không thể tìm thấy Tài khoản Google của bạn") {
            return GoogleResponse.NOT_FOUND_EMAIL()
        }
        return null
    }


    override fun action(pageStatus: PageStatus): Response {
        println(this::class.java.simpleName + ": action")
        pageStatus.driver.sendKeysFirstEl(email, "input#identifierId[type=\"email\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("div#identifierNext[role=\"button\"]") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }


    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst("#headingText")?.text() ?: return false
        val headingSubtext = pageStatus.doc?.selectFirst("#headingSubtext")?.text() ?: return false
        val emailInput = pageStatus.doc?.selectFirst("input#identifierId[type=\"email\"]") ?: return false

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
        Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst("#headingText")?.text() ?: return false
        val forgotPassword = pageStatus.doc?.selectFirst("#forgotPassword")?.text() ?: return false

        return when {
            forgotPassword != "Bạn quên mật khẩu?" -> false
            headingText == "Chào mừng" || headingText == "Đăng nhập" -> true
            else -> false
        }
    }

    override fun onWaiting(pageStatus: PageStatus): Response? {
        if (pageStatus.doc?.selectFirst("[jsname=\"B34EJ\"]")?.text()?.startsWith("Mật khẩu không chính xác.") == true) {
            return GoogleResponse.INCORECT_PASSWORD()//incorect password
        } else if (pageStatus.doc?.selectFirst(".Xk3mYe.Jj6Lae .xgOPLd")?.text()?.startsWith("Mật khẩu của bạn đã thay đổi") == true
        ) {
            return GoogleResponse.PASSWORD_CHANGED()//incorect password
        }
        return null
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, "input[name=\"password\"]") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("div[role=\"button\"]#passwordNext") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.driver.findFirstEl("input[name=\"password\"]") != null &&
                    pageStatus.driver.findFirstEl("div[role=\"button\"]#passwordNext") != null
}

class WellcomeToNewAccount(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val wellcomeTitle = pageStatus.doc?.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle == "Chào mừng bạn đến với tài khoản mới"
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("input#accept[name=\"accept\"]") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = pageStatus.driver.findFirstEl("input#accept[name=\"accept\"]") != null
}

class ChangePasswordFirstTime(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val wellcomeTitle = pageStatus.doc?.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle.startsWith("Thay đổi mật khẩu cho ")
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
        return form.selectors.all { pageStatus.driver.findFirstEl(it) != null }
    }
}

class EnterPasswordFirstTimeChanged(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val wellcomeTitle = pageStatus.doc?.selectFirst(".dMArKd .ck6P8")?.text() ?: return false
        return wellcomeTitle.startsWith("Để tiếp tục, trước tiên, hãy xác minh danh tính của bạn")
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(newPassword, "input[name=\"password\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("#passwordNext") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class ProtectYourAccount(
        val defaultAction: DEFAULT_ACTION = DEFAULT_ACTION.UPDATE,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    enum class DEFAULT_ACTION {
        UPDATE, DONE
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.doc?.selectFirst("#headingText") == null &&
                pageStatus.doc?.selectFirst("#headingSubtext") == null &&
                pageStatus.doc?.selectFirst(".mkCr7e .N4lOwd")?.text() == "Bảo vệ tài khoản của bạn"
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

class ProtectYourAccountUpdatePhone(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.doc?.selectFirst("#headingText") == null &&
                pageStatus.doc?.selectFirst("#headingSubtext") == null &&
                pageStatus.doc?.selectFirst(".mkCr7e .N4lOwd")?.text() == "Xác minh số điện thoại của bạn"

    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".n6Gm2e > a:nth-child(1)") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class ProtectYourAccountUpdateRecoverEmail(val recoverEmail: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.doc?.selectFirst("#headingText") == null &&
                pageStatus.doc?.selectFirst("#headingSubtext") == null &&
                pageStatus.doc?.selectFirst(".mkCr7e .N4lOwd")?.text() == "Thêm email khôi phục"
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(recoverEmail, "input.whsOnd") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(".yKBrKe .U26fgb") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

}

class ProtectYourAccountUpdateRecoverEmailSuccess(onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.doc?.selectFirst("#headingText") == null &&
                pageStatus.doc?.selectFirst("#headingSubtext") == null &&
                pageStatus.doc?.selectFirst(".mkCr7e .N4lOwd")?.text() == "Thành công!"
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".yKBrKe div[role=\"button\"].U26fgb") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

class GoogleSearch(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.doc?.selectFirst("#main input[type=\"search\"][name=\"q\"]") != null ||
                pageStatus.doc?.selectFirst("#searchform input[autocomplete=\"off\"][name=\"q\"]") != null
    }

    override fun isEndPage() = true

    override fun action(pageStatus: PageStatus): Response {
        return Response.WAITING()
    }
}

//Veryfy it you action
class VerifyItsYouAction(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                pageStatus.doc?.selectFirst("#headingText")?.text() == "Xác minh đó là bạn" &&
                pageStatus.doc?.selectFirst("#headingSubtext")?.text() == "" &&
                pageStatus.doc?.html().contains("Để bảo mật tài khoản của bạn, Google cần xác minh danh tính của bạn. Vui lòng đăng nhập lại để tiếp tục.") &&
                pageStatus.doc?.selectFirst("#identifierNext")?.text() == "Tiếp theo"
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("#identifierNext span", equals = "Tiếp theo")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

//Veryfy phone number
class VerifyItsYouPhoneNumber(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                pageStatus.doc?.selectFirst("#headingText")?.text() == "Xác minh đó là bạn" &&
                pageStatus.doc?.selectFirst("#headingSubtext")?.text() == "Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn. Tìm hiểu thêm" &&
                pageStatus.doc?.selectFirst("input#phoneNumberId") != null
    }

    override fun action(pageStatus: PageStatus) = GoogleResponse.VERIFY_PHONE_NUMBER_DETECT()
}

//Nhận mã xác minh tại ••• ••• •• 68
//Gọi vào số điện thoại của bạn trong hồ sơ ••• ••• •• 68
//Xác nhận số điện thoại khôi phục của bạn
class VerifyItsYouPhoneNumberRecieveMessage(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return !pageStatus.html.contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                pageStatus.doc?.selectFirst("#headingText")?.text() == "Xác minh đó là bạn" &&
                pageStatus.doc?.selectFirst("#headingSubtext")?.text() == "Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn. Tìm hiểu thêm" &&
                pageStatus.doc?.selectFirst("input#phoneNumberId") == null &&
                pageStatus.html.contains("Thử cách đăng nhập khác") &&
                (pageStatus.html.contains("Nhận mã xác minh tại") ||
                        pageStatus.html.contains("Gọi vào số điện thoại của bạn trong hồ sơ") ||
                        pageStatus.html.contains("Xác nhận số điện thoại khôi phục của bạn"))

    }

    override fun action(pageStatus: PageStatus) = GoogleResponse.VERIFY_PHONE_NUMBER_DETECT()

}

class VerifyItsYouRecoverEmail(val recoverEmail: String, onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst("#headingText")?.text() ?: return false
        return "Xác minh đó là bạn" == headingText && pageStatus.doc?.html().contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình")
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(recoverEmail, "input#knowledge-preregistered-email-response")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("div[role=\"button\"] > span > span", equals = "Tiếp theo")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}

class VerifyItsYouPhoneDevice(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst("#headingText")?.text() ?: return false
        return "Xác minh đó là bạn" == headingText
                && pageStatus.doc?.html().contains("Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn.")
                && pageStatus.doc?.html().contains("Bạn có điện thoại của mình chứ?")
                && pageStatus.doc?.html().contains("Google sẽ gửi thông báo đến điện thoại của bạn để xác minh đó là bạn")
                && pageStatus.doc?.html().contains("Thử cách khác")
    }

    override fun isEndPage() = true

}

class AccountDisable(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        val headingText = pageStatus.doc?.selectFirst("#headingText")?.text() ?: return false
        return "Đã vô hiệu hóa tài khoản" == headingText
    }

    override fun isEndPage() = true
}


class CantLoginForYou(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return "Không thể đăng nhập cho bạn" == pageStatus.doc?.selectFirst("#headingText")?.text()
                && pageStatus.html.contains("Google không thể xác minh tài khoản này thuộc về bạn.")
                && pageStatus.html.contains("Hãy thử lại sau hoặc sử dụng Khôi phục tài khoản để được trợ giúp.")
    }

    override fun action(pageStatus: PageStatus) = GoogleResponse.CANT_LOGIN_FOR_YOU()
}
