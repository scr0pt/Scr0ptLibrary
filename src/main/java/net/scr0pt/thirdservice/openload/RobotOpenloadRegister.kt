package net.scr0pt.thirdservice.openload

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.selenium.bypassCaptcha
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
        openBrowser()
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


