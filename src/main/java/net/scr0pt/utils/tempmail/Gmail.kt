package net.scr0pt.utils.tempmail

import com.sun.mail.imap.IMAPBodyPart
import net.scr0pt.utils.curl.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.models.Mail
import java.util.*
import javax.mail.*
import javax.mail.event.MessageCountAdapter
import javax.mail.event.MessageCountEvent
import javax.mail.internet.MimeMultipart
import kotlin.text.StringBuilder

fun main() {
    NetworkUtils.getUnsafeOkHttpClient()

    //1571281709000
    val registerTime = 1571290983527
    val gmail = Gmail("vinhnguyen4h4@gmail.com", "eHK;HyL.e=2k1704FgqN").apply {
        onEvent(
                MailReceiveEvent(
                        key = "ona1sender",
                        validator = { mail ->
                            Mail.CompareType.EQUAL_IGNORECASE.compare(
                                    mail.from,
                                    "animebentojp@gmail.com"
                            )
                        },
                        callback = { mails ->
                            mails.forEach { println(it.content) }
                        },
                        once = false,
                        new = true,
                        fetchContent = true
                )
        )
    }
}


class Gmail(
        val username: String,
        val password: String,
        onInnitSuccess: ((GenericMail) -> Unit)? = null,
        onInitFail: (() -> Unit)? = null
) :
        GenericMail(onInnitSuccess, onInitFail) {
    object MailConfig {
        val IMAP_PORT: Int = 993
        val HOST_NAME = "smtp.gmail.com"
        val IMAP_HOST = "imap.gmail.com"
        val IMAP_PROTOCOL = "imaps"
        val SSL_PORT = 465 // Port for SSL
        val TSL_PORT = 587 // Port for TLS/STARTTLS
    }


    private var session: Session? = null
    private var store: Store? = null
    private var inboxFolder: Folder? = null
    private var spamFolder: Folder? = null

    // hardcoding protocol and the folder
    // it can be parameterized and enhanced as required
    private val inboxFolderName = "INBOX"
    private val spamFolderName = "[Gmail]/Spam"

    val isLoggedIn: Boolean
        get() = store?.isConnected ?: false

    override fun init(): Boolean {
        this.emailAddress = username
        return login(
                MailConfig.IMAP_PROTOCOL,
                MailConfig.IMAP_HOST,
                username,
                password,
                MailConfig.IMAP_PORT
        )
    }


    var messages = arrayListOf<Message>()
    override fun updateInbox(): List<Mail>? {
        val list = arrayListOf<Mail>()
        messages = arrayListOf<Message>()
        spamFolder?.messages?.reversed()?.let {
            messages.addAll(it)
        }
        inboxFolder?.messages?.reversed()?.let {
            messages.addAll(it)
        }
        for (message in messages) {
            val mail = message.toMail()
            list.add(mail)
        }
        list.sortByDescending { it.receivedDate }
        return list
    }

    fun Message.toMail(): Mail {
        val from = this.from?.first()?.toString()?.substringAfter("<")?.substringBefore(">")
        val mail = Mail(from, emailAddress, this.subject)
        mail.receivedDate = this.receivedDate.time
        mail.sentDate = this.sentDate.time
        this.messageNumber
        mail.id = mail.receivedDate
        return mail
    }

    override fun getMailContent(mail: Mail): Element? {
        val message = messages?.first { it.receivedDate.time == mail.id }
        val content = getContent(message.content)
        return Jsoup.parse(content)
    }

    fun getContent(content: Any): String {
        val contentSb = StringBuilder()
        if (content is IMAPBodyPart) {
            contentSb.append(getContent(content.content)).append("\n")
        } else if (content is String) {
            contentSb.append(content).append("\n")
        } else {
            try {
                val a = content as MimeMultipart
                for (i in 0 until a.count) {
                    contentSb.append(getContent(a.getBodyPart(i))).append("\n")
                }
            } catch (e: Exception) {
//                e.printStackTrace()
            }
        }
        return contentSb.toString()
    }

    /**
     * to login to the mail host server
     */
    fun login(protocol: String, host: String, username: String, password: String, port: Int): Boolean {
        return try {
            val url = URLName(protocol, host, port, inboxFolderName, username, password)
            if (session == null) {
                val props: Properties = try {
                    System.getProperties()
                } catch (sex: Exception) {
                    sex.printStackTrace()
                    Properties()
                }
                session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })
            }
            store = session?.getStore(url)
            if (!isLoggedIn) store?.connect(host, port, username, password)

//            store?.defaultFolder.list("*")
            inboxFolder = store?.getFolder(inboxFolderName)?.apply {
                open(Folder.READ_ONLY)

                /*addMessageCountListener(object: MessageCountAdapter(){
                    override fun messagesRemoved(e: MessageCountEvent?) {
                        e?.messages?.forEach {
                            it.receivedDate
                            println("Remove email: ${it.from.first()?.toString()}")
                        }
                    }

                    override fun messagesAdded(e: MessageCountEvent?) {
                        val list = arrayListOf<Mail>()
                        e?.messages?.forEach {
                            println("Added email: ${it.from.first()?.toString()}")
                            list.add(it.toMail())
                        }
                        onMailAdded(list)
                    }
                })*/
            }
            spamFolder = store?.getFolder(spamFolderName)?.apply {
                open(Folder.READ_ONLY)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * to logout from the mail host server
     */
    @Throws(MessagingException::class)
    override fun logout() {
        super.logout()

        spamFolder?.close(true)
        spamFolder = null
        inboxFolder?.close(true)
        inboxFolder = null
        store?.close()
        store = null
        session = null
    }
}