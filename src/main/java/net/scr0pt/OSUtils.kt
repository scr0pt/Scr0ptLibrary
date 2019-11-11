package net.scr0pt

import java.awt.Toolkit

fun main() {
    println(System.getProperty("sun.arch.data.model"))
}

object OSUtils {
    val OSName = System.getProperty("os.name")
    val OSModel = System.getProperty("sun.arch.data.model")

    fun isWindows() = OSName.startsWith("Windows")
    fun isLinux() = OSName.startsWith("Linux")
    fun isMac() = OSName.startsWith("Mac")

    fun makeSound() {
        Thread(Runnable {
            for (i in 0..5) {
                Toolkit.getDefaultToolkit().beep()
                Thread.sleep(500)
            }
        }).start()
    }
}