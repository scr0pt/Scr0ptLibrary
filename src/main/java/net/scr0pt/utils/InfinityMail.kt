package net.scr0pt.utils


fun main() {
    val infinityMail = InfinityMail("alph@gmail.com")
    var mailAddress: String? = null
    var i = 0
    do {
        mailAddress = infinityMail.getNext()?.username
        println("${++i}: $mailAddress")
    } while (mailAddress != null)
}

class InfinityMail(var username: String) {
    data class GenerateInfinityMail(
            val username: String,
            val domain: String? = null,
            var isScan: Boolean = false,
            var isRead: Boolean = false
    ) {
        val fullAddress
            get() = username + domain?.run { "@$this" }
    }

    var domain: String? = null

    init {
        if (username.contains("@")) {
            domain = username.substringAfterLast("@")
            username = username.substringBeforeLast("@")
        }
    }

    private val list = arrayListOf(GenerateInfinityMail(username, domain, false))

    private fun generateMail(generateInfinityMail: GenerateInfinityMail): Boolean {
        if (generateInfinityMail.isScan == true) {
            return false
        } else {
            generateInfinityMail.isScan = true
        }

        var sussess = false
        val email = generateInfinityMail.username
        for (j in 1 until email.length) {
            if (!(email[j - 1].toString() == ".") && !(email[j].toString() == ".")) {
                val mynewEmailAddress = email.substring(0, j) + "." + email.substring(j)
                val mynewEmail = GenerateInfinityMail(mynewEmailAddress, domain)
                if (list.none { it.username == mynewEmail.username }) {
                    list.add(mynewEmail)
                    sussess = true
                }
            }
        }
        return sussess
    }

    private fun generateNext(): Boolean {
        list.firstOrNull { it.isScan == false }?.let {
            if (generateMail(it)) {
                return true
            } else {
                return generateNext()
            }
        }
        return false
    }

    fun getNext(): GenerateInfinityMail? {
        list.firstOrNull { it.isRead == false }?.let {
            it.isRead = true
            return@getNext it
        }

        if (generateNext()) {
            return getNext()
        }

        return null
    }
}