package net.scr0pt.utils

import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent


class RobotManager {
    val CLIPBOARD_DELETED = "SystemClipboardDeleted"
    val screenResolution = Toolkit.getDefaultToolkit().screenResolution
    val screenSize = Toolkit.getDefaultToolkit().screenSize

    enum class BrowserType {
        CHROME {
            override fun baseY() = 30
        },
        FIREFOX {
            override fun baseY() = 0
        };

        abstract fun baseY(): Int
    }

    val browerType = BrowserType.CHROME
    val robot = Robot()
    val run = Runtime.getRuntime()

    fun sleep() {
        Thread.sleep(1000)
    }

    fun longSleep() {
        Thread.sleep(5000)
    }

    fun realylongSleep() {
        Thread.sleep(20000)
    }

    fun openBrowser() {
        if (browerType == BrowserType.CHROME) {
            run.exec("\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\" -incognito -start-maximized")
        } else if (browerType == BrowserType.FIREFOX) {
            run.exec("\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\" -private-window")
        }
        realylongSleep()
    }

    fun switchWindow() {
        robot.keyPress(KeyEvent.VK_ALT)
        robot.keyPress(KeyEvent.VK_TAB)
        robot.keyRelease(KeyEvent.VK_ALT)
        robot.keyRelease(KeyEvent.VK_TAB)
        sleep()
    }

    fun tab() {
        robot.keyPress(KeyEvent.VK_TAB)
        robot.keyRelease(KeyEvent.VK_TAB)
        sleep()
    }

    fun shiftTab() {
        robot.keyPress(KeyEvent.VK_SHIFT)
        robot.keyPress(KeyEvent.VK_TAB)
        robot.keyRelease(KeyEvent.VK_SHIFT)
        robot.keyRelease(KeyEvent.VK_TAB)
        sleep()
    }

    fun enter() {
        robot.keyPress(KeyEvent.VK_ENTER)
        robot.keyRelease(KeyEvent.VK_ENTER)
        sleep()
    }

    fun clearInput() {
        ctrA()
        robot.keyPress(KeyEvent.VK_DELETE)
        sleep()
    }

    fun clearAndPasteInput(text: String? = null) {
        if (text != null) {
            SystemClipboard.copy(text)
        }
        clearInput()
        ctrV()
    }

    fun waitUntilUrlChangedTo(url: String){
        var _url: String
        do {
            sleep()
            _url = getCurrentUrl()
        } while (_url != url)
    }

    fun ctrA() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_A)
        sleep()
    }

    fun ctrC() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_C)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_C)
        sleep()
    }

    fun ctrV() {
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.keyRelease(KeyEvent.VK_V)
        sleep()
    }

    fun mouseMove(x: Int, y: Int) {
        robot.mouseMove(x, y)
        println("mouseMove to ${x}:${y} color: ${robot.getPixelColor(x, y)}")
        sleep()
    }

    fun click(x: Int, y: Int) {
        mouseMove(x,y)
        click()
    }

    fun click(position: Pair<Int, Int>) {
        mouseMove(position.first, position.second)
        click()
    }

    fun click() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        sleep()
    }

    fun mouseMove(position: Pair<Int, Int>) {
        mouseMove(position.first, position.second)
    }


    fun printScreenText(): String {
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~")
        ctrA()
        ctrC()
        val clipboardTxt = SystemClipboard.get()
        SystemClipboard.copy(CLIPBOARD_DELETED)
        println(clipboardTxt)
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~")
        return clipboardTxt
    }

    fun clearClipboard() {
        SystemClipboard.copy(CLIPBOARD_DELETED)
    }

    fun moveToAddressBarAndClick() {
        mouseMove(500, 50)
        click()
    }

    fun browserGoTo(url: String) {
        moveToAddressBarAndClick()
        clearAndPasteInput(url)
        robot.keyPress(KeyEvent.VK_ENTER)
        realylongSleep()
    }

    fun getCurrentUrl(): String {
        moveToAddressBarAndClick()
        return printScreenText()
    }

    fun closeWindow() {
        mouseMove(Toolkit.getDefaultToolkit().getScreenSize().width - 13, 13)
        click()
    }

}
