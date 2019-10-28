package net.scr0pt.thirdservice.mlab

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.bot.MlabPageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.bot.google.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.thirdservice.openload.bypassCaptcha
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.SystemClipboard
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document
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
    processLogin(collection)
//    processRegister(collection)
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
                    driver = Browser.chrome,
                    collection = collection
            )
        }
    } while (true)
}

fun loginGoogle(email: String, password: String, driver: DriverManager, onLoginSuccess: () -> Unit, onLoginFail: ((pageResponse: PageResponse?) -> Unit)? = null, recoverEmail: String? = null) {
    println("loginGoogle: $email $password")
    PageManager(driver,
            "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
    ).apply {
        addPageList(arrayListOf<Page>(
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

        generalWatingResult = { jsoupDoc, currentUrl ->
            if ((jsoupDoc.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
                PageResponse.RECAPTCHA()
            } else PageResponse.WAITING_FOR_RESULT()
        }

        run { pageResponse ->
            if (pageResponse is PageResponse.OK) {
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
        driver: DriverManager,
        collection: MongoCollection<org.bson.Document>
) {
    val password = "XinChaoVietnam"

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
                        BuildClusterPage() {
                            println("BuildClusterPage success")
                        }
                )
        )

        run { pageResponse ->
            println(pageResponse)
            Thread.sleep(60000)
            driver.close()
            Thread.sleep(180000)
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
                arrayListOf<Page>(
                        TryMongoDBAtlasPage(email, password, firstName, lastName) {
                            collection.insertOne(
                                    org.bson.Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                            )
                            println("register success")
                        },
                        BuildClusterPage() {
                            println("BuildClusterPage success")
                        },
                        CreateClusterTypePage {
                            println("BuildClusterPage success")
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
            if (pageResponse is MlabPageResponse.LOGIN_ERROR) {
                println(pageResponse.msg)
            }

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
    override fun isEndPage() = false

    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {
        val selectFirst = doc.selectFirst("div.form-error")
        if (selectFirst != null && selectFirst.attr("style")?.contains("display: none;") == false) {
            return MlabPageResponse.LOGIN_ERROR(msg = selectFirst.text())
        }
        return null
    }

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(email, "input#email") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(firstName, "input#first_name") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(lastName, "input#last_name") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input#password") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("input#atlasCheckbox") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("input#atlas-submit-btn") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            doc.selectFirst("h1.txt-center")?.text() == "Try MongoDB Atlas"
}

class BuildClusterPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".path-selector-door-footer-starter .path-selector-door-submit")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") &&
                doc.selectFirst("span.path-selector-header-title")?.text() == "MONGODB ATLAS" &&
                doc.selectFirst("span.path-selector-header-main-text")?.text() == "Choose a path. Adjust anytime."
    }
}

class CreateClusterTypePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl("button[type=\"button\"]:containsOwn(Create Cluster)")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") &&
                doc.selectFirst("header.editor-layout-header h1 strong")?.text() == "Create a Starter Cluster" &&
                doc.selectFirst("button[type=\"button\"]:containsOwn(Create Cluster)") != null
    }
}

class ClusterCreatingPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".left-nav a:containsOwn(Database Access)") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") &&
                doc.selectFirst(".nds-sparkline-empty-header")?.text() == "Your cluster is being created"
    }
}

class CreatingDatabaseUserPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".section-controls-is-end-justified .button-is-primary")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("database/users") &&
                doc.selectFirst(".empty-view-text-is-heading")?.text() == "Create a database user" &&
                doc.selectFirst(".section-controls-is-end-justified .button-is-primary")?.text() == "Add New User" &&
                doc.selectFirst("button[name=\"deleteUser\"]") == null
    }
}

class AddNewUserPage(
        val username: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(username, "input[name=\"user\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input[name=\"password\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("button[type=\"submit\"]:containsOwn(Add User)") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("database/users") &&
                doc.selectFirst(".nds-edit-modal-footer-checkbox-description")?.text() == "Save as temporary user" &&
                doc.selectFirst("h3.view-modal-header-title")?.text() == "Add New User"
    }
}


class CreatingDatabaseUserDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".left-nav a:containsOwn(Network Access)") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("database/users") &&
                doc.selectFirst(".empty-view-text-is-heading") == null &&
                doc.selectFirst(".section-controls-is-end-justified .button-is-primary")?.text() == "Add New User" &&
                doc.selectFirst("button[name=\"deleteUser\"]")?.text() == "Delete"
    }
}

class NetworkAccessPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".section-controls-is-end-justified .button-is-primary")
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("network/whitelist") &&
                doc.selectFirst("h1.section-header-title")?.text() == "Network Access" &&
                doc.selectFirst(".section-controls-is-end-justified .button-is-primary")?.text() == "Add IP Address"
    }
}

class NetworkAccessAddWhitelistPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl("button[name=\"allowAccessAnywhere\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl("button[name=\"confirm\"]") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("/network/whitelist/addToWhitelist") &&
                doc.selectFirst("header.view-modal-header h3.view-modal-header-title")?.text() == "Add Whitelist Entry"
    }
}

class NetworkAccessAddWhitelistDonePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.clickFirstEl(".left-nav a:containsOwn(Clusters)") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("network/whitelist") &&
                doc.selectFirst("td.plain-table-cell")?.text() == "0.0.0.0/0 (includes your current IP address)"
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
                            collection.updateOne(org.bson.Document("email", email), Updates.combine(
                                    Updates.set("cookies", cookieStr),
                                    Updates.set("cluster_builded", true)
                            ))
                        } else {
                            collection.updateOne(org.bson.Document("email", email), Updates.combine(
                                    Updates.set("cluster_builded", true)
                            ))
                        }
                        closeWindow()
                    }
                })
            } else if (getCurrentUrl().endsWith("#clusters")) {
                val cookieStr = getCookieStr(this)
                if (cookieStr != "") {
                    collection.updateOne(org.bson.Document("email", email), Updates.combine(
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
