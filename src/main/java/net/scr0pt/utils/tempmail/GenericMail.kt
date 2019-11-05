package net.scr0pt.utils.tempmail

import kotlinx.coroutines.runBlocking
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.event.onReceiveMails
import net.scr0pt.utils.tempmail.models.Mail
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

abstract class GenericMail(val onInnitSuccess: ((GenericMail) -> Unit)?, val onInitFail: (() -> Unit)?) {
    val TAG: String = this::class.java.simpleName
    val inbox = arrayListOf<Mail>()
    var isLogout = false
    var timeUpdate = 5000L//milisecond
    private var schedule: Timer? = null
    val events = arrayListOf<MailReceiveEvent>()
    fun onEvent(event: MailReceiveEvent) {
        events.removeIf { it.key == event.key }
        events.add(event)
    }

    fun log(msg: String) {
        println("$TAG [${SimpleDateFormat("HH:mm:ss").format(Date())}]: $msg")
    }

    var emailAddress: String? = null

    fun connect() {
        log("connect")
        runBlocking {
            if (init()) {
                onInnitSuccess?.invoke(this@GenericMail)
                startSchedule()
            } else {
                onInitFail?.invoke()
            }
        }
    }

    fun startSchedule() {
        log("startSchedule")
        schedule = Timer().also { timer ->
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    log("Timer schedule running")
                    onUpdateInbox()
                }
            }, 0, timeUpdate)
        }
    }

    abstract fun init(): Boolean

    abstract fun updateInbox(): List<Mail>?

    fun onUpdateInbox() {
        if (isLogout) return
        log("onUpdateInbox")
        val newInbox = updateInbox()
        log("child updateInbox done")
        newInbox?.takeIf { !isLogout && it.isNotEmpty() }
                ?.let { newInboxs ->
                    val filter =
                            newInboxs.filter { newInbox -> inbox.none { it.id != null && newInbox.id != null && it.id == newInbox.id } }//new inbox mails that not in old inbox mails
                    if (filter.isNotEmpty()) {
                        inbox.addAll(filter)
                        if (events.isNotEmpty()) {
                            events.onReceiveMails(filter) { mail ->
                                this.getMailContent(mail)
                            }
                        }
                    }
                }
    }

    /*  fun onMailAdded(mails: List<Mail>) {
          inbox.addAll(mails)
          events.onReceiveMails(mails) { mail ->
              this.getMailContent(mail)
          }
      }*/

    abstract fun getMailContent(mail: Mail): Element?

    open fun logout() {
        log("logout")
        isLogout = true
        events.clear()
        schedule?.cancel()
        schedule = null
    }
}