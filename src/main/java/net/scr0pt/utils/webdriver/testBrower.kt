package net.scr0pt.utils.webdriver

import java.io.*
import org.openqa.selenium.*
import org.openqa.selenium.chrome.*
import org.openqa.selenium.remote.*
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global.quit
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.WebDriver
import java.net.URL


class GettingStartedWithService {
    private var driver: WebDriver? = null

    fun createDriver() {
        driver = RemoteWebDriver(service!!.url, ChromeOptions())
    }

    fun quitDriver() {
        driver!!.quit()
    }

    fun testGoogleSearch() {
        driver!!.get("http://www.google.com")
        // rest of the test...
    }

    companion object {

        private var service: ChromeDriverService? = null

        @Throws(IOException::class)
        fun createAndStartService() {
            service = ChromeDriverService.Builder()
                    .usingDriverExecutable(File("/path/to/chromedriver"))
                    .usingAnyFreePort()
                    .build()
            service!!.start()
        }

        fun stopService() {
            service!!.stop()
        }
    }
}

fun main() {

}

fun main3() {
    val driver =  DriverManager(driverType = DriverManager.BrowserType.chrome)
    driver.get("https://openload.co/register")
    Thread.sleep(5000)
    val iframe = driver.findFirstEl("iframe", filter = {it.getAttribute("src")?.startsWith("https://www.google.com/recaptcha/api2/anchor") == true})
    driver.driver.switchTo().frame(iframe)
    print(driver.title)
}


fun main2() {
    val firefox = DriverManager(driverType = DriverManager.BrowserType.chrome)
    firefox.get("http://google.com")
    print(firefox.title)
}