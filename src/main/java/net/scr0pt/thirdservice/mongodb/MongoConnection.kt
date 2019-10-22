package net.scr0pt.thirdservice.mongodb

import net.scr0pt.utils.localstorage.LocalStorage

object MongoConnection {
    val malConnection
        get() = LocalStorage("MongoUrl.txt").getItem("mal")
    val megaConnection
        get() = LocalStorage("MongoUrl.txt").getItem("mega")
    val eduConnection
        get() = LocalStorage("MongoUrl.txt").getItem("edu")
}