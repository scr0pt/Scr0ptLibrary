package net.scr0pt.selenium

import kotlinx.coroutines.*
import net.scr0pt.bot.PageResponse
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document
import java.time.LocalDateTime


fun main() {
    val driverManager = DriverManager(driverType = DriverManager.BrowserType.firefox)
    PageManager(driverManager, "https://www.google.com").apply {
        addPageList(arrayListOf(
                GoogleSearch().apply {
                    actionTime = 5
                }, GoogleSearchResult()
        ))
        run()
    }
}

class GoogleSearchResult(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url?.startsWith("https://www.google.com/search") == true &&
                pageStatus.title?.endsWith("- Tìm với Google") == true
    }
}

class GoogleSearch(onPageFinish: (() -> Unit)? = null) : Page(onPageFinish) {
    override fun detect(pageStatus: PageStatus): Boolean {
        return pageStatus.url?.startsWith("https://www.google.com") == true &&
                pageStatus.title == "Google"
    }

    override fun action(pageStatus: PageStatus): Response {
        pageStatus.driver.sendKeysFirstEl("Hello World", "input[name=\"q\"]")?.submit()
                ?: return Response.NOT_FOUND_ELEMENT()
        return Response.WAITING()
    }
}


class PageManager(val driver: DriverManager, val originUrl: String? = null) {
    val startTime = System.currentTimeMillis()
    var prevPage: Page? = null
    var currentPage: Page? = null
    val INTERVAL_SLEEP_TIME = 1000L//1 second
    val MAX_SLEEP_TIME = 120000L//2 min
    val pageList = arrayListOf<Page>()
    var sleepCounting = 0
    var generalWatingResult: ((pageStatus: PageStatus) -> Response)? = null
    var onFinish: ((response: Response) -> Unit)? = null
    var isSuccess: Boolean = false
    var pageResponse: Response = Response.INITIAL()
    var isFinish = false

    fun addPage(page: Page) {
        this.pageList.add(page)
    }

    fun addPageList(pageList: ArrayList<Page>) {
        this.pageList.addAll(pageList)
    }


    fun run() {
        if (this.pageList.isEmpty()) {
            println("Page list is empty")
            return
        }
        println("Running")


        runBlocking {
            originUrl?.let { driver.get(it) }

            loop@ while (currentPage?.isEndPage() != true) {
                var waitTime = 0.0
                while (waitTime < MAX_SLEEP_TIME) {
                    pageResponse = if (isSuccess) {
                        Response.OK("Force success")
                    } else {
                        onWaiting()
                    }

                    if (pageResponse is Response.WAITING) {
                        waitTime += INTERVAL_SLEEP_TIME
                        delay(INTERVAL_SLEEP_TIME)
                    } else {
                        break@loop
                    }
                }
            }

            println("onRunFinish running with pageResponse $pageResponse ${(pageResponse.msg) ?: ""}")
            onFinish?.invoke(pageResponse)
            isFinish = true
        }
    }

    val nonePageDetectCountDown = MyCountDown()
    private fun onWaiting(): Response {
        val now = LocalDateTime.now()
        println("waiting ${sleepCounting++} ${now.hour}:${now.minute}:${now.second}")
        val pageStatus = PageStatus(driver)

        //is go to next page
        val listOfpageDetect = pageList.filter { it.parentDetect(pageStatus) }
        if (listOfpageDetect.size != 1) {
            println("listOfpageDetect: ${listOfpageDetect.size}")
        }

        if (listOfpageDetect.isEmpty()) {
            if(nonePageDetectCountDown.isTimeout()) return Response.TIME_OUT()
        } else {
            listOfpageDetect.firstOrNull()?.let { page ->
                nonePageDetectCountDown.reset()
                if (page != currentPage) {
                    prevPage = currentPage
                    prevPage?.onPageFinish?.let { onPageFinish -> onPageFinish() }
                    currentPage = page
                }

                page.onParentWaiting(pageStatus)?.let {
                    if (it !is Response.WAITING) {
                        return@onWaiting it
                    }
                }

                val response = page.parentAction(pageStatus)
                if (response !is Response.WAITING) {
                    return@onWaiting response
                }

                if (page.isEndPage()) return@onWaiting Response.OK()
            }
        }



        generalWatingResult?.let { generalWatingResult ->
            val response = generalWatingResult(pageStatus)
            if (response !is Response.WAITING) {
                return@onWaiting response
            }
        }


        return Response.WAITING()
    }
}

class PageStatus(val driver: DriverManager) {
    val doc: Document? = driver.doc
    val title: String? = driver.title
    val url: String? = driver.url
}

abstract class Page(val onPageFinish: (() -> Unit)? = null) {
    val TAG = this::class.java.simpleName
    fun log(msg: String) {
        val now = LocalDateTime.now()
        println("$TAG [${now.hour}:${now.minute}:${now.second}]: $msg")
    }

    var detectTime: Long? = null
    var actionTime: Int = 1
    fun getPageAction() = PageAction.DEFAULT()

    val detectWatingCountDown = MyCountDown()
    fun onParentWaiting(pageStatus: PageStatus): Response? {
        if (detectWatingCountDown.isTimeout()) {
            return Response.TIME_OUT()
        } else {
            val res = onWaiting(pageStatus)
            log("Wating")
            return res
        }
    }

    protected fun onWaiting(pageStatus: PageStatus): Response? = null
    fun isEndPage() = false
    protected abstract fun detect(pageStatus: PageStatus): Boolean
    fun parentDetect(pageStatus: PageStatus): Boolean {
        val res = detect(pageStatus)
        if (res) {
            log("detect")
            if (detectTime == null) {
                detectTime = System.currentTimeMillis()
            }
        }
        return res
    }

    fun parentAction(pageStatus: PageStatus): Response {
        if (actionTime > 0) {
            val res = action(pageStatus)
            log("action ${actionTime--}")
            return res
        }
        return Response.WAITING()
    }

    protected open fun action(pageStatus: PageStatus): Response {
        return Response.WAITING()
    }
}


sealed class PageAction(msg: String? = null) {
    class DEFAULT(msg: String? = null) : PageAction(msg)
}

sealed class Response(val msg: String? = null) {
    class INITIAL(msg: String? = null) : Response(msg)//first state of response (mean no response installed)
    class OK(msg: String? = null) : Response(msg)
    class WAITING(msg: String? = null) : Response(msg)
    class TIME_OUT(msg: String? = null) : Response(msg)
    class NOT_FOUND_ELEMENT(msg: String? = null) : Response(msg)
}

class MyCountDown(val MaxTime: Long = 2 * 60 * 1000) {
    var watingTime = 0L
    var lastTimeWating= 0L

    fun isTimeout(): Boolean {
        if (lastTimeWating != 0L) {
            watingTime += (System.currentTimeMillis() - lastTimeWating)
        }
        lastTimeWating = System.currentTimeMillis()

        return watingTime > MaxTime
    }

    fun reset(){
        watingTime = 0L
        lastTimeWating= 0L
    }
}
