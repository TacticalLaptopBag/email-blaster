package com.github.tacticallaptopbag.email_blaster

import org.apache.commons.mail.HtmlEmail
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class EmailSession(
    private val _subject: String,
    private val _message: String,
) {
    private val _email = HtmlEmail()

    companion object {
        private val _logger = LoggerFactory.getLogger(EmailSession::class.java)
    }

    init {
        val user = MailBlasterProperties.emailUser
        val password = MailBlasterProperties.emailPassword

        _email.hostName = MailBlasterProperties.emailHostName
        _email.setSmtpPort(MailBlasterProperties.emailSmtpPort)
        _email.setAuthentication(user, password)
        _email.isSSLOnConnect = MailBlasterProperties.emailEnableSSL
        _email.setFrom(user)
        _email.addCc(user)
    }

    fun addTo(email: String) {
        _email.addBcc(email)
    }

    fun sendHTML() {
        _email.subject = _subject

        val classLoader = Thread.currentThread().contextClassLoader
        classLoader.getResourceAsStream("email-template.html")?.let { emailTemplateStream ->
            val emailTemplate = BufferedReader(InputStreamReader(emailTemplateStream))
                .lines().collect(Collectors.joining("\n"))

            // TODO: Parse markdown in message into HTML
            val htmlMessage = _message.replace("\n", "\n<br>\n")
            _email.setHtmlMsg(emailTemplate.replace("{{CONTENT}}", htmlMessage))
        } ?: {
            _logger.warn("Unable to find email-template.html in resources. This message will not be sent as HTML.")
            _email.setMsg(_message)
        }

        _email.send()
    }

    fun send() {
        _email.subject = _subject
        _email.setMsg(_message)
        _email.send()
    }
}