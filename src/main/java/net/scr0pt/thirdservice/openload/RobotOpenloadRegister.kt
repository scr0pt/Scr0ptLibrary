package net.scr0pt.thirdservice.openload

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.SystemClipboard
import org.bson.Document
import java.awt.event.KeyEvent
import java.util.*

/**
 * Created by Long
 * Date: 10/25/2019
 * Time: 8:17 PM
 */


 fun main() {
    val gmailUsername = "vanlethi74@gmail.com"
    val gmailPassword = "XinChaoVietNam@@2000"

    val infinityMail = InfinityMail(gmailUsername)
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("openload")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("openload-account")

    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }

    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (email.contains("vanlethi74")) continue

        if (emails.contains(email)) continue
        emails.add(email)
        val password = "XinChaoVietNam@1990"

        openloadRegister(RobotManager(), email, password, collection)

    } while (true)


}


 fun openloadRegister(robotManager: RobotManager, email: String, password: String, collection: MongoCollection<Document>) {
    with(robotManager) {
        run.exec("\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\" -incognito -start-maximized")

//        run.exec("\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\" -private-window")
        longSleep()

//        switchWindow()


        mouseMove(500, 50)

        leftClick()
        ctrA()

        robot.keyPress(KeyEvent.VK_DELETE)

//        openload.co/register
        robot.keyPress(KeyEvent.VK_O)
        robot.keyPress(KeyEvent.VK_P)
        robot.keyPress(KeyEvent.VK_E)
        robot.keyPress(KeyEvent.VK_N)
        robot.keyPress(KeyEvent.VK_L)
        robot.keyPress(KeyEvent.VK_O)
        robot.keyPress(KeyEvent.VK_A)
        robot.keyPress(KeyEvent.VK_D)
        robot.keyPress(KeyEvent.VK_PERIOD)
        robot.keyPress(KeyEvent.VK_C)
        robot.keyPress(KeyEvent.VK_O)
        robot.keyPress(KeyEvent.VK_SLASH)
        robot.keyPress(KeyEvent.VK_R)
        robot.keyPress(KeyEvent.VK_E)
        robot.keyPress(KeyEvent.VK_G)
        robot.keyPress(KeyEvent.VK_I)
        robot.keyPress(KeyEvent.VK_S)
        robot.keyPress(KeyEvent.VK_T)
        robot.keyPress(KeyEvent.VK_E)
        robot.keyPress(KeyEvent.VK_R)
        sleep()

        robot.keyPress(KeyEvent.VK_ENTER)
        longSleep()

        mouseMove(500, 260 + browerType.baseY())
        leftClick()
        SystemClipboard.copy(email)
        clearInput()
        ctrV()

        mouseMove(500, 360 + browerType.baseY())
        leftClick()
        SystemClipboard.copy(password)
        clearInput()
        ctrV()

        mouseMove(500, 460 + browerType.baseY())
        leftClick()
        SystemClipboard.copy(password)
        clearInput()
        ctrV()

        clearClipboard()

        mouseMove(400, 630 + browerType.baseY())
        leftClick()


        mouseMove(500, 570 + browerType.baseY())
        leftClick()

        mouseMove(563, 835 + browerType.baseY())
        leftClick()

        bypassCaptcha(Pair(563, 690 + browerType.baseY()), Pair(563, 570 + browerType.baseY()), robotManager, onSuccess =  {
            robotManager.mouseMove(440, 665 + robotManager.browerType.baseY())
            robotManager.leftClick()

            var url: String
            do {
                Thread.sleep(1000)
                url = robotManager.getCurrentUrl()
            } while (url != "https://openload.co/")

            collection.insertOne(
                    Document("email", email).append("password", password).append("temp_ban", null).append("created_at", Date()).append("updated_at", Date())
            )
            robotManager.closeWindow()
        }, onFail = {})
    }
}


 fun bypassCaptcha(multipleCorrect: Pair<Int, Int>, newCapthchaBtn: Pair<Int, Int>, robotManager: RobotManager, onSuccess: () -> Unit, onFail: () -> Unit) {
    with(robotManager) {
        realylongSleep()
        realylongSleep()
        var text: String
        do {
            sleep()
            text = printScreenText().trim()
        } while (text == "Press PLAY and enter the words you hear")

        //Press PLAY and enter the words you hear: verifying

        //\nI'm not a robot\nPrivacy - Terms : done
        if (text.contains("I'm not a robot\nPrivacy - Terms")) {
            onSuccess()

        } else if (text == CLIPBOARD_DELETED) {
            mouseMove(newCapthchaBtn)
            leftClick()

            if (printScreenText().trim() == "I'm not a robot\nPrivacy - Terms") {
                onSuccess()
            } else {
                println("onFail")
                onFail()
            }
        } else if (text == "Try again later\nYour computer or network may be sending automated queries. To protect our users, we can't process your request right now. For more details visit our help page") {
            println("onFail")
            onFail()

//            onFail()
        } else if (text == "Multiple correct solutions required - please solve more.\nPress PLAY and enter the words you hear\nPLAY\nVERIFY") {
            mouseMove(multipleCorrect)
            leftClick()

            bypassCaptcha(multipleCorrect, newCapthchaBtn, robotManager, onSuccess,onFail)

        } else if (text == "Press PLAY and enter the words you hear\nPLAY\nVERIFY") {
            onFail()
        } else {
            print("sdfsdfsdfsdf: $text")
        }
    }
}


