package net.scr0pt.thirdservice.openload

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.RobotManager
import org.bson.Document
import java.awt.event.KeyEvent
import java.util.*

/**
 * Created by Long
 * Date: 10/25/2019
 * Time: 8:17 PM
 */


fun main() {
    val gmailUsername = "rmargotd98e774be42153db0d49afa@gmail.com"

    val infinityMail = InfinityMail(gmailUsername)
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("openload")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("openload-account")

    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }

    do {
        val email = infinityMail.getNext()?.fullAddress ?: break

        if (emails.contains(email)) continue
        emails.add(email)
        val password = "Alan_${System.currentTimeMillis()}"

        openloadRegister(RobotManager(), email, password, collection)

    } while (true)


}


fun openloadRegister(robotManager: RobotManager, email: String, password: String, collection: MongoCollection<Document>) {
    with(robotManager) {
        run.exec("\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\" -incognito -start-maximized")

//        run.exec("\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\" -private-window")
        longSleep()

//        switchWindow()


        browserGoTo("openload.co/register")

        val emailInput = Pair(500, 260 + browerType.baseY())
        val ckeckBoxAgree = Pair(110, 630 + browerType.baseY())
//        val ckeckBoxAgree = Pair(400, 630 + browerType.baseY())
        val multipleCorrect = Pair(280, 480 + browerType.baseY())
//        val multipleCorrect = Pair(563, 480 + browerType.baseY())
        val newCapthchaBtn = Pair(563, 440 + browerType.baseY())
//        val newCapthchaBtn = Pair(563, 570 + browerType.baseY())
        val initialResolveCaptchaBtn = Pair(280, 640 + browerType.baseY())
//        val initialResolveCaptchaBtn = Pair(563, 835 + browerType.baseY())
        val submitBtn = Pair(160, 425 + robotManager.browerType.baseY())
//        val submitBtn = Pair(440, 665 + robotManager.browerType.baseY())


        click(emailInput)
        clearAndPasteInput(email)

        tab()
        clearAndPasteInput(password)

        tab()
        clearAndPasteInput(password)

        clearClipboard()


        click(ckeckBoxAgree)//I agree on Openload terms.  You must agree to the Openload Terms of Service

        robot.keyPress(KeyEvent.VK_END)
        robot.keyRelease(KeyEvent.VK_END)

        shiftTab()
        shiftTab()
        shiftTab()
        shiftTab()
        enter()

        bypassCaptcha(initialResolveCaptchaBtn, multipleCorrect, newCapthchaBtn, robotManager, onSuccess = {
            robotManager.click(submitBtn)
            longSleep()

            robotManager.click(5 * robotManager.screenSize.width / 6, robotManager.screenSize.height / 2)//safe screen point

            val txt = robotManager.printScreenText()
            if (txt.contains("An Account with this Email exists already.")) {
                collection.insertOne(
                        Document("email", email).append("temp_ban", null).append("created_at", Date()).append("updated_at", Date())
                )
            } else if (txt.contains("OpenloadUpload User Panel Support Logout")) {
                collection.insertOne(
                        Document("email", email).append("password", password).append("temp_ban", null).append("created_at", Date()).append("updated_at", Date())
                )
            } else {

            }
            robotManager.closeWindow()

        }, onFail = {})
    }
}


fun bypassCaptcha(initialResolveCaptchaBtn: Pair<Int, Int>? = null, multipleCorrect: Pair<Int, Int>, newCapthchaBtn: Pair<Int, Int>, robotManager: RobotManager, onSuccess: () -> Unit, onFail: () -> Unit, onSpecialCase: (() -> Unit)? = null) {
    with(robotManager) {
        longSleep()
        initialResolveCaptchaBtn?.let { click(it) }

        for (i in 0..40) {
            sleep()
            val text = printScreenText().trim()
            when {
                text.contains("I'm not a robot\nPrivacy - Terms") -> {
                    onSuccess()
                    return@bypassCaptcha
                }
                text == "Multiple correct solutions required - please solve more.\nPress PLAY and enter the words you hear\nPLAY\nVERIFY" -> {
                    click(multipleCorrect)
                    bypassCaptcha(null, multipleCorrect, newCapthchaBtn, robotManager, onSuccess, onFail)
                    return@bypassCaptcha
                }
                text == "Try again later\nYour computer or network may be sending automated queries. To protect our users, we can't process your request right now. For more details visit our help page" -> {
                    println("onFail")
                    onFail()
                    return@bypassCaptcha
                }
            }
        }

        var text: String
        do {
            sleep()
            text = printScreenText().trim()
        } while (text == "Press PLAY and enter the words you hear")

        //Press PLAY and enter the words you hear: verifying
        if (text == "Press PLAY and enter the words you hear\nPLAY\nVERIFY") {
            onFail()
        } else {
            print("sdfsdfsdfsdf: $text")
            onSpecialCase?.let { it() }
        }
    }
}


