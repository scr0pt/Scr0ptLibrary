package net.scr0pt.thirdservice.mlab

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates

import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.selenium.bypassCaptcha
import net.scr0pt.thirdservice.google.*
import net.scr0pt.thirdservice.google.GoogleSearch
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.SystemClipboard

import net.scr0pt.utils.webdriver.DriverElements
import net.scr0pt.utils.webdriver.DriverManager
import org.bson.Document
import java.awt.event.KeyEvent

/**
 * Created by Long
 * Date: 10/15/2019
 * Time: 11:07 AM
 *
 */

fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("mlab")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("mlab-account")
//    processLogin(collection)
    processRegister(collection)
}

fun processLogin(collection: MongoCollection<org.bson.Document>) {
    collection.find(Filters.and(Filters.exists("cluster_builded", false), Filters.exists("cookies", false))).forEach {
        login(it.getString("email"), it.getString("password"), collection)
    }
}

fun processRegister(collection: MongoCollection<org.bson.Document>) {
    val infinityMail = InfinityMail("tranvananh.200896@gmail.com")
    val emails = arrayListOf<String>()
    collection.find().forEach { doc -> doc?.getString("email")?.let { emails.add(it) } }
    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (emails.contains(email)) continue
        emails.add(email)
        if (email != "t.ranvananh.2.00.896@gmail.com") {
            val result = FakeProfile.getNewProfile()
            val first = result?.name?.first ?: "Bruce"
            val last = result?.name?.last ?: "Lee"
            println(email)
            println(first)
            println(last)
            registerMlab(
                    email = email,
                    firstName = first,
                    lastName = last,
                    collection = collection
            )
        }
    } while (true)
}

fun loginGoogle(email: String, password: String, driver: DriverManager, onLoginSuccess: () -> Unit, onLoginFail: ((pageResponse: Response?) -> Unit)? = null, recoverEmail: String? = null) {
    println("loginGoogle: $email $password")
    PageManager(driver,
            "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
    ).apply {
        addPageList(arrayListOf(
                LoginEnterEmailPage(email) {
                    println("enter email success")
                },
                LoginEnterPasswordPage(password) {
                    println("enter password success")
                },
                ProtectYourAccount(defaultAction = ProtectYourAccount.DEFAULT_ACTION.DONE) {
                    println("ProtectYourAccount success")
                },
                VerifyItsYouPhoneNumber {
                    println("VerifyItsYouPhoneNumber success")
                },
                VerifyItsYouPhoneNumberRecieveMessage {
                    println("VerifyItsYouPhoneNumberRecieveMessage success")
                },
                GoogleSearch {
                    println("GoogleSearch success")
                },
                VerifyItsYouPhoneDevice {
                    println("VerifyItsYouPhoneDevice success")
                },
                CantLoginForYou {
                    println("CantLoginForYou success")
                }
        ))

        recoverEmail?.let {
            addPage(VerifyItsYouRecoverEmail(it) {
                println("VerifyItsYouRecoverEmail success")
            })
        }

        generalWatingResult = { pageStatus ->
            if ((pageStatus.doc?.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
                GoogleResponse.RECAPTCHA()
            } else Response.WAITING()
        }

        run { pageResponse ->
            if (pageResponse is Response.OK) {
                onLoginSuccess()
            } else {
                driver.close()
                onLoginFail?.let { it(pageResponse) }
            }
        }
    }
}


fun registerMlab(
        email: String,
        firstName: String,
        lastName: String,
        collection: MongoCollection<org.bson.Document>
) {
    val password = "XinChaoVietnam"

    val driver = DriverManager(driverType = DriverManager.BrowserType.Chrome, driverHeadless = true)
    PageManager(driver,
            "https://www.mongodb.com/atlas-signup-from-mlab?utm_source=mlab.com&utm_medium=referral&utm_campaign=mlab%20signup&utm_content=blue%20sign%20up%20button"
    ).apply {
        addPageList(
                arrayListOf<Page>(
                        TryMongoDBAtlasPage(email, password, firstName, lastName) {
                            collection.insertOne(
                                    org.bson.Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                            )
                            println("register success")
                        },
                        WelcomePage() {
                            println("WelcomePage success")
                            this.isSuccess = true
                        },
                        BuildClusterPage() {
                            println("BuildClusterPage success")
                        }
                )
        )

        run { pageResponse ->
            if (pageResponse is MlabResponse.LOGIN_ERROR) {
                if (pageResponse.msg == "This email address is already in use.") {
                    if (collection.countDocuments(org.bson.Document("email", email)) == 0L) {
                        collection.insertOne(org.bson.Document("email", email))
                    }
                }
                println(pageResponse.msg)
            }
            println(pageResponse)
            driver.close()
            Thread.sleep(5 * 60000)
        }
    }
}

fun loginMlab(driver: DriverManager, collection: MongoCollection<org.bson.Document>) {
    val email = "v.a.n.a.n.ngu.y.en.0.8.3@gmail.com"
    val password = "XinChaoVietnam"
    val firstName = "Bruce"
    val lastName = "Lee"
    val db_username = "root"
    val db_password = "mongo"

    PageManager(driver,
            "https://www.mongodb.com/atlas-signup-from-mlab?utm_source=mlab.com&utm_medium=referral&utm_campaign=mlab%20signup&utm_content=blue%20sign%20up%20button"
    ).apply {
        addPageList(
                arrayListOf(
                        TryMongoDBAtlasPage(email, password, firstName, lastName) {
                            collection.insertOne(
                                    org.bson.Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                            )
                            println("register success")
                        },
                        WelcomePage() {
                            println("WelcomePage success")
                        },
                        BuildClusterPage() {
                            println("BuildClusterPage success")
                        },
                        CreateClusterTypePage {
                            println("CreateClusterTypePage success")
                        },
                        ClusterCreatingPage() {
                            println("ClusterCreatingPage success")
                        },
                        CreatingDatabaseUserPage() {
                            println("CreatingDatabaseUserPage success")
                        },
                        AddNewUserPage(db_username, db_password) {
                            println("AddNewUserPage success")
                        },
                        CreatingDatabaseUserDonePage() {
                            println("CreatingDatabaseUserDonePage success")
                        },
                        NetworkAccessPage() {
                            println("NetworkAccessPage success")
                        },
                        NetworkAccessAddWhitelistPage {
                            println("NetworkAccessAddWhitelistPage success")
                        },
                        NetworkAccessAddWhitelistDonePage {
                            println("NetworkAccessAddWhitelistDonePage success")
                        }
                )
        )
        run { pageResponse ->
            println(pageResponse)
        }
    }
}

class TryMongoDBAtlasPage(
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun onWaiting(pageStatus: PageStatus): Response? {
        val selectFirst = pageStatus.doc?.selectFirst("div.form-error")
        if (selectFirst != null && selectFirst.attr("style")?.contains("display: none;") == false) {
            return MlabResponse.LOGIN_ERROR(msg = selectFirst.text())
        }
        return null
    }

    val form = DriverElements.Form(
            inputs = arrayListOf(
                    "input#email" to email,
                    "input#first_name" to firstName,
                    "input#last_name" to lastName,
                    "input#password" to password
            ),
            buttons = arrayListOf(
                    "input#atlasCheckbox"
            ),
            submitBtn = "input#atlas-submit-btn"

    )

    override fun action(pageStatus: PageStatus): Response {
        form.submit(pageStatus.driver)
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus): Boolean {
        return form.selectors.all { pageStatus.driver.findFirstEl(it) != null }
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.title == "Sign Up for MongoDB Atlas | Cloud MongoDB Hosting | MongoDB" &&
                    pageStatus.equalsText("h1.txt-center", "Try MongoDB Atlas")
}

class WelcomePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.title == "Welcome | Cloud: MongoDB Cloud" &&
                pageStatus.url.startsWith("https://cloud.mongodb.com/user#/atlas/register/welcomeBot")
    }
}

class BuildClusterPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".path-selector-door-footer-starter .path-selector-door-submit")
        return Response.WAITING()
    }

    override fun isReady(pageStatus: PageStatus) =
            pageStatus.driver.findFirstEl(".path-selector-door-footer-starter .path-selector-door-submit") != null

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com/v2/") &&
                pageStatus.url.endsWith("#clusters/pathSelector") &&
                pageStatus.title == "Choose a Path | Atlas: MongoDB Atlas" &&
                pageStatus.equalsText("span.path-selector-header-title", "MONGODB ATLAS") &&
                pageStatus.equalsText("span.path-selector-header-main-text", "Choose a path. Adjust anytime.")
    }
}

class CreateClusterTypePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("button[type=\"button\"]:containsOwn(Create Cluster)")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.equalsText("header.editor-layout-header h1 strong", "Create a Starter Cluster") &&
                pageStatus.notContain("button[type=\"button\"]:containsOwn(Create Cluster)")
    }
}

class ClusterCreatingPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".left-nav a:containsOwn(Database Access)")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.equalsText(".nds-sparkline-empty-header", "Your cluster is being created")
    }
}

class CreatingDatabaseUserPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".section-controls-is-end-justified .button-is-primary")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("database/users") &&
                pageStatus.equalsText(".empty-view-text-is-heading", "Create a database user" )&&
                pageStatus.equalsText(".section-controls-is-end-justified .button-is-primary", "Add New User" )&&
                pageStatus.notContain("button[name=\"deleteUser\"]")
    }
}

class AddNewUserPage(
        val username: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(username, "input[name=\"user\"]") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(password, "input[name=\"password\"]") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("button[type=\"submit\"]:containsOwn(Add User)")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("database/users") == true &&
                pageStatus.equalsText(".nds-edit-modal-footer-checkbox-description", "Save as temporary user") &&
                pageStatus.equalsText("h3.view-modal-header-title", "Add New User")
    }
}


class CreatingDatabaseUserDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".left-nav a:containsOwn(Network Access)") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("database/users") == true &&
                pageStatus.notContain(".empty-view-text-is-heading") &&
                pageStatus.equalsText(".section-controls-is-end-justified .button-is-primary","Add New User") &&
                pageStatus.equalsText("button[name=\"deleteUser\"]", "Delete")
    }
}

class NetworkAccessPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".section-controls-is-end-justified .button-is-primary")
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("network/whitelist") &&
                pageStatus.equalsText("h1.section-header-title", "Network Access" )&&
                pageStatus.equalsText(".section-controls-is-end-justified .button-is-primary", "Add IP Address")
    }
}

class NetworkAccessAddWhitelistPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl("button[name=\"allowAccessAnywhere\"]") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl("button[name=\"confirm\"]") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("/network/whitelist/addToWhitelist") &&
                pageStatus.equalsText("header.view-modal-header h3.view-modal-header-title", "Add Whitelist Entry")
    }
}

class NetworkAccessAddWhitelistDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.clickFirstEl(".left-nav a:containsOwn(Clusters)") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url.startsWith("https://cloud.mongodb.com") &&
                pageStatus.url.endsWith("network/whitelist") &&
                pageStatus.equalsText("td.plain-table-cell", "0.0.0.0/0 (includes your current IP address)")
    }
}

fun login(email: String, password: String, collection: MongoCollection<org.bson.Document>) {
    RobotManager().apply {
        openBrowser()
        browserGoTo("https://cloud.mongodb.com/user#/atlas/login")

        clearAndPasteInput(email)
        enter()
        sleep()
        enter()

        longSleep()

        clearAndPasteInput(password)
        enter()


        val initialResolveCaptchaBtn = Pair(885, 915)
        bypassCaptcha(initialResolveCaptchaBtn, initialResolveCaptchaBtn, initialResolveCaptchaBtn, this, onSuccess = {

        }, onFail = {
            println("Fail")
        }, onSpecialCase = {

            if (printScreenText().contains("Clusters\n" +
                            "Find a cluster...\n" +
                            "Create a cluster\n" +
                            "Create a cluster\n" +
                            "Choose your cloud provider, region, and specs.\n" +
                            "Build a Cluster")) {

                println(1)
                val buildAClusterPosition = Pair(1050, 540)
                click(buildAClusterPosition)
                println(2)
                waitUntilUrlEndWith("#clusters/pathSelector")
                println(3)

                val createACluserPosition = Pair(630, 880)
                click(createACluserPosition)
                println(4)

                longSleep()
                click(screenSize.width / 2, 130)//click safe point
                val txt = printScreenText()
                if (txt.contains("Your cluster name is used to generate your hostname and cannot be changed later") && txt.contains("Enter cluster name")) {
                    click((screenSize.width / (2.6)).toInt(), (screenSize.height / (2.33)).toInt())
                    clearAndPasteInput("MyCluster")
                    click((screenSize.width / (1.43)).toInt(), (screenSize.height / (1.92)).toInt())
                    longSleep()
                }

                robot.keyPress(KeyEvent.VK_END)
                robot.keyRelease(KeyEvent.VK_END)
                println(5)

                val finalcreateACluserPosition = Pair(1300, 1000)
                click(finalcreateACluserPosition)
                println(6)


                val initialResolveCaptchaBtn = Pair(875, 1000)
                val multipleCorrect = Pair(900, 1000)
                bypassCaptcha(initialResolveCaptchaBtn, multipleCorrect, initialResolveCaptchaBtn, this, onSuccess = {

                }, onFail = {

                }, onSpecialCase = {
                    println(6)
                    click(screenSize.width / 2, 130)//click safe point
                    val text = printScreenText()
                    if (text.contains("Your cluster is being created") && text.contains("New clusters take between 7-10 minutes to provision.")) {
                        println("Your cluster is being created")
                        val cookieStr = getCookieStr(this)

                        if (cookieStr != "") {
                            collection.updateOne(Document("email", email), Updates.combine(
                                    Updates.set("cookies", cookieStr),
                                    Updates.set("cluster_builded", true)
                            ))
                        } else {
                            collection.updateOne(Document("email", email), Updates.combine(
                                    Updates.set("cluster_builded", true)
                            ))
                        }
                        closeWindow()
                    }
                })
            } else if (getCurrentUrl().endsWith("#clusters")) {
                val cookieStr = getCookieStr(this)
                if (cookieStr != "") {
                    collection.updateOne(Document("email", email), Updates.combine(
                            Updates.set("cookies", cookieStr)
                    ))
                }
                closeWindow()
            }
        })

    }
}

fun getCookieStr(robotManager: RobotManager): String {
    println("getCookieStr")
    with(robotManager) {

        robot.keyPress(KeyEvent.VK_F12)
        robot.keyRelease(KeyEvent.VK_F12)
        click(4 * screenSize.width / 5, 4 / screenSize.height / 5)

        SystemClipboard.copy("")

        clearAndPasteInput("""
                            const copyToClipboard = str => {
                              const el = document.createElement('textarea');
                              el.value = str;
                              document.body.appendChild(el);
                              el.select();
                              document.execCommand('copy');
                              document.body.removeChild(el);
                            };
                            copyToClipboard(document.cookie)
                        """.trimIndent())
        enter()

        return SystemClipboard.get()
    }
}
