package net.scr0pt.selenium

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.scr0pt.utils.webdriver.DriverManager
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


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


    fun run(onFinish: ((response: Response) -> Unit)? = null) {
        onFinish?.let {
            this.onFinish = onFinish
        }

        if (this.pageList.isEmpty()) {
            println("Page list is empty")
            return
        }
        println("Running")


        runBlocking {
            originUrl?.let { driver.get(it) }

            do {
                pageResponse = if (isSuccess) {
                    Response.OK("Force success")
                } else {
                    onWaiting()
                }

                delay(INTERVAL_SLEEP_TIME)
            } while (pageResponse is Response.WAITING)

            println("onRunFinish running with pageResponse $pageResponse ${(pageResponse.msg) ?: ""}")
            this@PageManager.onFinish?.invoke(pageResponse)
            isFinish = true
        }
    }

    val nonePageDetectCountDown = MyCountDown()
    private fun onWaiting(): Response {
        val now = LocalDateTime.now()
        println("waiting ${sleepCounting++} ${now.hour}:${now.minute}:${now.second}")
        var pageStatus = PageStatus(driver)

        //is go to next page
        val listOfpageDetect = pageList.filter { it.parentDetect(pageStatus) }
        if (listOfpageDetect.size != 1) {
            println("listOfpageDetect: ${listOfpageDetect.size} | title: ${pageStatus.title} | url: ${pageStatus.url}")
        }

        if (listOfpageDetect.isEmpty()) {
            if (nonePageDetectCountDown.isTimeout()) return Response.TIME_OUT()
        } else {
            listOfpageDetect.firstOrNull()?.let { page ->
                nonePageDetectCountDown.reset()
                if (page != currentPage) {
                    prevPage = currentPage
                    prevPage?.onPageFinish?.invoke()
                    currentPage = page
                }

                page.onParentWaiting(pageStatus)?.let {
                    if (it !is Response.WAITING) {
                        return@onWaiting it
                    }
                }

                driver.wait({
                    PageStatus(driver).run {
                        pageStatus = this
                        page.isReady(this)
                    }
                })

                if (page.isReady(pageStatus)) {
                    val response = page.parentAction(pageStatus)
                    if (response !is Response.WAITING) {
                        return@onWaiting response
                    }
                } else {
                    return@onWaiting Response.PAGE_CANNOT_READY()
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
        println("$TAG [${SimpleDateFormat("HH:mm:ss").format(Date())}]: $msg")
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
    open fun isEndPage() = false
    protected abstract fun detect(pageStatus: PageStatus): Boolean
    fun parentDetect(pageStatus: PageStatus): Boolean {
        val res = detect(pageStatus)
        if (res) {
            log("detect | title: ${pageStatus.title} | url: ${pageStatus.url}")
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

    open fun isReady(pageStatus: PageStatus): Boolean = true
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
    class PAGE_CANNOT_READY(msg: String? = null) : Response(msg)
}

class MyCountDown(val MaxTime: Long = 2 * 60 * 1000) {
    var watingTime = 0L
    var lastTimeWating = 0L

    fun isTimeout(): Boolean {//also counting
        if (lastTimeWating != 0L) {
            watingTime += (System.currentTimeMillis() - lastTimeWating)
        }
        lastTimeWating = System.currentTimeMillis()

        return watingTime > MaxTime
    }

    fun reset() {
        watingTime = 0L
        lastTimeWating = 0L
    }
}
