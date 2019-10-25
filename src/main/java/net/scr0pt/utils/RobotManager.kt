package net.scr0pt.utils

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.Browser
import org.bson.Document
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.*


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

        RobotManager().apply {
            openFirefox(email, password, collection)
        }

    } while (true)


}


class RobotManager {
    val robot = Robot()
    val run = Runtime.getRuntime()
    val chromeBaseY = 30

    fun sleep() {
        Thread.sleep(1000)
    }

    fun longSleep() {
        Thread.sleep(5000)
    }

    fun switchWindow() {
        robot.keyPress(KeyEvent.VK_ALT)
        robot.keyPress(KeyEvent.VK_TAB)
        robot.keyRelease(KeyEvent.VK_ALT)
        robot.keyRelease(KeyEvent.VK_TAB)
        sleep()
    }

    fun leftClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        sleep()
    }

    fun clearInput() {
        ctrA()
        robot.keyPress(KeyEvent.VK_DELETE)
        sleep()
    }

    fun ctrA() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_A)
        sleep()
    }

    fun ctrC() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_C)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_C)
        sleep()
    }

    fun ctrV() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_V)
        sleep()
    }

    fun mouseMove(x: Int, y: Int) {
        robot.mouseMove(x, y)
        println("mouseMove to ${x}:${y} color: ${robot.getPixelColor(x, y)}")
        sleep()
    }

    fun openFirefox(email: String, password: String, collection: MongoCollection<Document>) {
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

        mouseMove(500, 260 + chromeBaseY)
        leftClick()
        SystemClipboard.copy(email)
        clearInput()
        ctrV()

        mouseMove(500, 360 + chromeBaseY)
        leftClick()
        SystemClipboard.copy(password)
        clearInput()
        ctrV()

        mouseMove(500, 460 + chromeBaseY)
        leftClick()
        SystemClipboard.copy(password)
        clearInput()
        ctrV()

        clearClipboard()

        mouseMove(400, 630 + chromeBaseY)
        leftClick()


        mouseMove(500, 570 + chromeBaseY)
        leftClick()

        mouseMove(563, 835 + chromeBaseY)
        leftClick()

        bypassCaptcha(email, password, collection)
    }

    fun printScreenText(): String {
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~")
        ctrA()
        ctrC()
        val clipboardTxt = SystemClipboard.get()
        SystemClipboard.copy("SystemClipboardDeleted")
        println(clipboardTxt)
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~")
        return clipboardTxt
    }

    fun clearClipboard() {
        SystemClipboard.copy("SystemClipboardDeleted")
    }

    fun getCurrentUrl(): String {
        mouseMove(500, 50)
        leftClick()
        return printScreenText()
    }

    fun closeWindow() {
        mouseMove(Toolkit.getDefaultToolkit().getScreenSize().width - 13, 13)
        leftClick()
    }

    fun bypassCaptcha(email: String, password: String, collection: MongoCollection<Document>) {
        longSleep()
        longSleep()
        var text: String
        do {
            Thread.sleep(1000)
            text = printScreenText().trim()
        } while (text.equals("Press PLAY and enter the words you hear"))

        //Press PLAY and enter the words you hear: verifying

        //\nI'm not a robot\nPrivacy - Terms : done
        if (text.contains("I'm not a robot\nPrivacy - Terms")) {
            onSuccess(collection, email, password)

        } else if (text.equals("SystemClipboardDeleted")) {
            mouseMove(563, 570 + chromeBaseY)
            leftClick()

            if (printScreenText().trim() == "I'm not a robot\nPrivacy - Terms") {
                onSuccess(collection, email, password)
            } else {
                println("onFail")
                closeWindow()
            }
        } else if (text.equals("Try again later\nYour computer or network may be sending automated queries. To protect our users, we can't process your request right now. For more details visit our help page")) {
            println("onFail")
            closeWindow()

//            onFail()
        } else if (text.equals("Multiple correct solutions required - please solve more.\nPress PLAY and enter the words you hear\nPLAY\nVERIFY")) {
            mouseMove(563, 690 + chromeBaseY)
            leftClick()

            bypassCaptcha(email, password, collection)

        }  else if (text.equals("Press PLAY and enter the words you hear\nPLAY\nVERIFY")) {
            closeWindow()
        } else {
            print("sdfsdfsdfsdf: $text")
        }
    }

    private fun onSuccess(collection: MongoCollection<Document>, email: String, password: String) {
        mouseMove(440, 665 + chromeBaseY)
        leftClick()

        var url: String
        do {
            Thread.sleep(1000)
            url = getCurrentUrl()
        } while (!url.equals("https://openload.co/"))

        collection.insertOne(
                Document("email", email).append("password", password).append("temp_ban", null).append("created_at", Date()).append("updated_at", Date())
        )
        closeWindow()
    }
}
