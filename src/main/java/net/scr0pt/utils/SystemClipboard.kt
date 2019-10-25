package net.scr0pt.utils

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object SystemClipboard {
    val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
    fun copy(text: String){
        systemClipboard.setContents(StringSelection(text), null)
    }
    fun get(): String = systemClipboard.getData(DataFlavor.stringFlavor).toString()
}