package com.github.tacticallaptopbag.email_blaster

import java.util.*

object MailBlasterProperties {
    private val _properties = Properties()

    init {
        val classLoader = Thread.currentThread().contextClassLoader
        _properties.load(classLoader.getResourceAsStream("mail-blaster.properties"))
    }

    val emailUser = _properties.getProperty("mail-blaster.email.user")!!
    val emailPassword = _properties.getProperty("mail-blaster.email.password")!!
    val emailHostName = _properties.getProperty("mail-blaster.email.hostName")!!
    val emailSmtpPort = _properties.getProperty("mail-blaster.email.smtpPort").toInt()
    val emailEnableSSL = _properties.getProperty("mail-blaster.email.enableSSL").toBoolean()

    val emailSubjectPrefix = _properties.getProperty("mail-blaster.email.subjectPrefix")!!
    val emailDefaultSubject = _properties.getProperty("mail-blaster.email.defaultSubject")!!

    val discordToken = _properties.getProperty("mail-blaster.discord.token")!!
}