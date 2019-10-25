package net.scr0pt.utils.webdriver

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.awt.Toolkit
import java.awt.Toolkit.getDefaultToolkit




object Browser {
    val htmlUnitDriver: DriverManager
        get() {
            return DriverManager(object : HtmlUnitDriver(BrowserVersion.FIREFOX_60, true) {
                override fun modifyWebClient(client: WebClient): WebClient {
                    val webClient = super.modifyWebClient(client)
                    // you might customize the client here
                    webClient.options.isCssEnabled = false
                    return webClient
                }
            })
        }
    val firefox: DriverManager
        get() {
            if (GeckoUtils.getGeckoDriver()) {
                System.setProperty("webdriver.gecko.driver", GeckoUtils.GECKODRIVER_EXE_FILE);
            } else {
                println("Cant get getko driver")
            }

            val firefoxOptions = FirefoxOptions().apply {
                profile = FirefoxProfile().apply {
                    setPreference("browser.helperApps.neverAsk.saveToDisk", "application/excel")
                    setAcceptUntrustedCertificates(true)
                    setAssumeUntrustedCertificateIssuer(false)
                }
//                val screenSize = Toolkit.getDefaultToolkit().getScreenSize()
//                addArguments("--width=${screenSize.width}","--height=${screenSize.height/2}")
            }

            return DriverManager(FirefoxDriver(firefoxOptions))
        }

    val chrome: DriverManager
        get() {
            //file config phai dat o desktop
            val options = ChromeOptions()
            options.addArguments("--start-maximized")
            options.addArguments("--ignore-certificate-errors")
            options.addArguments("--disable-popup-blocking")
            options.addArguments("disable-infobars") //disable chrome is being controlled by automated test software
//        options.addArguments("user-data-dir=" + Config.getInstance().get("chrome_profile"))
            if (ChromeDriverUtils.getChromeDriver()) {
                System.setProperty("webdriver.chrome.driver",
                        ChromeDriverUtils.CHROMEDRIVER_EXE_FILE
                )
            }
            return DriverManager(ChromeDriver(options))
        }


}
