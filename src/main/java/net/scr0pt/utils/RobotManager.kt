package net.scr0pt.utils

import java.awt.BorderLayout
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JTextField

fun main() {
    detectMousePosition()
}

class RobotManager(val browerType: BrowserType = BrowserType.CHROME) {
    val CLIPBOARD_DELETED = "SystemClipboardDeleted"
    val screenResolution = Toolkit.getDefaultToolkit().screenResolution
    val screenSize = Toolkit.getDefaultToolkit().screenSize

    enum class BrowserType {
        CHROME {
            override fun cmd() = ""
            override fun baseY() = 30
            override fun path() = "\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\""
        },
        FIREFOX {
            override fun path() = "\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\""
            override fun cmd() = ""
            override fun baseY() = 0
        },
        CHROME_INCOGNITO {
            override fun path() = "\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\""
            override fun cmd() = "-incognito"
            override fun baseY() = 30
        },
        FIREFOX_INCOGNITO {
            override fun path() = "\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\""
            override fun cmd() = "-private-window"
            override fun baseY() = 0
        };

        abstract fun baseY(): Int
        abstract fun cmd(): String
        abstract fun path(): String
    }

    val robot = Robot()
    val run = Runtime.getRuntime()

    fun sleep() {
        Thread.sleep(500)
    }

    fun longSleep() {
        Thread.sleep(5000)
    }

    fun realylongSleep() {
        Thread.sleep(20000)
    }

    fun openBrowser() {
        run.exec("${browerType.path()} ${browerType.cmd()} -start-maximized")
        longSleep()
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

    fun waitUntilUrlChangedTo(url: String) {
        var _url: String
        do {
            sleep()
            _url = getCurrentUrl()
        } while (_url != url)
    }

    fun waitUntilUrlStartWith(url: String) {
        var _url: String
        do {
            sleep()
            _url = getCurrentUrl()
        } while (!_url.startsWith(url))
    }

    fun waitUntilUrlFit(filter: (String) -> Boolean) {
        var _url: String
        do {
            sleep()
            _url = getCurrentUrl()
        } while (!filter(_url))
    }

    fun waitUntilUrlEndWith(url: String) {
        var _url: String
        do {
            sleep()
            _url = getCurrentUrl()
        } while (!_url.endsWith(url))
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
        mouseMove(x, y)
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
        val clipboardTxt = getScreenText()
        println(clipboardTxt)
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~")
        return clipboardTxt
    }


    fun getScreenText(): String {
        clearClipboard()
        ctrA()
        ctrC()
        val clipboardTxt = SystemClipboard.get()
        clearClipboard()
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
        longSleep()
    }

    fun getCurrentUrl(): String {
        moveToAddressBarAndClick()
        return printScreenText()
    }

    fun closeWindow() {
        mouseMove(Toolkit.getDefaultToolkit().getScreenSize().width - 13, 13)
        click()
        longSleep()
    }

    fun isInputReady(): Boolean {
        val text = "isInputReady"
        clearAndPasteInput(text)
        return getScreenText() == text
    }

    fun isScreenTextContain(list: List<String>): Boolean {
        val text = getScreenText()
        return list.all { text.contains(it) }
    }

}

fun detectMousePosition() {
    val frame = JFrame("Key Listener")
    val contentPane = frame.contentPane
    val listener = object : KeyListener {
        override fun keyReleased(e: KeyEvent?) {
        }

        override fun keyTyped(e: KeyEvent?) {
        }

        override fun keyPressed(event: KeyEvent) {
            if (event.extendedKeyCode == KeyEvent.VK_SPACE) {
                val location = MouseInfo.getPointerInfo().location
                println("")
                println("Pair<Int, Int>(${location.x}, ${location.y})")
                println("")
                println("val position = Pair<Int, Int>(${location.x}, ${location.y})")
            }
        }
    }

    val textField = JTextField()
    textField.addKeyListener(listener)
    contentPane.add(textField, BorderLayout.NORTH)
    frame.pack()
    frame.isVisible = true
}

