package net.scr0pt.thirdservice.mega

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.thirdservice.mongodb.MongoConnection
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import net.scr0pt.utils.*
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.DriverManager
import java.io.File


suspend fun main() {
    val gmailUsername = "brucealmighty5daeae612ce20558@gmail.com"
    val gmailPassword = "5dQICtEu5Z6AIo5C8vnN"
    val infinityMail = InfinityMail(gmailUsername)

    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("mega")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("mega-account")
    val emails = arrayListOf<String>()
    collection.find().forEach { doc->doc?.getString("User_Name")?.let { emails.add(it) } }

    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (emails.contains(email)) continue
        emails.add(email)

        val result = FakeProfile.getNewProfile()
        val firstName = result?.name?.first ?: "Bruce"
        val lastName = result?.name?.last ?: "Lee"
        val password = "XinChaoVietNam@2024"
        PageManager(Browser.firefox, "https://mega.nz/register").apply {
            addPageList(
                    arrayListOf(
                            MegaRegisterPage(firstName, lastName, email, password) {
                                println("net.scr0pt.thirdservice.mega.MegaRegisterPage finish")
                                collection.insertOne(
                                        org.bson.Document("User_Name", email)
                                                .append("Password", password).append("firstName", firstName)
                                                .append("lastName", lastName).append("verify_email", false)
                                )
                            },
                            MegaRegisterConfirmEmailPage(gmailUsername, gmailPassword, startTime) {
                                println("net.scr0pt.thirdservice.mega.MegaRegisterConfirmEmailPage finish")
                                collection.updateOne(org.bson.Document("User_Name", email), Updates.set("verify_email", true))
                            },
                            MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(
                                    gmailUsername,
                                    gmailPassword,
                                    password,
                                    startTime
                            ) {
                                println("net.scr0pt.thirdservice.mega.MegaRegisterEnterPasswordAfterEnterConfirmLinkPage finish")
                            },
                            MegaChooseAccTypePage() {
                                println("net.scr0pt.thirdservice.mega.MegaChooseAccTypePage finish")
                            },
                            MegaDownloadAppPage() {
                                println("net.scr0pt.thirdservice.mega.MegaDownloadAppPage finish")
                            },
                            MegaGetRecoverKeyPage() {
                                println("net.scr0pt.thirdservice.mega.MegaGetRecoverKeyPage finish")
                                Thread.sleep(10000)
                                getRecoverKey()?.let { recoverKey ->
                                    //update recover key
                                    println("recoverKey: $recoverKey")

                                }
                            },
                            MegaRecoverKeyDownloadedPage() {
                                println("net.scr0pt.thirdservice.mega.MegaRecoverKeyDownloadedPage finish")
                            },
                            MegaCloudDrivePage() {
                                println("net.scr0pt.thirdservice.mega.MegaCloudDrivePage finish")
                            }
                    )
            )
            run { response ->
                println(response)
                Thread.sleep(20000)
                this.driver.close()
                Thread.sleep(600000)
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
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(firstName, "input#register-firstname-registerpage2")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(lastName, "input#register-lastname-registerpage2")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(email, "input#register-email-registerpage2") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input#register-password-registerpage2")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input#register-password-registerpage3")
                ?: return PageResponse.NOT_FOUND_ELEMENT()

        //I understand that if I lose my password, I may lose my data. Read more about MEGA’s end-to-end encryption.
        driver.clickFirstEl(".checkbox-block.pw-remind .understand-check input.checkboxOff")

        //I agree with the MEGA Terms of Service
        driver.clickFirstEl("input#register-check-registerpage2")
        driver.clickFirstEl("form#register_form .register-button.active")

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/register") &&
                    doc.selectFirst("form#register_form .account.top-header.wide")?.text() == "Create your free account" &&
                    MegaRegisterConfirmEmailPage("", "", 0L)._detect(doc, currentUrl, title) == false
}

class MegaRegisterConfirmEmailPage(
        private val gmailUsername: String,
        private val gmailPassword: String,
        private val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    private var gmail: Gmail? = null

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
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
                                                driver.get(confirmLink)
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

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/register") &&
                    doc.selectFirst(".registration-page-success.special .reg-success-special .reg-success-txt")?.text()?.trim() == "Please check your email and click the link to confirm your account." &&
                    doc.selectFirst(".reg-resend-button-bl .resend-email-button")?.text()?.trim() == "Resend"
}

class MegaRegisterEnterPasswordAfterEnterConfirmLinkPage(
        val gmailUsername: String,
        val gmailPassword: String,
        val password: String,
        val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false
    var gmail: Gmail? = null

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        val doc = driver.doc ?: return PageResponse.WAITING_FOR_RESULT()

        val notiText = doc.selectFirst(".main-top-info-block .main-top-info-text")?.text()
        if (notiText == "Please enter your password to confirm your account.") {
            if (doc.selectFirst("#login_form .account.top-header.login")?.text()?.trim() == "Confirm your account" &&
                    doc.selectFirst("#login_form .big-red-button.login-button.button")?.text()?.trim() == "Confirm your account"
            ) {
                driver.sendKeysFirstEl(password, "input#login-password2") ?: return PageResponse.NOT_FOUND_ELEMENT()
                driver.clickFirstEl("#login_form .big-red-button.login-button.button")
                        ?: return PageResponse.NOT_FOUND_ELEMENT()
                return PageResponse.WAITING_FOR_RESULT()
            }
        } else if (notiText == "Your confirmation link is no longer valid. Your account may already be activated or you may have cancelled your registration.") {
            if (gmail == null) {
                gmail = Gmail(gmailUsername, gmailPassword)?.apply {
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
                                                    driver.get(confirmLink)
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
            return PageResponse.WAITING_FOR_RESULT()
        }
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/confirm") && doc.selectFirst(".main-top-info-block .main-top-info-text")?.text()?.isNotEmpty() == true
                    && doc.selectFirst(".fm-dialog.warning-dialog-a .fm-notification-info h1") == null
}

class MegaChooseAccTypePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstEl(By.className("loading-info"), filter = { el -> el.isDisplayed == false })
        driver.clickFirstEl(".key .plans .reg-st3-membership-bl.free .membership-pad-bl")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/pro") &&
                    doc.selectFirst(".bottom-page.top-info .big-header")?.text()?.trim() == "Choose your account type"
}

class MegaGetRecoverKeyPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/fm/") &&
                    doc.selectFirst(".post-register .step-main-question.post-register")?.text()?.trim() == "Here is your Recovery Key!" &&
                    doc.selectFirst(".improved-recovery-steps .recover-paste-block .right-section > div:not(.hidden)")?.text()?.trim() == "Download key" &&
                    !doc.selectFirst(".fm-dialog.recovery-key-dialog.backup-recover.improved-recovery-steps.post-register").hasClass(
                            "hidden"
                    )
}

class MegaRecoverKeyDownloadedPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".content-wrapper .default-green-button.close-dialog")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/fm/") &&
                    doc.selectFirst(".fm-dialog-header .fm-dialog-title.top-pad")?.text()?.trim() == "Account Recovery" &&
                    doc.selectFirst(".content-wrapper .default-green-button.close-dialog")?.text()?.trim() == "OK" &&
                    !doc.selectFirst(".fm-dialog.recovery-key-info.improved-recovery-steps").hasClass("hidden")
}

class MegaDownloadAppPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".button-wrappers .redirect-clouddrive-link") ?: return PageResponse.NOT_FOUND_ELEMENT()

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/downloadapp") &&
                    doc.selectFirst(".download-app")?.text()?.trim() == "Download the MEGA App" &&
                    doc.selectFirst(".button-wrappers .redirect-clouddrive-link")?.text() == "Skip this step"
}

class MegaCloudDrivePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        return PageResponse.OK()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/fm/") &&
                    doc.selectFirst(".cloud-drive .nw-fm-tree-header.cloud-drive input")?.attr("placeholder")?.trim() == "Cloud Drive" &&
                    doc.selectFirst("#how-to-upload .dropdown.hint-info-block .dropdown.hint-header")?.text()?.trim() == "How to upload"
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