package net.scr0pt.robot

import net.scr0pt.utils.RobotManager
import java.awt.Robot
import java.util.HashMap


fun main(args: Array<String>) {
    val rPageManager = RPageManager("https://cloud.mongodb.com/user#/atlas/login")
    rPageManager.pages.addAll(arrayListOf(
            MlabLoginEnterEmailPass("tranvana.n.h.200896@gmail.com", "XinChaoVietnam"),
            MladDashBoardClusterCreated(),
            MladDashBoardClusterCreateYourCluster(),
            MladChooseAPath(),
            MladSelectClusterTier(),
            MladDashBoardClusterIsBeingCreated()
    ))
    rPageManager.start()
}


class RPageManager(originUrl: String) {
    val robot = RobotManager(RobotManager.BrowserType.CHROME_INCOGNITO)
    val safePoint = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1908, 1025)
    )
    val pages = arrayListOf<RPage>()

    init {
        robot.openBrowser()
        robot.browserGoTo(originUrl)
    }

    fun start() {
        var response: RPageResponse? = null
        while (true) {
            robot.sleep()
            robot.click(safePoint)
            val screenText = robot.getScreenText()
            val detectPages = pages.filter { it.detect(screenText) }
            if (detectPages.isEmpty()) {

            } else if (detectPages.size == 1) {
                detectPages.firstOrNull()?.let { page ->
                    println("Detect page ${page.javaClass.simpleName}")
                    page.action(robot)
                    page.onWaiting(screenText)?.let {
                        return@start
                    }
                }
                robot.sleep()
            } else {

            }
        }
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

abstract class RPage() {
    abstract fun detect(screenText: String): Boolean
    open fun onWaiting(screenText: String): RPageResponse? = null
    open fun action(robot: RobotManager) {}
}

class MlabLoginEnterEmailPass(val email: String, val password: String) : RPage() {
    val usernamePosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(964, 491)
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

class MladDashBoardClusterCreated() : RPage() {
    override fun onWaiting(screenText: String) = RPageResponse.OK()

    override fun detect(screenText: String): Boolean {
        return screenText.contains("CONNECT\n" +
                "METRICS\n" +
                "COLLECTIONS")
                && screenText.contains("CLUSTER TIER\n" +
                "M0 Sandbox (General)")
                && screenText.contains("Enhance Your Experience\n" +
                "For dedicated throughput, richer metrics and enterprise security options, upgrade your cluster now!")
                && !screenText.contains("None Linked\n" +
                "Your cluster is being created\n" +
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

class MladDashBoardClusterCreateYourCluster() : RPage() {
    val buildAClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1050, 541)
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

class MladChooseAPath() : RPage() {
    val startingAtFreeCreateClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(635, 883)
    )


    override fun detect(screenText: String): Boolean {
        return screenText.contains("MONGODB ATLAS\n" +
                "Choose a path. Adjust anytime.\n" +
                "Available as a fully managed service across 60+ regions on AWS, Azure, and Google Cloud")
                && screenText.contains("Starting at\n" +
                "FREE\n" +
                "Create a cluster")
    }

    override fun action(robot: RobotManager) {
        robot.click(startingAtFreeCreateClusterBtnPosition)
    }
}

class MladSelectClusterTier() : RPage() {
    val finalCreateClusterBtnPosition: HashMap<RobotManager.ScreenSize, Pair<Int, Int>> = hashMapOf(
            RobotManager.ScreenSize.FullHD to Pair<Int, Int>(1305, 1005)
    )


    override fun detect(screenText: String): Boolean {
        return screenText.contains("Create a free tier cluster by selecting a region with FREE TIER AVAILABLE and choosing the M0 cluster tier below.")
                && screenText.contains("FREE\n" +
                "Free forever! Your M0 cluster is ideal for experimenting in a limited sandbox. You can upgrade to a production cluster anytime.")
                && screenText.contains("Back\n" +
                "Create Cluster")
    }

    override fun action(robot: RobotManager) {
        robot.click(finalCreateClusterBtnPosition)
    }
}


class MladDashBoardClusterIsBeingCreated() : RPage() {
    override fun onWaiting(screenText: String) = RPageResponse.OK()

    override fun detect(screenText: String): Boolean {
        return screenText.contains("None Linked\n" +
                "Your cluster is being created\n" +
                "New clusters take between 7-10 minutes to provision.")
    }

}