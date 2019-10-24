package net.scr0pt.bot.google

import net.scr0pt.bot.GooglePageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageResponse
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document


class LoginEnterEmailPage(val email: String, onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {
        if (doc.selectFirst(".LXRPh .GQ8Pzc")?.text() == "Không thể tìm thấy Tài khoản Google của bạn") {
            return PageResponse.NOT_FOUND_EMAIL()
        }
        return null
    }

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(email, "input#identifierId[type=\"email\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("div#identifierNext[role=\"button\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }


    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val headingText = doc.selectFirst("#headingText")?.text() ?: return false
        val headingSubtext = doc.selectFirst("#headingSubtext")?.text() ?: return false
        val emailInput = doc.selectFirst("input#identifierId[type=\"email\"]") ?: return false

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
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val headingText = doc.selectFirst("#headingText")?.text() ?: return false
        val forgotPassword = doc.selectFirst("#forgotPassword")?.text() ?: return false

        if (forgotPassword != "Bạn quên mật khẩu?") {
            return false
        } else if (headingText == "Chào mừng" || headingText == "Đăng nhập") {
            return true
        } else return false
    }

    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {
        if (doc.selectFirst("[jsname=\"B34EJ\"]")?.text()?.startsWith("Mật khẩu không chính xác.") ?: false) {
            return PageResponse.INCORECT_PASSWORD()//incorect password
        } else if (doc.selectFirst(".Xk3mYe.Jj6Lae .xgOPLd")?.text()?.startsWith("Mật khẩu của bạn đã thay đổi")
                        ?: false
        ) {
            return PageResponse.PASSWORD_CHANGED()//incorect password
        }
        return null
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(password, "input[name=\"password\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("div[role=\"button\"]#passwordNext") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

}

class WellcomeToNewAccount(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle == "Chào mừng bạn đến với tài khoản mới"
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl("input#accept[name=\"accept\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }
}

class ChangePasswordFirstTime(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle.startsWith("Thay đổi mật khẩu cho ")
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(newPassword, "input#Password") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(newPassword, "input#ConfirmPassword") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("input#submit") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }
}

class EnterPasswordFirstTimeChanged(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".dMArKd .ck6P8")?.text() ?: return false
        return wellcomeTitle.startsWith("Để tiếp tục, trước tiên, hãy xác minh danh tính của bạn")
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(newPassword, "input[name=\"password\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("#passwordNext") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

}

class ProtectYourAccount(
        val defaultAction: DEFAULT_ACTION = DEFAULT_ACTION.UPDATE,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    enum class DEFAULT_ACTION {
        UPDATE, DONE
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#headingText") == null &&
                doc.selectFirst("#headingSubtext") == null &&
                doc.selectFirst(".mkCr7e .N4lOwd")?.text() == "Bảo vệ tài khoản của bạn"
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action ~ $defaultAction")
        when (defaultAction) {
            DEFAULT_ACTION.UPDATE -> {
                driver.clickFirstEl("span", equals = "Cập nhật")
                        ?: return PageResponse.NOT_FOUND_ELEMENT()
                return PageResponse.WAITING_FOR_RESULT()
            }
            DEFAULT_ACTION.DONE -> {
                driver.clickFirstEl("span", equals = "Xong")?.click()
                        ?: return PageResponse.NOT_FOUND_ELEMENT()
                return PageResponse.WAITING_FOR_RESULT()
            }
        }
    }

}

class ProtectYourAccountUpdatePhone(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#headingText") == null &&
                doc.selectFirst("#headingSubtext") == null &&
                doc.selectFirst(".mkCr7e .N4lOwd")?.text() == "Xác minh số điện thoại của bạn"

    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".n6Gm2e > a:nth-child(1)") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

}

class ProtectYourAccountUpdateRecoverEmail(val recoverEmail: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#headingText") == null &&
                doc.selectFirst("#headingSubtext") == null &&
                doc.selectFirst(".mkCr7e .N4lOwd")?.text() == "Thêm email khôi phục"
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(recoverEmail, "input.whsOnd") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl(".yKBrKe .U26fgb") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

}

class ProtectYourAccountUpdateRecoverEmailSuccess(onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#headingText") == null &&
                doc.selectFirst("#headingSubtext") == null &&
                doc.selectFirst(".mkCr7e .N4lOwd")?.text() == "Thành công!"
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".yKBrKe div[role=\"button\"].U26fgb") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }
}

class GoogleSearch(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#main input[type=\"search\"][name=\"q\"]") != null ||
                doc.selectFirst("#searchform input[autocomplete=\"off\"][name=\"q\"]") != null
    }

    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }
}


//Veryfy it you action
class VerifyItsYouAction(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return !doc.html().contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                doc.selectFirst("#headingText")?.text() == "Xác minh đó là bạn" &&
                doc.selectFirst("#headingSubtext")?.text() == "" &&
                doc.html().contains("Để bảo mật tài khoản của bạn, Google cần xác minh danh tính của bạn. Vui lòng đăng nhập lại để tiếp tục.") &&
                doc.selectFirst("#identifierNext")?.text() == "Tiếp theo"
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl("#identifierNext span", equals = "Tiếp theo") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }
}
//Veryfy phone number
class VerifyItsYouPhoneNumber(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return !doc.html().contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình") &&
                doc.selectFirst("#headingText")?.text() == "Xác minh đó là bạn" &&
                doc.selectFirst("#headingSubtext")?.text() == "Không nhận dạng được thiết bị này. Để bảo mật cho bạn, Google muốn đảm bảo rằng đó thực sự là bạn. Tìm hiểu thêm" &&
                doc.selectFirst("input#phoneNumberId") != null
    }

    override fun responseWhenDetect(): PageResponse? {
        return GooglePageResponse.VERIFY_PHONE_NUMBER_DETECT()
    }

    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }
}

class VerifyItsYouRecoverEmail(val recoverEmail: String, onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val headingText = doc.selectFirst("#headingText")?.text() ?: return false
        return "Xác minh đó là bạn" == headingText && doc.html().contains("Xác nhận địa chỉ email khôi phục bạn đã thêm vào tài khoản của mình")
    }

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(recoverEmail, "input#knowledge-preregistered-email-response")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("span", equals = "Tiếp theo")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }
}

class CanotLoginForYou(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val headingText = doc.selectFirst("#headingText")?.text() ?: return false
        return "Không thể đăng nhập cho bạn" == headingText
    }

    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }
}


class AccountDisable(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val headingText = doc.selectFirst("#headingText")?.text() ?: return false
        return "Đã vô hiệu hóa tài khoản" == headingText
    }

    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        //RveJvd snByac Cố gắng khôi phục
        println(this::class.java.simpleName + ": action")
        return PageResponse.WAITING_FOR_RESULT()
    }
}


class CantLoginForYou(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return "Không thể đăng nhập cho bạn" == doc.selectFirst("#headingText")?.text()
                && doc.html().contains("Google không thể xác minh tài khoản này thuộc về bạn.")
                && doc.html().contains("Hãy thử lại sau hoặc sử dụng Khôi phục tài khoản để được trợ giúp.")
    }

    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? = GooglePageResponse.CANT_LOGIN_FOR_YOU()

    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        //RveJvd snByac Cố gắng khôi phục
        println(this::class.java.simpleName + ": action")
        return GooglePageResponse.CANT_LOGIN_FOR_YOU()
    }
}
