package net.scr0pt.selenium

import net.scr0pt.utils.RobotManager

/**
 * Created by Long
 * Date: 11/9/2019
 * Time: 2:22 PM
 */

fun isCaptchaOpen(text: String) = text.startsWith("Select all images with") || text.startsWith("Select all squares with")

fun bypassCaptcha(initialResolveCaptchaBtn: Pair<Int, Int>? = null, multipleCorrect: Pair<Int, Int>, newCapthchaBtn: Pair<Int, Int>, robotManager: RobotManager, onSuccess: () -> Unit, onFail: () -> Unit, onSpecialCase: (() -> Unit)? = null, isDone: ((String) -> Boolean)? = null) {
    with(robotManager) {
        println("bypassCaptcha 1")
        for (i in 0..40) {//Select all images with
            println("bypassCaptcha 2")
            val text = printScreenText().trim()
            if (isCaptchaOpen(text)) break
            println("bypassCaptcha 3")

            if (i > 10 && (text == "I'm not a robot\nPrivacy - Terms" || (isDone != null && isDone(text)))) {
                println("bypassCaptcha 4")
                onSuccess()
                return@bypassCaptcha
            }
            println("bypassCaptcha 5")

            sleep()
            println(text)
        }

        println("bypassCaptcha 6")
        initialResolveCaptchaBtn?.let { click(it) }

        for (i in 0..40) {
            println("bypassCaptcha 7")
            sleep()
            val text = printScreenText().trim()
            when {
                text == "I'm not a robot\nPrivacy - Terms" -> {
                    println("bypassCaptcha 8")
                    onSuccess()
                    return@bypassCaptcha
                }
                text == "Multiple correct solutions required - please solve more.\nPress PLAY and enter the words you hear\nPLAY\nVERIFY" -> {
                    println("bypassCaptcha 9")
                    click(multipleCorrect)
                    bypassCaptcha(null, multipleCorrect, newCapthchaBtn, robotManager, onSuccess, onFail)
                    return@bypassCaptcha
                }
                text == "Try again later\nYour computer or network may be sending automated queries. To protect our users, we can't process your request right now. For more details visit our help page" -> {
                    println("bypassCaptcha 10")
                    println("onFail")
                    onFail()
                    return@bypassCaptcha
                }
                text.endsWith("Signing up signifies that you have read and agree to the Terms of Service and our Privacy Policy.", ignoreCase = true) -> {
                    println("bypassCaptcha 11")
                    println("onFail")
                    onFail()
                    return@bypassCaptcha
                }
                isDone != null && isDone(text) -> {
                    println("bypassCaptcha 11.1")
                    onSuccess()
                    return@bypassCaptcha
                }
            }
        }
        println("bypassCaptcha 12")
        var text: String
        do {
            sleep()
            text = printScreenText().trim()
        } while (text == "Press PLAY and enter the words you hear")

        println("bypassCaptcha 13")
        //Press PLAY and enter the words you hear: verifying
        if (text == "Press PLAY and enter the words you hear\nPLAY\nVERIFY") {
            println("bypassCaptcha 14")
            onFail()
        } else {
            println("bypassCaptcha 15")
            print("sdfsdfsdfsdf: $text")
            onSpecialCase?.let { it() }
        }
    }
}