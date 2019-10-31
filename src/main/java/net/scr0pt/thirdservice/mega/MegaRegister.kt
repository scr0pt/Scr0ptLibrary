package net.scr0pt.thirdservice.mega

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
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


fun main() {
    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"
    val infinityMail = InfinityMail(gmailUsername)

    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
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
        val password = "XinChaoVietNam@2024"
        val driverManager = DriverManager(driverType = DriverManager.BrowserType.firefox, driverHeadless = true)
        PageManager(driverManager, "https://mega.nz/register").apply {
            addPageList(
                    arrayListOf(
                            MegaRegisterPage(firstName, lastName, email, password) {
                                println("MegaRegisterPage finish")
                                collection.insertOne(
                                        org.bson.Document("User_Name", email)
                                                .append("Password", password).append("firstName", firstName)
                                                .append("lastName", lastName).append("verify_email", false)
                                )
                            },
                            MegaRegisterConfirmEmailPage(gmailUsername, gmailPassword, startTime) {
                                println("MegaRegisterConfirmEmailPage finish")
                                collection.updateOne(org.bson.Document("User_Name", email), Updates.set("verify_email", true))
                            },
                            MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(
                                    gmailUsername,
                                    gmailPassword,
                                    password,
                                    startTime
                            ) {
                                println("MegaRegisterEnterPasswordAfterEnterConfirmLinkPage finish")
                            },
                            MegaGenerateKeyPage {
                                println("MegaGenerateKeyPage finish")
                            },
                            MegaChooseAccTypePage {
                                println("MegaChooseAccTypePage finish")
                            },
                            MegaDownloadAppPage {
                                println("MegaDownloadAppPage finish")
                            },
                            MegaGetRecoverKeyPage() {
                                println("MegaGetRecoverKeyPage finish")
                                Thread.sleep(10000)
                                getRecoverKey()?.let { recoverKey ->
                                    //update recover key
                                    println("recoverKey: $recoverKey")

                                }
                            },
                            MegaRecoverKeyDownloadedPage() {
                                println("MegaRecoverKeyDownloadedPage finish")
                            },
                            MegaCloudDrivePage() {
                                println("MegaCloudDrivePage finish")
                            }
                    )
            )
            run { response ->
                println(response)
                this.driver.close()
                Thread.sleep(5 * 60000)
            }
        }

    } while (true)
}

class MegaRegisterPage(
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
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

    override fun isReady(pageStatus: PageStatus): Boolean {
        return form.selectors.all { pageStatus.driver.findFirstEl(it) != null }
    }

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/register" &&
                    pageStatus.title == "Register - MEGA" &&
                    pageStatus.doc?.selectFirst("form#register_form .account.top-header")?.text() == "Create your free account" &&
                    pageStatus.doc.selectFirst(".registration-page-success.special .reg-success-special .reg-success-txt") == null
}

class MegaRegisterConfirmEmailPage(
        private val gmailUsername: String,
        private val gmailPassword: String,
        private val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    private var gmail: Gmail? = null

    override fun action(pageStatus: PageStatus): Response {
        if (gmail == null) {
            gmail = Gmail(gmailUsername, gmailPassword).apply {
                onEvent(
                        MailReceiveEvent(
                                key = "ona1sender",
                                validator = { mail ->
                                    Mail.CompareType.EQUAL_IGNORECASE.compare(
                                            mail.from,
                                            "welcome@mega.nz"
                                    )
                                },
                                callback = { mails ->
                                    val mail = mails.firstOrNull {
                                        (it.id
                                                ?: 0) > registerTime && it.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button") != null
                                    }
                                    val timestamp = mail?.id
                                    println(timestamp)
                                    mail?.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button")
                                            ?.attr("href")?.let { confirmLink ->
                                                pageStatus.driver.get(confirmLink)
                                                this.logout()
                                            }
                                },
                                once = false,
                                new = true,
                                fetchContent = true
                        )
                )
            }
        }

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/register" &&
                    pageStatus.title == "Register - MEGA" &&
                    pageStatus.doc?.selectFirst("form#register_form .account.top-header")?.text() == "Create your free account" &&
                    pageStatus.doc.selectFirst(".registration-page-success.special .reg-success-special .reg-success-txt")?.text()?.trim() == "Please check your email and click the link to confirm your account." &&
                    pageStatus.doc.selectFirst(".reg-resend-button-bl .resend-email-button")?.text()?.trim() == "Resend"
}


class MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(
        val gmailUsername: String,
        val gmailPassword: String,
        val password: String,
        val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    var gmail: Gmail? = null

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
            if (gmail == null) {
                gmail = Gmail(gmailUsername, gmailPassword).apply {
                    onEvent(
                            MailReceiveEvent(
                                    key = "ona1sender",
                                    validator = { mail ->
                                        Mail.CompareType.EQUAL_IGNORECASE.compare(
                                                mail.from,
                                                "welcome@mega.nz"
                                        )
                                    },
                                    callback = { mails ->
                                        val mail = mails.firstOrNull {
                                            (it.id
                                                    ?: 0) > registerTime && it.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button") != null
                                        }
                                        val timestamp = mail?.id
                                        println(timestamp)
                                        mail?.contentDocumented?.selectFirst("a[href^='https://mega.nz/#confirm']#bottom-button")
                                                ?.attr("href")?.let { confirmLink ->
                                                    pageStatus.driver.get(confirmLink)
                                                    this.logout()
                                                }
                                    },
                                    once = false,
                                    new = true,
                                    fetchContent = true
                            )
                    )
                }
            }
            return Response.WAITING()
        }
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.doc?.selectFirst(".main-top-info-block .main-top-info-text")?.text()?.isNotEmpty() == true &&
                    pageStatus.doc.selectFirst(".fm-dialog.warning-dialog-a .fm-notification-info h1") == null &&
                    pageStatus.title == "Login - MEGA"

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url?.startsWith("https://mega.nz/confirm") == true

}

class MegaGenerateKeyPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url == "https://mega.nz/key" &&
                    pageStatus.title == "MEGA"
}

class MegaChooseAccTypePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".key .plans .reg-st3-membership-bl.free .membership-pad-bl")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.title == "Plans & pricing - MEGA" &&
                    pageStatus.doc?.selectFirst(".bottom-page.top-info .big-header")?.text()?.trim() == "Choose your account type" &&
                    pageStatus.driver.findFirstEl(By.className("loading-info"), filter = { el -> !el.isDisplayed }) != null &&
                    pageStatus.driver.findFirstEl(".key .plans .reg-st3-membership-bl.free .membership-pad-bl") != null

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url == "https://mega.nz/pro"


}

class MegaGetRecoverKeyPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.driver.findFirstEl(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)") != null

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url?.startsWith("https://mega.nz/fm") == true &&
                    pageStatus.doc?.selectFirst(".post-register .step-main-question.post-register")?.text()?.trim() == "Here is your Recovery Key!" &&
                    pageStatus.doc.selectFirst(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)")?.text()?.trim() == "Download key" &&
                    !pageStatus.doc.selectFirst(".fm-dialog.recovery-key-dialog.backup-recover.improved-recovery-steps.post-register").hasClass(
                            "hidden"
                    )
}

class MegaRecoverKeyDownloadedPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".content-wrapper .default-green-button.close-dialog")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.driver.findFirstEl(".content-wrapper .default-green-button.close-dialog") != null

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url?.startsWith("https://mega.nz/fm") == true &&
                    pageStatus.doc?.selectFirst(".fm-dialog-header .fm-dialog-title.top-pad")?.text()?.trim() == "Account Recovery" &&
                    pageStatus.doc.selectFirst(".content-wrapper .default-green-button.close-dialog")?.text()?.trim() == "OK" &&
                    !pageStatus.doc.selectFirst(".fm-dialog.recovery-key-info.improved-recovery-steps").hasClass("hidden")
}

class MegaDownloadAppPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".button-wrappers .redirect-clouddrive-link")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.driver.findFirstEl(".button-wrappers .redirect-clouddrive-link") != null

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/downloadapp" &&
                    pageStatus.title == "Download our App" &&
                    pageStatus.doc?.selectFirst(".download-app")?.text()?.trim() == "Download the MEGA App" &&
                    pageStatus.doc.selectFirst(".button-wrappers .redirect-clouddrive-link")?.text() == "Skip this step"
}

class MegaCloudDrivePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun detect(pageStatus: PageStatus) =
            pageStatus.url == "https://mega.nz/fm" &&
                    pageStatus.title == "MEGA" &&
                    pageStatus.doc?.selectFirst(".cloud-drive .nw-fm-tree-header.cloud-drive input")?.attr("placeholder")?.trim() == "Cloud Drive" &&
                    pageStatus.doc.selectFirst("#how-to-upload .dropdown.hint-info-block .dropdown.hint-header")?.text()?.trim() == "How to upload"
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