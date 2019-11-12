package net.scr0pt.robot

import com.mongodb.client.MongoCollection
import net.scr0pt.selenium.MyCountDown
import net.scr0pt.thirdservice.mlab.*
import net.scr0pt.utils.RobotManager
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.*


fun rPageProcessLogin(email: String, password: String, collection: MongoCollection<Document>) {
    val rPageManager = RPageManager("https://cloud.mongodb.com/user#/atlas/login")
    val captchaSelectAllImages = CaptchaSelectAllImages(hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(601, 695)
    ))
    val captchaMu = CaptchaMu(hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(661, 699)
    ))
    rPageManager.pages.addAll(arrayListOf(
            MlabLoginEnterEmailPass(email, password) {
                captchaSelectAllImages.resolveBtn = hashMapOf(
                        RobotManager.ScreenSize.HD to Pair<Int, Int>(601, 695)
                )
            }.apply {
                onPageDetect = {
                    rPageManager.safePointClick = false
                }
            },
            MladDashBoardClusterCreated(),
            captchaSelectAllImages,
            captchaMu,
            MladDashBoardClusterCreateYourCluster(),
            MladChooseAPath(),
            MladEnterClusterNamePage(),
            MladSelectClusterTier(),
            MladDashBoardClusterIsBeingCreated()
    ))
    rPageManager.run { rPageResponse ->
        when (rPageResponse) {
            is RPageResponse.OK -> {
                dooone(rPageManager.robot, collection, email, false)
            }
            else -> {
                println(rPageResponse)
            }
        }
        rPageManager.robot.closeWindow()
    }
}


class RPageManager(originUrl: String) {
    val robot = RobotManager(RobotManager.BrowserType.CHROME_INCOGNITO)
    var safePointClick = true
    var prevPage: RPage? = null
    var currentPage: RPage? = null
    val safePoint = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1908, 1025),
            RobotManager.ScreenSize.HD to Pair<Int, Int>(1336, 721)
    )
    val pages = arrayListOf<RPage>()

    init {
        robot.openBrowser()
        robot.browserGoTo(originUrl)
    }

    fun run(onFinish: ((RPageResponse?) -> Unit)? = null) {
        var response: RPageResponse? = null
        while (true) {
            val res = waiting()
            if (res !is RPageResponse.WAITING) {
                response = res
                break
            }
        }
        onFinish?.invoke(response)
    }

    val nonePageDetectCountDown = MyCountDown()
    fun waiting(): RPageResponse {
        robot.sleep()
        if (safePointClick) robot.click(safePoint)
        val screenText = robot.getScreenText()
        val listOfpageDetect = pages.filter { it.parentDetect(screenText) }
        val size = listOfpageDetect.size


        var pagesString = ""
        listOfpageDetect.forEach { pagesString += it.TAG + ", " }
        println("listOfpageDetect: $size | ${pagesString.removeSuffix(", ").trim()} ")
        if (listOfpageDetect.isEmpty()) {
            if (nonePageDetectCountDown.isTimeout()) return RPageResponse.TIME_OUT()
        } else {
            listOfpageDetect.firstOrNull()?.let { page ->
                nonePageDetectCountDown.reset()
                if (page != currentPage) {
                    prevPage = currentPage

                    prevPage?.let {
                        println(it.TAG + " onPageFinish")
                        it.onPageFinish?.invoke()
                    }

                    currentPage = page
                }

                page.onParentWaiting(screenText)?.let {
                    if (it !is RPageResponse.WAITING) {
                        return it
                    }
                }

                page.action(robot)

                robot.sleep()
            }
        }
        return RPageResponse.WAITING()
    }
}

sealed class RPageResponse(val msg: String? = null) {
    class INITIAL(msg: String? = null) : RPageResponse(msg)//first state of response (mean no response installed)
    class OK(msg: String? = null) : RPageResponse(msg)
    class WAITING(msg: String? = null) : RPageResponse(msg)
    class TIME_OUT(msg: String? = null) : RPageResponse(msg)
    class PAGE_CANNOT_READY(msg: String? = null) : RPageResponse(msg)
}

sealed class RPageMlabResponse(msg: String? = null) : RPageResponse(msg) {
    class INVALID_EMAIL_PASSWORD(msg: String? = null) : RPageMlabResponse(msg)
}

sealed class RPageCaptchaResponse(msg: String? = null) : RPageResponse(msg) {
    class CAPTCHA_FAIL(msg: String? = null) : RPageCaptchaResponse(msg)
}

abstract class RPage(val onPageFinish: (() -> Unit)? = null) {
    val TAG: String = this::class.java.simpleName
    fun log(msg: String) {
        println("$TAG [${SimpleDateFormat("HH:mm:ss").format(Date())}]: $msg")
    }

    var onPageDetectOnce: (() -> Unit)? = null
    var onPageDetect: (() -> Unit)? = null
    protected abstract fun detect(screenText: String): Boolean

    protected open fun onWaiting(screenText: String): RPageResponse? = null
    val detectWatingCountDown = MyCountDown()
    fun onParentWaiting(screenText: String): RPageResponse? {
        if (detectWatingCountDown.isTimeout()) {
            return RPageResponse.TIME_OUT()
        } else {
            val res = onWaiting(screenText)
            log("Wating")
            return res
        }
    }


    open fun action(robot: RobotManager) {}
    var detectTime: Long? = null
    fun parentDetect(screenText: String): Boolean {
        val res = detect(screenText)
        if (res) {
            log("detect")
            if (detectTime == null) {
                detectTime = System.currentTimeMillis()
            }

            onPageDetectOnce?.let {
                it()
                onPageDetectOnce = null
            }

            onPageDetect?.invoke()
        }
        return res
    }
}
