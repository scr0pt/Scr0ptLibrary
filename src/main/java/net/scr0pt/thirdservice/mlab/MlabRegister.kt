package net.scr0pt.thirdservice.mlab

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.bot.MlabPageResponse
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.bot.google.*
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.FakeProfile
import net.scr0pt.utils.InfinityMail
import net.scr0pt.utils.webdriver.Browser
import net.scr0pt.utils.webdriver.findElWait
import net.scr0pt.utils.webdriver.findFirstElWait
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver

/**
 * Created by Long
 * Date: 10/15/2019
 * Time: 11:07 AM
 */

suspend fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("mlab")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("mlab-account")
    val infinityMail = InfinityMail("tranvananh.200896@gmail.com")
    do {
        val email = infinityMail.getNext()?.fullAddress ?: break
        if (collection.countDocuments(
                        org.bson.Document(
                                "email",
                                email
                        )
                ) == 0L && email != "t.ranvananh.2.00.896@gmail.com"
        ) {
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

suspend fun loginGoogle(email: String, password: String, driver: WebDriver, onLoginSuccess: suspend () -> Unit, onLoginFail: (suspend (pageResponse: PageResponse?) -> Unit)? = null, recoverEmail: String? = null) {
    println("loginGoogle: $email $password")
    val googlePageManager = PageManager(
            arrayListOf<Page>(
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
                    GoogleSearch {
                        println("GoogleSearch success")
                    },
                    CantLoginForYou {
                        println("CantLoginForYou success")
                    }
            ),
            driver,
            "https://accounts.google.com/signin/v2/identifier?hl=vi&passive=true&continue=https%3A%2F%2Fwww.google.com%2F&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
    )

    recoverEmail?.let {
        googlePageManager.pageList.add(VerifyItsYouRecoverEmail(it) {
            println("VerifyItsYouRecoverEmail success")
        })
    }

    googlePageManager.generalWatingResult = { jsoupDoc, currentUrl ->
        if ((jsoupDoc.selectFirst("img#captchaimg")?.attr("src")?.length ?: 0) > 5) {
            PageResponse.RECAPTCHA()
        } else PageResponse.WAITING_FOR_RESULT()
    }

    googlePageManager.run { pageResponse ->
        if (pageResponse is PageResponse.OK) {
            onLoginSuccess()
        } else {
            driver.close()
            onLoginFail?.let { it(pageResponse) }
        }
    }
}


suspend fun registerMlab(
        email: String,
        firstName: String,
        lastName: String,
        driver: WebDriver,
        collection: MongoCollection<org.bson.Document>
) {
    val password = "XinChaoVietnam"

    val pageManager = PageManager(
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
            ),
            driver,
            "https://www.mongodb.com/atlas-signup-from-mlab?utm_source=mlab.com&utm_medium=referral&utm_campaign=mlab%20signup&utm_content=blue%20sign%20up%20button"
    )
    pageManager.run { pageResponse ->
        println(pageResponse)
        Thread.sleep(60000)
        driver.close()
        Thread.sleep(180000)
    }
}

suspend fun loginMlab(driver: WebDriver, collection: MongoCollection<org.bson.Document>) {
    val email = "v.a.n.a.n.ngu.y.en.0.8.3@gmail.com"
    val password = "XinChaoVietnam"
    val firstName = "Bruce"
    val lastName = "Lee"
    val db_username = "root"
    val db_password = "mongo"

    val pageManager = PageManager(
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
            ),
            driver,
            "https://www.mongodb.com/atlas-signup-from-mlab?utm_source=mlab.com&utm_medium=referral&utm_campaign=mlab%20signup&utm_content=blue%20sign%20up%20button"
    )
    pageManager.run { pageResponse ->
        when (pageResponse) {
            is MlabPageResponse.LOGIN_ERROR -> println(pageResponse.msg)
        }

        println(pageResponse)
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, "input#email")?.sendKeys(email)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input#first_name")?.sendKeys(firstName)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input#last_name")?.sendKeys(lastName)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input#password")?.sendKeys(password)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input#atlasCheckbox")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input#atlas-submit-btn")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            doc.selectFirst("h1.txt-center")?.text() == "Try MongoDB Atlas"
}

class BuildClusterPage(
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = true

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".path-selector-door-footer-starter .path-selector-door-submit", jsoup = false)?.click()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, "button[type=\"button\"]:containsOwn(Create Cluster)")?.click()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".left-nav a:containsOwn(Database Access)")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".section-controls-is-end-justified .button-is-primary")?.click()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, "input[name=\"user\"]")?.sendKeys(username)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "input[name=\"password\"]")?.sendKeys(password)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "button[type=\"submit\"]:containsOwn(Add User)")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".left-nav a:containsOwn(Network Access)")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".section-controls-is-end-justified .button-is-primary")?.click()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, "button[name=\"allowAccessAnywhere\"]")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findFirstElWait(100, 5000, "button[name=\"confirm\"]")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
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

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")
        driver.findFirstElWait(100, 5000, ".left-nav a:containsOwn(Clusters)")?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean {
        return currentUrl.startsWith("https://cloud.mongodb.com") && currentUrl.endsWith("network/whitelist") &&
                doc.selectFirst("td.plain-table-cell")?.text() == "0.0.0.0/0 (includes your current IP address)"
    }
}
