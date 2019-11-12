package net.scr0pt.thirdservice.mlab

import net.scr0pt.robot.RPage
import net.scr0pt.robot.RPageCaptchaResponse
import net.scr0pt.robot.RPageMlabResponse
import net.scr0pt.robot.RPageResponse
import net.scr0pt.utils.RobotManager
import org.apache.commons.lang3.RandomUtils
import java.util.*

/**
 * Created by Long
 * Date: 11/12/2019
 * Time: 12:18 PM
 */


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

class CaptchaProsecc(onPageFinish: (() -> Unit)? = null) : RPage(onPageFinish) {
    override fun onWaiting(screenText: String): RPageResponse? {
        return RPageCaptchaResponse.CAPTCHA_FAIL()
    }

    override fun detect(screenText: String): Boolean {
        return screenText == "Press PLAY and enter the words you hear\nPLAY\nVERIFY"
    }
}