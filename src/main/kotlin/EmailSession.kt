package com.github.tacticallaptopbag.email_blaster

import org.apache.commons.mail.HtmlEmail
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class EmailSession(
    guildId: String,
    private val _subject: String,
    private val _message: String,
    ccSelf: Boolean = true,
) {
    private val _email = HtmlEmail()

    companion object {
        private val _logger = LoggerFactory.getLogger(EmailSession::class.java)
    }

    init {
        val persistence = Persistence(guildId)
        val guildUser = persistence.get(KEY_EMAIL_USER)
        val guildPassword = persistence.get(KEY_EMAIL_PASS)
        val guildHostName = persistence.get(KEY_EMAIL_HOST)
        val guildSmtpPort = persistence.get(KEY_EMAIL_PORT)
        val guildSSL = persistence.get(KEY_EMAIL_SSL)

        val useGuildSettings = !guildUser.isNullOrBlank()
                && !guildPassword.isNullOrBlank()
                && !guildHostName.isNullOrBlank()
                && !guildSmtpPort.isNullOrBlank()
                && !guildSSL.isNullOrBlank()

        val user = if(useGuildSettings) guildUser else MailBlasterProperties.emailUser
        val password = if(useGuildSettings) guildPassword else MailBlasterProperties.emailPassword
        val hostName = if(useGuildSettings) guildHostName else MailBlasterProperties.emailHostName
        val smtpPort = if(useGuildSettings) guildSmtpPort!!.toInt() else MailBlasterProperties.emailSmtpPort
        val enableSSL = if(useGuildSettings) guildSSL.toBoolean() else MailBlasterProperties.emailEnableSSL

        _email.hostName = hostName
        _email.setSmtpPort(smtpPort)
        _email.setAuthentication(user, password)
        _email.isSSLOnConnect = enableSSL
        _email.setFrom(user)

        if(useGuildSettings && ccSelf) {
            _email.addCc(user)
        }
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