package net.scr0pt.bot.google

import net.scr0pt.bot.GooglePageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageResponse
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver
import net.scr0pt.utils.webdriver.findElWait
import net.scr0pt.utils.webdriver.findFirstElWait


class LoginEnterEmailPage(val email: String, onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {
        if (doc.selectFirst(".LXRPh .GQ8Pzc")?.text() == "Không thể tìm thấy Tài khoản Google của bạn") {
            return PageResponse.NOT_FOUND_EMAIL()
        }
        return null
    }

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val emailInputs = driver.findElWait(100, 5000, "input#identifierId[type=\"email\"]")
        if (emailInputs.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            emailInputs.first().sendKeys(email)
            val nextBtns = driver.findElWait(100, 5000, "div#identifierNext[role=\"button\"]")
            if (nextBtns.isEmpty()) {
                return PageResponse.NOT_FOUND_ELEMENT()
            } else {
                nextBtns.first().click()
                return PageResponse.WAITING_FOR_RESULT()
            }
        }
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
//        val headingSubtext = doc.selectFirst("#headingSubtext")?.text() ?: return false
//        val passwordInput = doc.selectFirst("input[type=\"password\"]") ?: return false
//        if (passwordInput.attr("name") == "hiddenPassword") {
//            return false
//        }

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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val passwordInputs = driver.findElWait(100, 5000, "input[name=\"password\"]")
        if (passwordInputs.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            passwordInputs.first().clear()
            passwordInputs.first().sendKeys(password)
            val nextBtns = driver.findElWait(100, 5000, "div[role=\"button\"]#passwordNext")
            if (nextBtns.isEmpty()) {
                return PageResponse.NOT_FOUND_ELEMENT()
            } else {
                nextBtns.first().click()
                return PageResponse.WAITING_FOR_RESULT()
            }
        }
    }

}

class WellcomeToNewAccount(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle == "Chào mừng bạn đến với tài khoản mới"
    }

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val acceptBtns = driver.findElWait(100, 5000, "input#accept[name=\"accept\"]")
        if (acceptBtns.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            acceptBtns.first().click()
            return PageResponse.WAITING_FOR_RESULT()
        }
    }
}

class ChangePasswordFirstTime(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".glT6eb h1")?.text() ?: return false
        return wellcomeTitle.startsWith("Thay đổi mật khẩu cho ")
    }

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val passwordBtns = driver.findElWait(100, 5000, "input#Password")
        if (passwordBtns.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            passwordBtns.first().clear()
            passwordBtns.first().sendKeys(newPassword)
            val confirmPasswordBtns = driver.findElWait(100, 5000, "input#ConfirmPassword")
            if (confirmPasswordBtns.isEmpty()) {
                return PageResponse.NOT_FOUND_ELEMENT()
            } else {
                confirmPasswordBtns.first().clear()
                confirmPasswordBtns.first().sendKeys(newPassword)
                val submitBtns = driver.findElWait(100, 5000, "input#submit")
                if (submitBtns.isEmpty()) {
                    return PageResponse.NOT_FOUND_ELEMENT()
                } else {
                    submitBtns.first().click()
                    return PageResponse.WAITING_FOR_RESULT()
                }
            }
        }
    }
}

class EnterPasswordFirstTimeChanged(val newPassword: String, onPageFinish: (() -> Unit)? = null) :
        Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        val wellcomeTitle = doc.selectFirst(".dMArKd .ck6P8")?.text() ?: return false
        return wellcomeTitle.startsWith("Để tiếp tục, trước tiên, hãy xác minh danh tính của bạn")
    }

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val passwordBtns = driver.findElWait(100, 5000, "input[name=\"password\"]")
        if (passwordBtns.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            passwordBtns.first().clear()
            passwordBtns.first().sendKeys(newPassword)
            val submitBtns = driver.findElWait(100, 5000, "div#passwordNext")
            if (submitBtns.isEmpty()) {
                return PageResponse.NOT_FOUND_ELEMENT()
            } else {
                submitBtns.first().click()
                return PageResponse.WAITING_FOR_RESULT()
            }
        }
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action ~ $defaultAction")
        when (defaultAction) {
            DEFAULT_ACTION.UPDATE -> {
                driver.findFirstElWait(5000, 120000, "span", jsoup = false, filter = { el ->  ( "Cập nhật".equals(el.text, ignoreCase = true)  || "Update".equals(el.text, ignoreCase = true)) })?.click() ?: return PageResponse.NOT_FOUND_ELEMENT()
                return PageResponse.WAITING_FOR_RESULT()
            }
            DEFAULT_ACTION.DONE -> {
                driver.findFirstElWait(5000, 120000, "span", jsoup = false, filter = { el -> ( "Xong".equals(el.text, ignoreCase = true)  || "Done".equals(el.text, ignoreCase = true)) })?.click() ?: return PageResponse.NOT_FOUND_ELEMENT()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val nextBtns = driver.findElWait(100, 5000, ".n6Gm2e > a:nth-child(1)")
        if (nextBtns.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            nextBtns.first().click()
            return PageResponse.WAITING_FOR_RESULT()
        }
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val emailInputs = driver.findElWait(100, 5000, "input.whsOnd")
        if (emailInputs.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            emailInputs.first().sendKeys(recoverEmail)
            val nextBtns = driver.findElWait(100, 5000, ".yKBrKe .U26fgb")
            if (nextBtns.isEmpty()) {
                return PageResponse.NOT_FOUND_ELEMENT()
            } else {
                nextBtns.first().click()
                return PageResponse.WAITING_FOR_RESULT()
            }
        }
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        val nextBtns = driver.findElWait(100, 5000, ".yKBrKe div[role=\"button\"].U26fgb")
        if (nextBtns.isEmpty()) {
            return PageResponse.NOT_FOUND_ELEMENT()
        } else {
            nextBtns.first().click()
            return PageResponse.WAITING_FOR_RESULT()
        }
    }

}

class GoogleSearch(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish = onPageFinish) {
    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return doc.selectFirst("#main input[type=\"search\"][name=\"q\"]") != null ||
                doc.selectFirst("#searchform input[autocomplete=\"off\"][name=\"q\"]") != null
    }

    override fun isEndPage() = true

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
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

    override fun _action(driver: WebDriver): PageResponse {
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, "input#knowledge-preregistered-email-response", jsoup = false)?.sendKeys(recoverEmail)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        Thread.sleep(1000)
        driver.findFirstElWait(100, 5000, "span", jsoup = false, filter = { el -> el.text == "Tiếp theo" })?.click()
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

    override fun _action(driver: WebDriver): PageResponse {
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

    override fun _action(driver: WebDriver): PageResponse {
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

    override fun _action(driver: WebDriver): PageResponse {
        //RveJvd snByac Cố gắng khôi phục
        println(this::class.java.simpleName + ": action")
        return GooglePageResponse.CANT_LOGIN_FOR_YOU()
    }
}
