package net.scr0pt.utils.webdriver

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class DriverManager(var driver: WebDriver) {
    companion object {
        @JvmStatic
        var INTERVAL_SLEEP_TIME = 1000L//1 second
        var MAX_SLEEP_TIME = 180000L//3 min
    }

    fun get(url: String) = driver.get(url)
    fun close() = driver.close()
    fun renew(newDriver: Any) {
        this.driver.close()
        if (newDriver is WebDriver) {
            this.driver = newDriver
        } else if (newDriver is DriverManager) {
            this.driver = newDriver.driver
        }
    }


    val url
        get() = driver.currentUrl.removeSuffix("/")
    val html: String
        get() = driver.pageSource
    val doc: Document?
        get() = try {
            Jsoup.parse(html)
        } catch (e: Exception) {
            null
        }
    val title: String?
        get() = driver.title
    val cookieStr: String
        get() {
            var cookies = ""
            driver.manage().cookies.forEach {
                cookies += "${it.name}=${it.value};"
            }
            cookies.removeSuffix(";")
            return cookies
        }

    fun wait(isDone: () -> Boolean, onWait: (() -> Unit)? = null) {
        var waitTime = 0.0
        var sleepTime: Double = INTERVAL_SLEEP_TIME.toDouble()
        while (waitTime < MAX_SLEEP_TIME) {
            if (isDone()) return
            onWait?.let { it() }
            sleepTime *= 2
            waitTime += sleepTime
            Thread.sleep(sleepTime.toLong())
        }
    }

    fun waitUntilUrlChange() {
        val url = this.url
        wait(isDone = { url != this.url })
    }

    fun waitUntilUrlChange(url: String) {
        wait(isDone = { url.removeSuffix("/") == this.url })
    }

    fun waitUntilDismiss(selector: Any) {
        wait(isDone = { findFirstElOneTime(selector) == null })
    }


    fun findFirstElOneTime(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return this.findElsOneTime(selector, filter = filter, contains = contains, equals = equals).firstOrNull()
    }

    fun findElsOneTime(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): ArrayList<WebElement> {
        val arr = arrayListOf<WebElement>()
        try {
            parseSelector(selector)?.let {
                this.driver.findElements(it)?.forEach {
                    if (it != null && filter(it) && (contains == null || (it.text.contains(contains, ignoreCase = true))) && (equals == null || (equals.equals(it.text, ignoreCase = true)))) {
                        arr.add(it)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return arr
    }

    fun parseSelector(selector: Any): By? {
        return when (selector) {
            is String -> if (selector.isEmpty()) null else By.cssSelector(selector)
            is By -> selector
            else -> null
        }
    }


    fun findFirstEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains,equals = equals).firstOrNull()
    }


    fun sendKeysFirstEl(txt: String, selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains,equals = equals).firstOrNull()?.apply {
            clear()
            sendKeys(txt)
        }
    }

    fun sendKeysLastEl(txt: String, selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals).lastOrNull()?.apply {
            clear()
            sendKeys(txt)
        }
    }


    fun clickFirstEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals).firstOrNull()?.apply {
            click()
        }
    }

    fun clickLastEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals).lastOrNull()?.apply {
            click()
        }
    }

    fun findEls(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null): ArrayList<WebElement> {
        wait(isDone = { findElsOneTime(selector, filter = filter, contains = contains).isNotEmpty() })
        return findElsOneTime(selector, filter = filter, contains =  contains)
    }


    fun executeAsyncScript(js: String): Any {
        return (driver as JavascriptExecutor).executeAsyncScript(js)
    }

    fun executeScript(js: String): Any {
        return (driver as JavascriptExecutor).executeScript(js)
    }

    fun refresh() {
        this.driver.navigate().refresh()
    }


}