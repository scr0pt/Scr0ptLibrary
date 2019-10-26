package net.scr0pt.thirdservice.mega

import net.scr0pt.bot.MegaPageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document
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
                    Browser.firefox,
                    collection
            )
    }
}


 fun loginMega(
        email: String,
        password: String,
        driver: DriverManager,
        collection: MongoCollection<org.bson.Document>
) {
    PageManager(driver, "https://mega.nz/login").apply {
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
            if (pageResponse is PageResponse.OK) {
                collection.updateOne(org.bson.Document("User_Name", email), Updates.set("last_time_login", Date()))
                if (pageResponse.msg?.startsWith("Storage") == true) {
                    val storage = pageResponse.msg.removePrefix("Storage").trim()
                    println("storage: $storage")
                    collection.updateOne(
                            org.bson.Document("User_Name", email),
                            Updates.set("Storage", storage)
                    )
                }
            } else if (pageResponse is MegaPageResponse.NOT_VERIFY_EMAIL_YET) {
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
    override fun isEndPage() = false

    override fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? {
        val notification = doc.selectFirst(".fm-notification-body .fm-notification-info h1")?.text()
        if (notification == "Invalid email and/or password. Please try again.") return PageResponse.INCORECT_PASSWORD(
                msg = notification
        )
        else if (notification == "This account has not completed the registration process yet. First check your email, click on the Activate Account button and reconfirm your chosen password.") return MegaPageResponse.NOT_VERIFY_EMAIL_YET(
                msg = notification
        )
        return null
    }

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.sendKeysFirstEl(email, "input#login-name2") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.sendKeysFirstEl(password, "input#login-password2") ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.clickFirstEl(".big-red-button.login-button") ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/login")
}

class CloudDrivePage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.get("https://mega.nz/fm/account")
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/fm") &&
                    doc.selectFirst(".nw-fm-tree-header.cloud-drive input[placeholder=\"Cloud Drive\"]") != null

}

class AccountPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    val quotaSelector = ".dashboard-container .fm-account-blocks.storage .account.chart-block .account.chart.data"
    override fun isEndPage() = true

    override fun _action(driver: DriverManager): PageResponse {
        println(this::class.java.simpleName + ": action")
        val quota =
                driver.doc?.selectFirst(quotaSelector)
                        ?.text()

        if (quota != null) {
            return PageResponse.OK(msg = quota)
        } else return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            currentUrl.startsWith("https://mega.nz/fm/account") &&
                    doc.selectFirst(".settings-banner .first-block .title-txt")?.text() == "Overall Usage:" &&
                    (doc.selectFirst(quotaSelector)?.text()?.isNotEmpty() == true) &&
                    (doc.selectFirst(quotaSelector)?.text() != "Storage 2.93 GB / 15 GB") &&
                    doc.selectFirst(".fm-account-profile.fm-account-sections .cancel-account-block .settings-left-block") != null &&
                    doc.selectFirst(".fm-account-profile.fm-account-sections .cancel-account-block .cancel-account")?.text() == "Cancel Account"

}