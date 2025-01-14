package com.github.tacticallaptopbag.email_blaster

import org.apache.commons.mail.HtmlEmail
import java.util.*

class EmailSession(
    private val _subject: String,
    private val _message: String,
    private val _html: Boolean = false,
) {
    private val _email = HtmlEmail()

    init {
        val user = MailBlasterProperties.emailUser
        val password = MailBlasterProperties.emailPassword

        _email.hostName = MailBlasterProperties.emailHostName
        _email.setSmtpPort(MailBlasterProperties.emailSmtpPort)
        _email.setAuthentication(user, password)
        _email.isSSLOnConnect = MailBlasterProperties.emailEnableSSL
        _email.setFrom(user)
        _email.addBcc(user)
    }

    fun addTo(email: String) {
        _email.addTo(email)
    }

    fun send() {
        _email.subject = _subject
        if(_html) {
            _email.setHtmlMsg(_message)
        } else {
            _email.setTextMsg(_message)
        }

        _email.send()
    }
}