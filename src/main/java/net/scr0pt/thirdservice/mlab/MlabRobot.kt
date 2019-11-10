package net.scr0pt.thirdservice.mlab

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.crawl.school.random
import net.scr0pt.selenium.MlabResponse
import net.scr0pt.selenium.PageManager
import net.scr0pt.selenium.bypassCaptcha
import net.scr0pt.selenium.isCaptchaOpen
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.RobotManager
import net.scr0pt.utils.SystemClipboard
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
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("mlab-account")
    processLogin(collection)
//    processRegister(collection)
}

fun processLogin(collection: MongoCollection<Document>) {
    collection.random(Filters.and(Filters.exists("cluster_builded", false), Filters.exists("cookies", false)))?.let {
        robotLoginMlab(it.getString("email"), it.getString("password"), collection)
    }
}

fun processRegister(collection: MongoCollection<Document>) {
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

fun registerMlab(
        email: String,
        firstName: String,
        lastName: String,
        collection: MongoCollection<Document>
) {
    val password = "XinChaoVietnam"

    val driver = DriverManager(driverType = DriverManager.BrowserType.Chrome, driverHeadless = true)
    PageManager(driver,
            "https://www.mongodb.com/atlas-signup-from-mlab?utm_source=mlab.com&utm_medium=referral&utm_campaign=mlab%20signup&utm_content=blue%20sign%20up%20button"
    ).apply {
        addPageList(
                arrayListOf(
                        TryMongoDBAtlasPage(email, password, firstName, lastName) {
                            collection.insertOne(
                                    Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                            )
                            println("register success")
                        },
                        WelcomePage {
                            println("WelcomePage success")
                            this.isSuccess = true
                        },
                        BuildClusterPage {
                            println("BuildClusterPage success")
                        }
                )
        )

        run { pageResponse ->
            if (pageResponse is MlabResponse.LOGIN_ERROR) {
                if (pageResponse.msg == "This email address is already in use.") {
                    if (collection.countDocuments(Document("email", email)) == 0L) {
                        collection.insertOne(Document("email", email))
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

fun loginMlab(driver: DriverManager, collection: MongoCollection<Document>) {
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
                                    Document("email", email)
                                            .append("password", password).append("firstName", firstName).append("lastName", lastName)
                            )
                        },
                        WelcomePage(),
                        BuildClusterPage(),
                        CreateClusterTypePage(),
                        ClusterCreatingPage(),
                        CreatingDatabaseUserPage(),
                        AddNewUserPage(db_username, db_password),
                        CreatingDatabaseUserDonePage(),
                        NetworkAccessPage(),
                        NetworkAccessAddWhitelistPage(),
                        NetworkAccessAddWhitelistDonePage()
                )
        )
        run { pageResponse ->
            println(pageResponse)
        }
    }
}

fun robotLoginMlab(email: String, password: String, collection: MongoCollection<Document>) {
    println("robot login mlab $email $password")
    RobotManager(browerType = RobotManager.BrowserType.CHROME_INCOGNITO).apply {
        openBrowser()
        browserGoTo("https://cloud.mongodb.com/user#/atlas/login")

        clearAndPasteInput(email)
        enter()
        sleep()
        enter()
        longSleep()

        clearAndPasteInput(password)
        enter()


        longSleep()
        for (i in 0..10) {
            val text = getScreenText()
            println(text)
            if (isCaptchaOpen(text)) {
                doing(this, collection, email)
                return@robotLoginMlab
            } else if (isBashboardCreateClusterOpen(text)) {
                doingAfterBypassCaptcha(this, collection, email)
                return@robotLoginMlab
            }
        }

        if (getCurrentUrl().endsWith("#clusters")) alreadyCreatedCluster(this, collection, email)
        else {
            val a = 1//debug
        }
    }
}

fun doing(robotManager: RobotManager, collection: MongoCollection<Document>, email: String) {
    val initialResolveCaptchaBtn = Pair<Int, Int>(599, 694)
//    val initialResolveCaptchaBtn = Pair(885, 915)
    bypassCaptcha(initialResolveCaptchaBtn, initialResolveCaptchaBtn, initialResolveCaptchaBtn, robotManager, onSuccess = {
        if (isBashboardCreateClusterOpen(robotManager.getScreenText())) {
            doingAfterBypassCaptcha(robotManager, collection, email)
        } else if (robotManager.getCurrentUrl().endsWith("#clusters")) alreadyCreatedCluster(robotManager, collection, email)
        else {
            val a = 1//debug
        }
    }, onFail = {
        println("doing bypassCaptcha Fail")
    }, onSpecialCase = {
        if (isBashboardCreateClusterOpen(robotManager.getScreenText())) {
            doingAfterBypassCaptcha(robotManager, collection, email)
        } else if (robotManager.getCurrentUrl().endsWith("#clusters")) alreadyCreatedCluster(robotManager, collection, email)
        else {
            val a = 1//debug
        }
    }, isDone = { screenText ->
        isBashboardCreateClusterOpen(screenText)
    })
}

fun doingAfterBypassCaptcha(robotManager: RobotManager, collection: MongoCollection<Document>, email: String) {
    with(robotManager) {
        println(1)
        sleep()
        val safePointPosition = Pair<Int, Int>(12, 394)
//        val safePointPosition = Pair<Int, Int>(screenSize.width / 2, 130)
//        val buildAClusterPosition = Pair(1050, 540)
        val buildAClusterPosition = Pair<Int, Int>(765, 538)
        click(buildAClusterPosition)
        println(2)
        waitUntilUrlEndWith("#clusters/pathSelector")
        click(safePointPosition)
        end()
        println(3)

//        val createACluserPosition = Pair(630, 880)
        val createACluserPosition = Pair<Int, Int>(353, 593)
        click(createACluserPosition)
        println(4)

        longSleep()
        click(safePointPosition)
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

        val finalcreateACluserPosition = Pair<Int, Int>(1021, 693)
//        val finalcreateACluserPosition = Pair(1300, 1000)
        click(finalcreateACluserPosition)
        println(6)

        longSleep()
        for (i in 0..10) {
            val text = getScreenText()
            println(text)
            if (isCaptchaOpen(text)) {
                val initialResolveCaptchaBtn = Pair<Int, Int>(601, 694)
//                val initialResolveCaptchaBtn = Pair(875, 1000)
//                val multipleCorrect = Pair<Int, Int>(600, 695)
                val multipleCorrect = Pair<Int, Int>(660, 695)
                bypassCaptcha(initialResolveCaptchaBtn, multipleCorrect, initialResolveCaptchaBtn, this, onSuccess = {
                    println(5.5)
                    click(safePointPosition)
                    val text = printScreenText()
                    if (isClusterBeingCreated(text)) {
                        dooone(this, collection, email)
                    }
                }, onFail = {
                    closeWindow()
                }, onSpecialCase = {
                    println(6)
                    click(safePointPosition)
                    val text = printScreenText()
                    if (isClusterBeingCreated(text)) {
                        dooone(this, collection, email)
                    }
                }, isDone = { screenText ->
                    isClusterBeingCreated(screenText)
                })
                return@doingAfterBypassCaptcha
            } else if (isClusterBeingCreated(text)) {
                dooone(this, collection, email)
                return@doingAfterBypassCaptcha
            }
        }
    }
}

fun dooone(robotManager: RobotManager, collection: MongoCollection<Document>, email: String) {
    println("Your cluster is being created")
    val cookieStr = robotManager.getCookieStr()

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
    robotManager.closeWindow()
}

fun alreadyCreatedCluster(robotManager: RobotManager, collection: MongoCollection<Document>, email: String) {
    val cookieStr = robotManager.getCookieStr()
    if (cookieStr != "") {
        collection.updateOne(Document("email", email), Updates.combine(
                Updates.set("cookies", cookieStr)
        ))
    }
    robotManager.closeWindow()
}

fun RobotManager.getCookieStr(): String {
    println("getCookieStr")

    robot.keyPress(KeyEvent.VK_F12)
    robot.keyRelease(KeyEvent.VK_F12)
    sleep()

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
    sleep()

    return SystemClipboard.get()
}


fun isBashboardCreateClusterOpen(text: String) = text.contains("Clusters\n" +
        "Find a cluster...\n" +
        "Create a cluster\n" +
        "Create a cluster\n" +
        "Choose your cloud provider, region, and specs.\n" +
        "Build a Cluster")

fun isClusterBeingCreated(text: String) = text.contains("Your cluster is being created") && text.contains("New clusters take between 7-10 minutes to provision.")