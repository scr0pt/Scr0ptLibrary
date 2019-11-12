package net.scr0pt.robot

import com.mongodb.client.MongoCollection
import net.scr0pt.OSUtils
import net.scr0pt.thirdservice.mlab.dooone
import net.scr0pt.utils.RobotManager
import org.apache.commons.lang3.RandomUtils
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
                rPageManager.robot.closeWindow()
            }
            else -> {
                println(rPageResponse)
            }
        }
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

    var count = 0
    fun waiting(): RPageResponse {
        robot.sleep()
        if (safePointClick) robot.click(safePoint)
        val screenText = robot.getScreenText()
        val listOfpageDetect = pages.filter { it.parentDetect(screenText) }
        val size = listOfpageDetect.size


        var pagesString = ""
        listOfpageDetect.forEach { pagesString += it.TAG + ", " }
        println("listOfpageDetect: $size | ${pagesString.removeSuffix(", ").trim()} ")


        if (size == 0) {
            if (count++ > 10) {
                OSUtils.makeSound()
            }
            println(screenText + "\n\n")
            val a = 1
        } else {
            count = 0
        }


        if (listOfpageDetect.isEmpty()) {
        } else {
            listOfpageDetect.firstOrNull()?.let { page ->
                if (page != currentPage) {
                    prevPage = currentPage

                    prevPage?.let {
                        println(it.TAG + " onPageFinish")
                        it.onPageFinish?.invoke()
                    }

                    currentPage = page
                }

                page.onWaiting(screenText)?.let {
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

abstract class RPage(val onPageFinish: (() -> Unit)? = null) {
    val TAG: String = this::class.java.simpleName
    fun log(msg: String) {
        println("$TAG [${SimpleDateFormat("HH:mm:ss").format(Date())}]: $msg")
    }

    var onPageDetectOnce: (() -> Unit)? = null
    var onPageDetect: (() -> Unit)? = null
    protected abstract fun detect(screenText: String): Boolean
    open fun onWaiting(screenText: String): RPageResponse? = null
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

class MlabLoginEnterEmailPass(val email: String, val password: String, onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    val usernamePosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(964, 491),
            RobotManager.ScreenSize.HD to Pair<Int, Int>(683, 496)
    )

    override fun onWaiting(screenText: String): RPageResponse? {
        if (screenText.contains("Please provide a valid username and password.")) return RPageMlabResponse.INVALID_EMAIL_PASSWORD()
        return null
    }

    override fun detect(screenText: String): Boolean {
        return screenText.contains("We‘ve launched a unified login experience that gives you access to MongoDB Cloud, Support, and Jira with a single identity.")
                && screenText.contains("Register for a new account")
    }

    override fun action(robot: RobotManager) {
        robot.click(usernamePosition)
        robot.clearAndPasteInput(email)
        robot.enter()
        robot.sleep()
        robot.enter()
        robot.longSleep()
        robot.clearAndPasteInput(password)
        robot.enter()
        robot.sleep()
    }

}

class MladDashBoardClusterCreated(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    override fun onWaiting(screenText: String) = RPageResponse.OK()

    override fun detect(screenText: String): Boolean {
        return screenText.contains("CONNECT\nMETRICS\nCOLLECTIONS")
                && screenText.contains("CLUSTER TIER\nM0 Sandbox (General)")
                && screenText.contains("Enhance Your Experience\n" +
                "For dedicated throughput, richer metrics and enterprise security options, upgrade your cluster now!")
                && !screenText.contains("None Linked\nYour cluster is being created\n" +
                "New clusters take between 7-10 minutes to provision.")
                && !screenText.contains("Clusters\n" +
                "Find a cluster...\n" +
                "Create a cluster\n" +
                "Create a cluster\n" +
                "Choose your cloud provider, region, and specs.\n" +
                "Build a Cluster\n" +
                "Once your cluster is up and running, live migrate an existing MongoDB database into Atlas with our Live Migration Service.")
    }

}

class MladDashBoardClusterCreateYourCluster(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    val buildAClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1050, 541),
            RobotManager.ScreenSize.HD to Pair<Int, Int>(769, 535)
    )


    override fun detect(screenText: String): Boolean {
        return screenText.contains("Clusters\n" +
                "Find a cluster...\n" +
                "Create a cluster\n" +
                "Create a cluster\n" +
                "Choose your cloud provider, region, and specs.\n" +
                "Build a Cluster\n" +
                "Once your cluster is up and running, live migrate an existing MongoDB database into Atlas with our Live Migration Service.")
                && !screenText.contains("None Linked\n" +
                "Your cluster is being created\n" +
                "New clusters take between 7-10 minutes to provision.")
    }

    override fun action(robot: RobotManager) {
        robot.click(buildAClusterBtnPosition)
    }
}

class MladChooseAPath(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    val startingAtFreeCreateClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(635, 883),
            RobotManager.ScreenSize.HD to Pair<Int, Int>(351, 594)
    )


    override fun detect(screenText: String): Boolean {
        return screenText.contains("MONGODB ATLAS\n" +
                "Choose a path. Adjust anytime.\n" +
                "Available as a fully managed service across 60+ regions on AWS, Azure, and Google Cloud")
                && screenText.contains("Starting at\nFREE\nCreate a cluster")
    }

    override fun action(robot: RobotManager) {
        robot.end()
        robot.click(startingAtFreeCreateClusterBtnPosition)
    }
}

class MladEnterClusterNamePage(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    val clusterNameInputPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(470, 464)
    )
    val continueBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(1072, 562)
    )


    override fun detect(screenText: String): Boolean {
        return screenText.contains("CLUSTERS > CREATE A STARTER CLUSTER")
                && screenText.contains("Your cluster name is used to generate your hostname and cannot be changed later.")
                && screenText.contains("Enter cluster name")
                && screenText.contains("Cluster names can only contain ASCII letters, numbers, and hyphens.")
                && screenText.contains("ContinueBack")
    }

    override fun action(robot: RobotManager) {
        robot.click(clusterNameInputPosition)
        robot.clearAndPasteInput("Cluster0")
        robot.click(continueBtnPosition)
    }
}

