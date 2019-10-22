package net.scr0pt.thirdservice.heroku

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Updates
import net.scr0pt.bot.Page
import net.scr0pt.bot.PageManager
import net.scr0pt.bot.PageResponse
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.webdriver.*
import org.bson.types.ObjectId
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver


fun randomAppname(prefix: String? = null): String {
    var appName = (if (prefix == null) "bruce-" else prefix) + ObjectId().toString()
    while (appName.length > 30) appName = appName.substring(0, appName.length - 1)
    return appName
}

suspend fun main() {
    val collaboratorEmail = "alphahoai@gmail.com"
//    val collaboratorEmail = "brucealmighty5daeae612ce205583fda39d5@gmail.com"
    val mongoClient2 =
            MongoClients.create(MongoConnection.megaConnection)
    val herokuDatabase = mongoClient2.getDatabase("heroku")
    val herokuCollection = herokuDatabase.getCollection("heroku-account")

//    herokuCollection.random(org.bson.Document())?.let {
    herokuCollection.find()?.forEach {
        if (it.getList("collaborators", String::class.java)?.contains(collaboratorEmail) == true) return@forEach

        val email = it.getString("email")
        val password = it.getString("password")

        println("$email ~ $password")
        var appName = randomAppname()
        val driver = Browser.chrome

        PageManager(arrayListOf(
                HerokuLoginPage(email, password) {
                    println("HerokuLoginPage success")
                }, HerokuDashboardPage(action = HerokuDashboardPage.HerokuDashboardAction.CLICK_FIRST_APP) {
            println("HerokuDashboardPage click first app")
        }
        ), driver,
                "https://id.heroku.com/login")
                .run { pageresponse ->
                    println(pageresponse)
                    if (pageresponse is PageResponse.OK) {

                        driver.get(driver.currentUrlTrim + "/settings")
                        if (it.getString("appName").startsWith("bruce") == false) {
                            driver.findFirstElWait(1000, 60000, "#app-rename-input", jsoup = false)?.click()
                            driver.findFirstElWait(1000, 120000, "input.input-reset.lh-copy")?.let {
                                it.clear()
                                it.sendKeys(appName)
                            }
                            driver.findFirstElWait(1000, 120000, ".__hk-inline-edit-submit-button__.async-button")?.click()
                            driver.waitUntilUrlChange(1000, 120000)
                        }


                        driver.get(driver.currentUrlTrim!!.substringBeforeLast("/") + "/access")
                        val addCollaboratorBtns =
                                driver.findFirstElWait(1000, 60000, "button", jsoup = false, filter = { el -> el.text.trim().equals("Add collaborator", ignoreCase = true) })

                        addCollaboratorBtns?.click()
                        println("passwordConfirmInputs")
                        val passwordConfirmInputs = driver.findFirstElWait(1000, 60000, "input", jsoup = false, filter = { el -> el.getAttribute("placeholder") == "user@domain.com" })

                        passwordConfirmInputs?.sendKeys(collaboratorEmail)
                        println("saveChangesBtns")
                        val saveChangesBtns =
                                driver.findFirstElWait(1000, 60000, "button", jsoup = false, filter = { el -> el.text.trim().equals("Save changes", ignoreCase = true) })
                        saveChangesBtns?.click()

                        driver.findElWait(1000, 180000, ".collaborator-item", filter = { el -> el.text.contains(collaboratorEmail) })

                        val nameOfApp = driver.currentUrlTrim!!.substringBeforeLast("/access").substringAfterLast("/")
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.push("collaborators", collaboratorEmail)
                        )
                        herokuCollection.updateOne(
                                org.bson.Document("email", email),
                                Updates.set("appName", nameOfApp)
                        )
                        Thread.sleep(5000)
                        driver.close()
                    }
                }
    }

}


class HerokuLoginPage(
        val email: String,
        val password: String,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {
    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")

        driver.findElWait(1000, 120000, "input#email", jsoup = false).firstOrNull()?.sendKeys(email)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 120000, "input#password", jsoup = false).firstOrNull()?.sendKeys(password)
                ?: return PageResponse.NOT_FOUND_ELEMENT()
        driver.findElWait(1000, 120000, "button[value=\"Log In\"]", jsoup = false).firstOrNull()?.click()
                ?: return PageResponse.NOT_FOUND_ELEMENT()

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Heroku | Login" &&
                    currentUrl.startsWith("https://id.heroku.com/login")
}

class HerokuDashboardPage(
        val action: HerokuDashboardAction,
        onPageFinish: (() -> Unit)? = null
) : Page(onPageFinish = onPageFinish) {

    enum class HerokuDashboardAction {
        CREATE_NEW_APP, CLICK_FIRST_APP
    }

    override fun isEndPage() = false

    override fun _action(driver: WebDriver): PageResponse {
        println(this::class.java.simpleName + ": action")

        when (action) {
            HerokuDashboardAction.CREATE_NEW_APP -> driver.get("https://dashboard.heroku.com/new-app")
            HerokuDashboardAction.CLICK_FIRST_APP -> {
                driver.document?.selectFirst(".apps-list-item .items-baseline .ember-view span.near-black")?.let {
                    val appName = it.text()
                    println("appName: $appName")
                    driver.get("https://dashboard.heroku.com/apps/$appName")
                    return PageResponse.OK()
                }
            }
        }

        return PageResponse.WAITING_FOR_RESULT()
    }

    override fun _detect(doc: Document, currentUrl: String, title: String): Boolean =
            title == "Personal apps | Heroku" &&
                    currentUrl.startsWith("https://dashboard.heroku.com/apps")
}