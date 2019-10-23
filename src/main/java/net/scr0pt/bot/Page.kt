package net.scr0pt.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver
import net.scr0pt.utils.webdriver.document


class PageManager(val pageList: ArrayList<Page>, val driver: WebDriver, val originUrl: String) {
    var prevPage: Page? = null
    var currentPage: Page? = null
    var hhhhhhhh = 0
    val INTERVAL_SLEEP_TIME =1000L//1 second
    val MAX_SLEEP_TIME = 180000L//3 min
    var isSuccess = false

    var generalWatingResult: ((doc: Document, currentUrl: String) -> PageResponse)? = null

    suspend fun run(onRunFinish: suspend (pageResponse: PageResponse?) -> Unit) {
        driver.get(originUrl)
        val job = GlobalScope.launch {
            var pageResponse: PageResponse? = null
            loop@ while (currentPage?.isEndPage() != true) {
                var waitTime = 0L
                while (waitTime < MAX_SLEEP_TIME) {

                    pageResponse = if(isSuccess){
                        PageResponse.OK("Force success")
                    } else {
                        waiting()
                    }

                    if (pageResponse is PageResponse.WAITING_FOR_RESULT) {
                        waitTime += INTERVAL_SLEEP_TIME
                        delay(INTERVAL_SLEEP_TIME)
                    } else {
                        break@loop
                    }
                }
            }

            println("onRunFinish running")
            onRunFinish(pageResponse)
        }
        job.join()
    }


    fun waiting(): PageResponse {
        println("waiting ${hhhhhhhh++}")
        val doc = driver.document ?: return PageResponse.NOT_OK()
        val currentUrl = driver.currentUrl ?: return PageResponse.NOT_OK()
        val title = driver.title ?: return PageResponse.NOT_OK()

        //is go to next page
        pageList.forEach { page ->
            if (page != currentPage && page.detect(doc, currentUrl,title)) {
                prevPage = currentPage
                prevPage?.onPageFinish?.let { onPageFinish -> onPageFinish() }
                currentPage = page

                val response = page.action(driver)
                if (response !is PageResponse.WAITING_FOR_RESULT) {
                    return response
                }

                if (page.isEndPage()) return PageResponse.OK()
            }
        }

        generalWatingResult?.let { generalWatingResult ->
            val response = generalWatingResult(doc, currentUrl)
            if (response !is PageResponse.WAITING_FOR_RESULT) {
                return response
            }
        }

        currentPage?.watingResult(doc, currentUrl, title)?.let {
            if (it !is PageResponse.WAITING_FOR_RESULT) {
                return it
            }
        }
        return PageResponse.WAITING_FOR_RESULT()
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
    //check if driver is in this page
    abstract fun _detect(doc: Document, currentUrl: String, title: String): Boolean

    fun detect(doc: Document?, currentUrl: String, title: String): Boolean {
        doc ?: return false
        val result = _detect(doc, currentUrl, title)
        if (result)
            onPageDetect()
        return result
    }

    open fun watingResult(doc: Document, currentUrl: String, title: String): PageResponse? = null
    abstract fun isEndPage(): Boolean

    abstract fun _action(driver: WebDriver): PageResponse

    fun action(driver: WebDriver): PageResponse {
        if(isDone) return PageResponse.WAITING_FOR_RESULT(this::class.java.simpleName + " done")

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


