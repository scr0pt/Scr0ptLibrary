package net.scr0pt.utils.tempmail

import org.jsoup.nodes.Element
import net.scr0pt.utils.tempmail.event.MailReceiveEvent
import net.scr0pt.utils.tempmail.event.onReceiveMails
import net.scr0pt.utils.tempmail.models.Mail
import java.util.*

abstract class GenericMail(val onInnitSuccess: ((GenericMail) -> Unit)?, val onInitFail: (() -> Unit)?) {
    val inbox = arrayListOf<Mail>()
    var isLogout = false
    var timeUpdate = 5000L//milisecond
    private var schedule: Timer? = null
    val events = arrayListOf<MailReceiveEvent>()
    fun onEvent(event: MailReceiveEvent) {
        events.removeIf { it.key == event.key }
        events.add(event)
    }

    var emailAddress: String? = null

    init {
        Thread(Runnable {
            if (init()) {
                onInnitSuccess?.let { it(this) }
                startSchedule()
            } else {
                onInitFail?.let { it() }
            }
        }).start()
    }

    fun startSchedule() {
        schedule = Timer().also { timer ->
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    onUpdateInbox()
                }
            }, 0, timeUpdate)
        }
    }

    abstract fun init(): Boolean

    abstract fun updateInbox(): List<Mail>?

    fun onUpdateInbox() {
        if (isLogout) return
        println("onUpdateInbox")
        updateInbox()
                ?.takeIf { !isLogout && it.isNotEmpty() }
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
        println("GenericMail logout")
        isLogout = true
        events.clear()
        schedule?.cancel()
    }
}