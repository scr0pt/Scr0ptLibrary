package net.scr0pt.thirdservice.mega

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.DriverElements
import net.scr0pt.utils.webdriver.DriverManager
import org.openqa.selenium.By
import java.io.File


fun main(args: Array<String>) {
    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"
    val infinityMail = InfinityMail(gmailUsername)

    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection!!)
    val serviceAccountDatabase = mongoClient.getDatabase("mega")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("mega-account")
    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("User_Name")?.let { emails.add(it) } }

    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (emails.contains(email)) continue
        emails.add(email)

        val result = FakeProfile.getNewProfile()
        val firstName = result?.name?.first ?: "Bruce"
        val lastName = result?.name?.last ?: "Lee"
        val password = "Bruce_${System.currentTimeMillis()}"
        println("email: $email")
        println("password: $password")
        val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = true)
        val pageManager = PageManager(driverManager, "https://mega.nz/register")
        pageManager.gmail = Gmail(gmailUsername, gmailPassword).apply {
            onEvent(
                    MailReceiveEvent(
                            key = "on_mega_sender",
                            validator = { mail ->
                                mail.receivedDate > pageManager.startTime
                                        && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "welcome@mega.nz")
                                        && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "MEGA Email Verification Required")
                            },
                            callback = { mails ->
                                val mail = mails.firstOrNull {
                                    it.content?.contains(email) == true
                                            && it.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button") != null
                                }
                                mail?.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button")
                                        ?.attr("href")?.let { confirmLink ->
                                            pageManager.driver.get(confirmLink)
                                            this.logout()
                                        }
                            },
                            once = false,
                            new = true,
                            fetchContent = true
                    )
            )
        }
        pageManager.addPageList(
                arrayListOf(
                        MegaRegisterPage(firstName, lastName, email, password) {
                            collection.insertOne(
                                    org.bson.Document("User_Name", email)
                                            .append("Password", password).append("firstName", firstName)
                                            .append("lastName", lastName).append("verify_email", false)
                            )
                        },
                        MegaRegisterConfirmEmailPage() {
                            collection.updateOne(org.bson.Document("User_Name", email), Updates.set("verify_email", true))
                        },
                        MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(password),
                        MegaGenerateKeyPage(),
                        MegaChooseAccTypePage(),
                        MegaDownloadAppPage(),
                        MegaGetRecoverKeyPage(),
                        MegaRecoverKeyDownloadedPage(),
                        MegaCloudDrivePage()
                )
        )
        pageManager.run { response ->
            println(response)
            pageManager.driver.close()
            Thread.sleep(10 * 60000)
        }

    } while (true)
}

class MegaRegisterPage(
        firstName: String,
        lastName: String,
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    val form = DriverElements.Form(
            inputs = arrayListOf(
                    "input#register-firstname-registerpage2" to firstName,
                    "input#register-lastname-registerpage2" to lastName,
                    "input#register-email-registerpage2" to email,
                    "input#register-password-registerpage2" to password,
                    "input#register-password-registerpage3" to password
            ),
            buttons = listOf(
                    //I understand that if I lose my password, I may lose my data. Read more about MEGA’s end-to-end encryption.
                    ".checkbox-block.pw-remind .understand-check input.checkboxOff",
                    //I agree with the MEGA Terms of Service
                    "input#register-check-registerpage2"
            ),
            submitBtn = "form#register_form .register-button.active"
    )

    override fun action(pageStatus: PageStatus): Response {
        form.submit(pageStatus.driver)
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = super.isReady(pageStatus) && form.selectors.all { pageStatus.contain(it) }

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/register"
                    && pageStatus.title == "Register - MEGA"
                    && pageStatus.equalsText("form#register_form .account.top-header", "Create your free account")
                    && pageStatus.notContain(".registration-page-success.special .reg-success-special .reg-success-txt")
                    && pageStatus.contain("body#bodyel.not-logged.bottom-pages")
                    && pageStatus.notContain("body#bodyel.not-logged.bottom-pages.overlayed")
}

class MegaRegisterConfirmEmailPage(
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/register"
                    && pageStatus.title == "Register - MEGA"
                    && pageStatus.equalsText("form#register_form .account.top-header", "Create your free account")
                    && pageStatus.equalsText(".registration-page-success.special .reg-success-special .reg-success-txt", "Please check your email and click the link to confirm your account.")
                    && pageStatus.equalsText(".reg-resend-button-bl .resend-email-button", "Resend")
                    && pageStatus.contain("body#bodyel.not-logged.bottom-pages.overlayed")
}


class MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        val doc = pageStatus.doc ?: return Response.WAITING()

        val notiText = doc.selectFirst(".main-top-info-block .main-top-info-text")?.text()
        if (notiText == "Please enter your password to confirm your account.") {
            if (doc.selectFirst("#login_form .account.top-header.login")?.text()?.trim() == "Confirm your account" &&
                    doc.selectFirst("#login_form .big-red-button.login-button.button")?.text()?.trim() == "Confirm your account"
            ) {
                pageStatus.driver.sendKeysFirstEl(password, "input#login-password2")
                        ?: return Response.NOT_FOUND_ELEMENT()
                pageStatus.driver.clickFirstEl("#login_form .big-red-button.login-button.button")
                        ?: return Response.NOT_FOUND_ELEMENT()
                return Response.WAITING()
            }
        } else if (notiText == "Your confirmation link is no longer valid. Your account may already be activated or you may have cancelled your registration.") {
            return MegaResponse.CONFIRMATIOM_LINK_NO_LONGER_VALID()
        }
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = super.isReady(pageStatus) &&
            pageStatus.doc?.selectFirst(".main-top-info-block .main-top-info-text")?.text()?.isNotEmpty() == true &&
            pageStatus.notContain(".fm-dialog.warning-dialog-a .fm-notification-info h1") &&
            pageStatus.title == "Login - MEGA"

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://mega.nz/confirm")

}

class MegaGenerateKeyPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url == "https://mega.nz/key"
                    && pageStatus.title == "MEGA"
}

class MegaChooseAccTypePage(
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".key .plans .reg-st3-membership-bl.free .membership-pad-bl")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = super.isReady(pageStatus) &&
            pageStatus.title == "Plans & pricing - MEGA" &&
            pageStatus.doc?.selectFirst(".bottom-page.top-info .big-header")?.text()?.trim() == "Choose your account type" &&
            pageStatus.driver.findFirstEl(By.className("loading-info"), filter = { el -> !el.isDisplayed }) != null &&
            pageStatus.driver.findFirstEl(".key .plans .reg-st3-membership-bl.free .membership-pad-bl") != null

    override fun detect(pageStatus: PageStatus) = pageStatus.url == "https://mega.nz/pro"


}

class MegaGetRecoverKeyPage(
        private val actonType: ACTION_TYPE = ACTION_TYPE.SKIP_DOWNLOAD_RECOVER_KEY,
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    enum class ACTION_TYPE {
        DOWNLOAD_RECOVER_KEY, SKIP_DOWNLOAD_RECOVER_KEY
    }

    override fun action(pageStatus: PageStatus): Response {
        when (actonType) {
            ACTION_TYPE.DOWNLOAD_RECOVER_KEY -> pageStatus.driver.clickFirstEl(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)")
            ACTION_TYPE.SKIP_DOWNLOAD_RECOVER_KEY -> pageStatus.driver.clickFirstEl("p.skip-button.top-pad", equals = "No thanks, I can remember my password.")
        }

        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = super.isReady(pageStatus) &&
            pageStatus.contain(".loading-spinner.hidden")

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/fm"
                    && pageStatus.equalsText("h1.step-main-question.post-register", "Here is your Recovery Key!")
                    && pageStatus.equalsText("p.skip-button.top-pad", "No thanks, I can remember my password.")
                    && pageStatus.html.contains("Download key")
                    && pageStatus.html.contains("You will need this if you have forgotten your password or lost your Authenticator.")
}

class MegaRecoverKeyDownloadedPage(
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    private val closeDialogSelector = ".content-wrapper .default-green-button.close-dialog"
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(closeDialogSelector)
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://mega.nz/fm")
                && pageStatus.equalsText(".fm-dialog-header .fm-dialog-title.top-pad", "Account Recovery")
                && pageStatus.notContain(".fm-dialog.recovery-key-info.improved-recovery-steps.hidden")
                && pageStatus.equalsText(closeDialogSelector, "OK")
    }
}

class MegaDownloadAppPage(
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".button-wrappers .redirect-clouddrive-link")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) = super.isReady(pageStatus) &&
            pageStatus.driver.findFirstEl(".button-wrappers .redirect-clouddrive-link") != null

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/downloadapp" &&
                    pageStatus.title == "Download our App" &&
                    pageStatus.equalsText(".download-app", "Download the MEGA App") &&
                    pageStatus.equalsText(".button-wrappers .redirect-clouddrive-link", "Skip this step")
}

class MegaCloudDrivePage(
        onPageFinish: (() -> Unit)? = null
) : MegaPage(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url.startsWith("https://mega.nz/fm/")
                    && pageStatus.title == "MEGA"
                    && pageStatus.contain("#bodyel.logged")
                    && pageStatus.contain(".cloud-drive .nw-fm-tree-header.cloud-drive input[placeholder=\"Cloud Drive\"]")
                    && pageStatus.equalsText("#how-to-upload .dropdown.hint-info-block .dropdown.hint-header", "How to upload")
                    && pageStatus.contain("body#bodyel.logged.free")
}

abstract class MegaPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isReady(pageStatus: PageStatus) = pageStatus.notContain("body#bodyel.loading")
}


fun getRecoverKey(): String? {
    val fileName = "C:\\Users\\Administrator\\Downloads\\MEGA-RECOVERYKEY.txt"
    val myFile = File(fileName)
    if (myFile.exists()) {
        val recoverKey = myFile.readLines()?.first()
        myFile.delete()
        return recoverKey
    }
    return null
}