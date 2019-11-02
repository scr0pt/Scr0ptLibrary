package net.scr0pt.thirdservice.mega

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.selenium.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.DriverManager
import java.util.*

/**
 * Created by Long
 * Date: 10/15/2019
 * Time: 11:02 PM
 */

fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("mega")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("mega-account")

//    collection.find().first()?.let {
//        loginMega(it.getString("User_Name"), it.getString("Password"), Browser.firefox, collection)
//    }
//    collection.find()?.forEach {
    //    collection.find().first()?.let {
    collection.find(Filters.exists("Storage", false)).forEach {
        //    collection.find(Filters.exists("last_time_login", false)).forEach {
        if (!it.containsKey("verify_email"))
            loginMega(
                    it.getString("User_Name"),
                    it.getString("Password"),
                    collection
            )
    }
}


fun loginMega(
        email: String,
        password: String,
        collection: MongoCollection<org.bson.Document>
) {
    val driverManager = DriverManager(driverType = DriverManager.BrowserType.Firefox)
    PageManager(driverManager, "https://mega.nz/login").apply {
        addPageList(arrayListOf<Page>(
                MegaLoginPage(email, password) {
                    println("register success")
                },
                CloudDrivePage {
                    println("CloudDrivePage success")
                },
                AccountPage {
                    println("AccountPage success")
                }
        ))

        run { pageResponse ->
            println(pageResponse)
            if (pageResponse is Response.OK) {
                collection.updateOne(org.bson.Document("User_Name", email), Updates.set("last_time_login", Date()))
                if (pageResponse.msg?.startsWith("Storage") == true) {
                    val storage = pageResponse.msg.removePrefix("Storage").trim()
                    println("storage: $storage")
                    collection.updateOne(
                            org.bson.Document("User_Name", email),
                            Updates.set("Storage", storage)
                    )
                }
            } else if (pageResponse is MegaResponse.NOT_VERIFY_EMAIL_YET) {
                collection.updateOne(org.bson.Document("User_Name", email), Updates.set("verify_email", false))
            }

            Thread.sleep(20000)
            driver.close()
        }
    }
}

class MegaLoginPage(
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun onWaiting(pageStatus: PageStatus): Response? {
        val notification = pageStatus.doc?.selectFirst(".fm-notification-body .fm-notification-info h1")?.text()
        return if (notification == "Invalid email and/or password. Please try again.") MegaResponse.INCORECT_PASSWORD(msg = notification)
        else if (notification == "This account has not completed the registration process yet. First check your email, click on the Activate Account button and reconfirm your chosen password.") MegaResponse.NOT_VERIFY_EMAIL_YET(msg = notification)
        else null
    }


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl(email, "input#login-name2") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.sendKeysFirstEl(password, "input#login-password2") ?: return Response.NOT_FOUND_ELEMENT()
        pageStatus.driver.clickFirstEl(".big-red-button.login-button") ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://mega.nz/login")
}

class CloudDrivePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {


    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.get("https://mega.nz/fm/account")
        return Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://mega.nz/fm") &&
                    pageStatus.contain(".nw-fm-tree-header.cloud-drive input[placeholder=\"Cloud Drive\"]")

}

class AccountPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    private val quotaSelector = ".dashboard-container .fm-account-blocks.storage .account.chart-block .account.chart.data"
    override fun isEndPage() = true

    override fun action(pageStatus: PageStatus): Response {

        val quota = pageStatus.doc?.selectFirst(quotaSelector)?.text()
        return if (quota != null) {
            Response.OK(msg = quota)
        } else Response.WAITING()
    }

    override fun detect(pageStatus: PageStatus): Boolean =
            pageStatus.url.startsWith("https://mega.nz/fm/account") &&
                    pageStatus.equalsText(".settings-banner .first-block .title-txt", "Overall Usage:") &&
                    (pageStatus.doc?.selectFirst(quotaSelector)?.text()?.isNotEmpty() == true) &&
                    !pageStatus.equalsText(quotaSelector, "Storage 2.93 GB / 15 GB") &&
                    pageStatus.contain(".fm-account-profile.fm-account-sections .cancel-account-block .settings-left-block") &&
                    pageStatus.equalsText(".fm-account-profile.fm-account-sections .cancel-account-block .cancel-account", "Cancel Account")

}