class MladSelectClusterTier(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    val asiaLocationBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(910, 622)
    )
    val googleCloudBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(503, 435)
    )
    val azureBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(662, 435)
    )
    val finalCreateClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1305, 1005),
            RobotManager.ScreenSize.HD to Pair<Int, Int>(1028, 692)
    )

    val resolveCaptchaBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.HD to Pair<Int, Int>(601, 697)
    )

    override fun detect(screenText: String): Boolean {
        return screenText.contains("Create a free tier cluster by selecting a region with FREE TIER AVAILABLE and choosing the M0 cluster tier below.")
                && screenText.contains("FREE\nFree forever! Your M0 cluster is ideal for experimenting in a limited sandbox. You can upgrade to a production cluster anytime.")
                && screenText.contains("Back\nCreate Cluster")
    }

    override fun action(robot: RobotManager) {
        when (RandomUtils.nextInt(0, 3)) {
            0 -> {
                //amazon web service ~ default
            }
            1 -> {
                robot.click(googleCloudBtnPosition)
            }
            2 -> {
                robot.click(azureBtnPosition)
            }
        }

        robot.click(asiaLocationBtnPosition)
        robot.end()
        robot.click(finalCreateClusterBtnPosition)
    }
}


class MladDashBoardClusterIsBeingCreated(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    override fun onWaiting(screenText: String) = RPageResponse.OK()

    override fun detect(screenText: String): Boolean {
        return screenText.contains("None Linked\nYour cluster is being created\nNew clusters take between 7-10 minutes to provision.")
    }

}

class CaptchaSelectAllImages(var resolveBtn: HashMap<RobotManager.ScreenSize, Pair<Int, Int>>, onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    override fun detect(screenText: String): Boolean {
        return (screenText.startsWith("Select all images with") || screenText.startsWith("Select all squares with"))
                && (screenText.endsWith("VERIFY") || screenText.endsWith("If there are none, click skip\nSKIP"))
    }

    override fun action(robot: RobotManager) {
        robot.click(resolveBtn)
    }
}

class CaptchaMu(var resolveBtn: HashMap<RobotManager.ScreenSize, Pair<Int, Int>>, onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    override fun detect(screenText: String): Boolean {
        return screenText == "Multiple correct solutions required - please solve more.\n" +
                "Press PLAY and enter the words you hear\n" +
                "PLAY\n" +
                "VERIFY"
    }

    override fun action(robot: RobotManager) {
        robot.click(resolveBtn)
    }
}