package net.scr0pt.utils.webdriver

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile


class DriverManager(
        val driverType: BrowserType,
        val driverHeadless: Boolean = false,
        var driver: WebDriver = driverType.get(driverHeadless)
) {
    companion object {
        @JvmStatic
        var INTERVAL_SLEEP_TIME = 1000L//1 second
        var MAX_SLEEP_TIME = 180000L//3 min
    }

    enum class BrowserType {
        HtmlUnitDriver {
            override fun get(headless: Boolean): WebDriver {
                return object : org.openqa.selenium.htmlunit.HtmlUnitDriver(BrowserVersion.FIREFOX_60, true) {
                    override fun modifyWebClient(client: WebClient): WebClient {
                        val webClient = super.modifyWebClient(client)
                        webClient.options.isCssEnabled = false
                        return webClient
                    }
                }
            }
        },
        Firefox {
            override fun get(headless: Boolean): WebDriver {
                if (GeckoUtils.getGeckoDriver()) {
                    System.setProperty("webdriver.gecko.driver", GeckoUtils.GECKODRIVER_EXE_FILE);
                } else {
                    println("Cant get getko driver")
                }

                val firefoxOptions = FirefoxOptions().apply {
                    profile = FirefoxProfile().apply {
                        setPreference("browser.helperApps.neverAsk.saveToDisk", "application/excel")
                        if (headless) {
                            addArguments("--headless")
                        }
                        setAcceptUntrustedCertificates(true)
                        setAssumeUntrustedCertificateIssuer(false)
                    }
                }

                return FirefoxDriver(firefoxOptions)
            }
        },
        Chrome {
            override fun get(headless: Boolean): WebDriver {
                val options = ChromeOptions()
                options.addArguments("--start-maximized", "--incognito", "--ignore-certificate-errors", "--disable-popup-blocking")
                options.addArguments("disable-infobars") //disable chrome is being controlled by automated test software

                if (headless) {
                    options.addArguments("--no-sandbox")
                    options.addArguments("--headless")
                    options.addArguments("disable-gpu")
                }

                if (ChromeDriverUtils.getChromeDriver()) {
                    System.setProperty("webdriver.chrome.driver",
                            ChromeDriverUtils.CHROMEDRIVER_EXE_FILE
                    )
                }
                return ChromeDriver(options)
            }
        };

        abstract fun get(headless: Boolean = false): WebDriver
    }

    fun get(url: String) {
        for(i in 0..10){
            try {
                driver.get(url)
                break
            } catch (e: Exception) {
                Thread.sleep(1000)
            }
        }
    }


    fun close() {
        try {
            driver.close()
        } catch (e: Exception) {
        }
    }


    fun renew(newDriver: Any? = null) {
        this.close()
        if (newDriver == null) {
            this.driver = this.driverType.get(this.driverHeadless)
        } else if (newDriver is WebDriver) {
            this.driver = newDriver
        } else if (newDriver is DriverManager) {
            this.driver = newDriver.driver
        }
    }


    val url
        get() = driver.currentUrl.removeSuffix("/")
    val html: String
        get() {
            for(i in 0..10){
                try {
                    return driver.pageSource
                } catch (e: Exception) {
                    Thread.sleep(1000)
                }
            }
            return ""
        }
    val doc: Document?
        get() = try {
            Jsoup.parse(html)
        } catch (e: Exception) {
            null
        }
    val title: String
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

    fun addCookies(cookieStr: String) {
        cookieStr.split(";").forEach {
            driver.manage().addCookie(Cookie(it.substringBefore("="), it.substringAfter("=")))
        }
    }

    fun wait(isDone: () -> Boolean, onWait: (() -> Unit)? = null, onFail: (() -> Unit)? = null) {
        var waitTime = 0.0
        var sleepTime: Double = INTERVAL_SLEEP_TIME.toDouble()
        while (waitTime < MAX_SLEEP_TIME) {
            if (isDone()) return
            onWait?.let { it() }
            sleepTime *= 2
            waitTime += sleepTime
            Thread.sleep(sleepTime.toLong())
        }
        onFail?.invoke()
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


    fun findFirstElOneTime(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return this.findElsOneTime(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).firstOrNull()
    }

    fun findElsOneTime(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): ArrayList<WebElement> {
        val list = arrayListOf<WebElement>()
        try {
            parseSelector(selector)?.let { parserd ->
                this.driver.findElements(parserd)?.forEach { element ->
                    val text = try {
                        element.text
                    } catch (e: Exception) {
                        return@forEach
                    }
                    if (element != null && filter(element)
                            && (contains == null || (text.contains(contains, ignoreCase = true)))
                            && (startWithsOneOf == null || (startWithsOneOf.firstOrNull { text.startsWith(it, ignoreCase = true) } != null))
                            && (equals == null || (equals.equals(text, ignoreCase = true)))) {
                        list.add(element)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun parseSelector(selector: Any): By? {
        return when (selector) {
            is String -> if (selector.isEmpty()) null else By.cssSelector(selector)
            is By -> selector
            else -> null
        }
    }


    fun findFirstEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).firstOrNull()
    }


    fun sendKeysFirstEl(txt: String, selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).firstOrNull()?.apply {
            clear()
            sendKeys(txt)
        }
    }

    fun sendKeysLastEl(txt: String, selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).lastOrNull()?.apply {
            clear()
            sendKeys(txt)
        }
    }


    fun clickFirstEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).firstOrNull()?.apply {
            click()
        }
    }

    fun clickLastEl(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): WebElement? {
        return findEls(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf).lastOrNull()?.apply {
            click()
        }
    }

    fun findEls(selector: Any, filter: (WebElement) -> Boolean = { _ -> true }, contains: String? = null, equals: String? = null, startWithsOneOf: List<String>? = null): ArrayList<WebElement> {
        wait(isDone = { findElsOneTime(selector, filter = filter, contains = contains, equals = equals).isNotEmpty() })
        return findElsOneTime(selector, filter = filter, contains = contains, equals = equals, startWithsOneOf = startWithsOneOf)
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


class DriverElements {
    class Form(
            private val inputs: ArrayList<Pair<String, String>> = arrayListOf(),
            private val buttons: List<String> = listOf<String>(),
            private val submitBtn: String
    ) {
        val selectors: ArrayList<String> = arrayListOf()

        init {
            inputs.forEach {
                selectors.add(it.first)
            }
            buttons.forEach {
                selectors.add(it)
            }
            selectors.add(submitBtn)
        }

        fun submit(driver: DriverManager) {
            inputs.forEach {
                driver.sendKeysFirstEl(it.second, it.first)
            }
            buttons.forEach {
                driver.clickFirstEl(it)
            }
            driver.clickFirstEl(submitBtn)
        }
    }
}