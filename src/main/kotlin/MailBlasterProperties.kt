package com.github.tacticallaptopbag.email_blaster

import java.util.*

object MailBlasterProperties {
    private val _properties = Properties()

    init {
        val classLoader = Thread.currentThread().contextClassLoader
        _properties.load(classLoader.getResourceAsStream("mail-blaster.properties"))
    }

    val secret = _properties.getProperty(KEY_SECRET)!!
    val emailUser = _properties.getProperty(KEY_EMAIL_USER)!!
    val emailEncryptedPassword = _properties.getProperty(KEY_EMAIL_PASS)!!
    val emailHostName = _properties.getProperty(KEY_EMAIL_HOST)!!
    val emailSmtpPort = _properties.getProperty(KEY_EMAIL_PORT).toInt()
    val emailEnableSSL = _properties.getProperty(KEY_EMAIL_SSL).toBoolean()
    val emailSubjectPrefix = _properties.getProperty(KEY_EMAIL_SUBJECT_PREFIX)!!
    val emailDefaultSubject = _properties.getProperty(KEY_EMAIL_SUBJECT_DEFAULT)!!

    val discordToken = _properties.getProperty(KEY_DISCORD_TOKEN)!!
}