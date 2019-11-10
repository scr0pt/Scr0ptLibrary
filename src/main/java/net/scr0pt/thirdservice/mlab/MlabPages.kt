package net.scr0pt.thirdservice.mlab

import net.scr0pt.selenium.MlabResponse
import net.scr0pt.selenium.Page
import net.scr0pt.selenium.PageStatus
import net.scr0pt.selenium.Response
import net.scr0pt.utils.webdriver.DriverElements

/**
 * Created by Long
 * Date: 11/10/2019
 * Time: 8:42 PM
 */


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
                pageStatus.equalsText(".empty-view-text-is-heading", "Create a database user") &&
                pageStatus.equalsText(".section-controls-is-end-justified .button-is-primary", "Add New User") &&
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
                pageStatus.equalsText(".section-controls-is-end-justified .button-is-primary", "Add New User") &&
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
                pageStatus.equalsText("h1.section-header-title", "Network Access") &&
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