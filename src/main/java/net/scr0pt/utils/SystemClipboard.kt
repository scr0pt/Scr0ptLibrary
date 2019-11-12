package net.scr0pt.utils

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object SystemClipboard {
    val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
    fun copy(text: String) {
        try {
            systemClipboard.setContents(StringSelection(text), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun get(): String = try {
        systemClipboard.getData(DataFlavor.stringFlavor).toString()
    } catch (e: Exception) {
        ""
    }
}