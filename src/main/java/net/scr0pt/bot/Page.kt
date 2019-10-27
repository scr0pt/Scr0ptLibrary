package net.scr0pt.bot

import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document
import java.time.LocalDateTime


class PageManager(val driver: DriverManager, val originUrl: String) {
    val startTime = System.currentTimeMillis()
    var prevPage: Page? = null
    var currentPage: Page? = null


    var sleepCounting = 0
    val INTERVAL_SLEEP_TIME = 1000L//1 second
    val MAX_SLEEP_TIME = 120000L//2 min
    var sleepTime: Double = INTERVAL_SLEEP_TIME.toDouble()
    var linearSleep = true


    var isFinish = false
    var isSuccess = false
    var generalWatingResult: ((doc: Document, currentUrl: String) -> PageResponse)? = null
    private val pageList = arrayListOf<Page>()

    fun addPage(page: Page) {
        this.pageList.add(page)
    }

    fun addPageList(pageList: ArrayList<Page>) {
        this.pageList.addAll(pageList)
    }


    fun run(onRunFinish: (pageResponse: PageResponse?) -> Unit) {
        if (this.pageList.isEmpty()) {
            println("Page list is empty")
            return
        }
        println("Running")

        driver.get(originUrl)
        Thread(Runnable {
            var pageResponse: PageResponse? = null
            loop@ while (currentPage?.isEndPage() != true) {
                var waitTime = 0.0
                while (waitTime < MAX_SLEEP_TIME) {
                    pageResponse = if (isSuccess) {
                        PageResponse.OK("Force success")
                    } else {
                        waiting()
                    }

                    if (pageResponse is PageResponse.WAITING_FOR_RESULT) {
                        if (linearSleep) {
                            waitTime += INTERVAL_SLEEP_TIME
                            Thread.sleep(INTERVAL_SLEEP_TIME)
                        } else {
                            sleepTime *= 2
                            waitTime += sleepTime
                            Thread.sleep(sleepTime.toLong())
                        }
                    } else {
                        break@loop
                    }
                }
            }

            println("onRunFinish running with pageResponse $pageResponse ${(pageResponse?.msg) ?: ""}")
            isFinish = true
            onRunFinish(pageResponse)
        }).start()

        while (!isFinish) {
            Thread.sleep(INTERVAL_SLEEP_TIME)
        }
    }


    fun waiting(): PageResponse {
        val now = LocalDateTime.now()
        println("waiting ${sleepCounting++} ${now.hour}:${now.minute}:${now.second}")
        val doc = driver.doc ?: return PageResponse.NOT_OK()
        val currentUrl = driver.url ?: return PageResponse.NOT_OK()
        val title = driver.title ?: return PageResponse.NOT_OK()

        //is go to next page
        pageList.filter { it != currentPage }
                .forEach { page ->
                    if (page.detect(doc, currentUrl, title)) {
                        if (!linearSleep) {
                            sleepTime = INTERVAL_SLEEP_TIME.toDouble()
                        }
                        prevPage = currentPage
                        prevPage?.onPageFinish?.let { onPageFinish -> onPageFinish() }
                        currentPage = page

                        page.responseWhenDetect()?.let {
                            return@waiting it
                        }

                        val response = page.action(driver)
                        if (response !is PageResponse.WAITING_FOR_RESULT) {
                            return@waiting response
                        }

                        if (page.isEndPage()) return@waiting PageResponse.OK()
                    }
                }

        generalWatingResult?.let { generalWatingResult ->
            val response = generalWatingResult(doc, currentUrl)
            if (response !is PageResponse.WAITING_FOR_RESULT) {
                return@waiting response
            }
        }

        currentPage?.watingResult(doc, currentUrl, title)?.let {
            if (it !is PageResponse.WAITING_FOR_RESULT) {
                return@waiting it
            }
        }
        return@waiting PageResponse.WAITING_FOR_RESULT()
    }

    fun success() {
        isSuccess = true
    }
}

abstract class Page(val onPageFinish: (() -> Unit)? = null) {
    var isDone = false
    public var onPageDetect: (() -> Unit) = {
        println(this::class.java.simpleName + ": detect")
    }

    open fun responseWhenDetect(): PageResponse? = null

    //check if driver is in this page
    abstract fun _detect(doc: Document, currentUrl: String, title: String): Boolean

    fun detect(doc: Document?, currentUrl: String, title: String): Boolean {
        doc ?: return false
        val result = _detect(doc, currentUrl, title)
        if (result) {
            onPageDetect()
        }
        return result
    }

    open fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? = null
    abstract fun isEndPage(): Boolean

    abstract fun _action(driver: DriverManager): PageResponse

    fun action(driver: DriverManager): PageResponse {
        if (isDone) return PageResponse.WAITING_FOR_RESULT(this::class.java.simpleName + " done")

        val response = _action(driver)
        isDone = true
        if (response is PageResponse.WAITING_FOR_RESULT) {
            println(this::class.java.simpleName + ": WAITING_FOR_RESULT")
        }

        return response
    }
}

sealed class PageResponse(val msg: String? = null) {
    class NOT_OK(msg: String? = null) : PageResponse(msg)
    class OK(msg: String? = null) : PageResponse(msg)
    class NOT_FOUND_EMAIL(msg: String? = null) : PageResponse(msg)
    class INCORECT_PASSWORD(msg: String? = null) : PageResponse(msg)
    class PASSWORD_CHANGED(msg: String? = null) : PageResponse(msg)
    class RECAPTCHA(msg: String? = null) : PageResponse(msg)
    class NOT_FOUND_ELEMENT(msg: String? = null) : PageResponse(msg)
    class WAITING_FOR_RESULT(msg: String? = null) : PageResponse(msg)
    class INVALID_CURRENT_PAGE(msg: String? = null) : PageResponse(msg)
}

//GooglePageResponse
sealed class GooglePageResponse(msg: String? = null) : PageResponse(msg) {
    class CANT_LOGIN_FOR_YOU(msg: String? = null) : GooglePageResponse(msg)
    class VERIFY_PHONE_NUMBER_DETECT(msg: String? = null) : GooglePageResponse(msg)
}

//MlabPageResponse
sealed class MlabPageResponse(msg: String? = null) : PageResponse(msg) {
    class LOGIN_ERROR(msg: String? = null) : MlabPageResponse(msg)
}

//MegaPageResponse
sealed class MegaPageResponse(msg: String? = null) : PageResponse(msg) {
    class NOT_VERIFY_EMAIL_YET(msg: String? = null) : MegaPageResponse(msg)
    class CONFIRMATIOM_LINK_NO_LONGER_VALID(msg: String? = null) : MegaPageResponse(msg)
}

//HerokuPageResponse
sealed class HerokuPageResponse(msg: String? = null) : PageResponse(msg) {
    class COLLABORATOR_ADDED(msg: String? = null) : HerokuPageResponse(msg)
}


//FembedPageResponse
sealed class FembedPageResponse(msg: String? = null) : PageResponse(msg) {
    class Email_Registered(msg: String? = null) : FembedPageResponse(msg)
}


