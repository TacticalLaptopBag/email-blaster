package com.github.tacticallaptopbag.email_blaster

import com.github.tacticallaptopbag.email_blaster.verifyaction.AddEmailAction
import com.github.tacticallaptopbag.email_blaster.verifyaction.RemoveEmailAction
import com.github.tacticallaptopbag.email_blaster.verifyaction.VerifyAction
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import org.slf4j.LoggerFactory
import java.time.Instant

class SlashCommandListener(
    private val _messageListener: MessageReceiveListener,
    private val _mailingList: MailingList
) : ListenerAdapter() {

    // https://emailregex.com/
    private val _emailRegex = """(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".toRegex()

    private val _verifyActions = mutableMapOf<Int, VerifyAction>()

    companion object {
        private val _logger = LoggerFactory.getLogger(SlashCommandListener::class.java)
    }

    private fun cmdSetChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel", OptionMapping::getAsChannel)
        val guildId = event.guild!!.id

        if(channel == null) {
            _logger.info("[$guildId] User failed to provide announcement channel")

            event.reply("You must supply a channel!")
                .setEphemeral(true)
                .queue()
        } else if(channel.type != ChannelType.TEXT) {
            _logger.info("[$guildId] User failed to provide a text channel")

            event.reply("You must supply a text channel!")
                .setEphemeral(true)
                .queue()
        } else {
            _logger.info("[$guildId] Setting announcement channel to ${channel.id}")
            _messageListener.setChannelId(event.guild!!.id, channel.id)
            event.reply("Successfully set announcement channel to <#${channel.id}>")
                .queue()
        }
    }

    private fun cmdEmailList(event: SlashCommandInteractionEvent) {
        val emails = _mailingList.loadAllEmails(event.guild!!.id)
        val msgBuilder = StringBuilder("Emails on the mailing list:")
        for(email in emails) {
            msgBuilder.append("\n- ")
            msgBuilder.append(email)
        }

        event.reply(msgBuilder.toString())
            .setEphemeral(true)
            .queue()
    }

    private fun cmdEmailAdd(event: SlashCommandInteractionEvent) {
        val email = event.getOption("email", OptionMapping::getAsString)
        val guildId = event.guild!!.id

        if(email == null) {
            _logger.info("[$guildId] User failed to provide an email to add")
            event.reply("You must supply an email to add!")
                .setEphemeral(true)
                .queue()
            return
        }

        if(!email.matches(_emailRegex)) {
            _logger.info("[$guildId] User tried to add an invalid email")
            event.reply("The supplied email is in an incorrect format!")
                .setEphemeral(true)
                .queue()
            return
        }

        if(event.member?.isAdmin() == true) {
            adminEmailAdd(event, email)
        } else {
            userEmailAdd(event, email)
        }
    }

    private fun sendVerificationEmail(event: SlashCommandInteractionEvent, email: String, message: String): Int {
        val guildId = event.guild!!.id
        val persistence = Persistence(guildId)
        val subjectPrefix = persistence.get(KEY_EMAIL_SUBJECT_PREFIX) ?: MailBlasterProperties.emailSubjectPrefix

        var code: Int
        do {
            code = (0..999999).random()
        } while(_verifyActions.contains(code))

        val subject = "$subjectPrefix Verify Email"
        event.reply("Verification email is on its way. Please check your inbox or spam folders for an email with the subject \"$subject\"")
            .setEphemeral(true)
            .queue()

        val session = EmailSession(
            guildId,
            subject,
            """
                $message
                
                Here is your verification code: <strong>$code</strong>
                Run <strong>`/verify $code`</strong> in the Discord server to complete this action
                
                This code will expire in 15 minutes.
            """.trimIndent(),
            ccSelf = false,
        )
        session.addTo(email)
        session.sendHTML()

        return code
    }

    private fun userEmailAdd(event: SlashCommandInteractionEvent, email: String) {
        val guildId = event.guild!!.id

        val code = sendVerificationEmail(event, email, """
                Either you, or another user has attempted to <em>add</em> this email to the mailing list for ${event.guild!!.name}.
                If you did not try to do this, simply delete this email.
        """.trimIndent())

        _verifyActions[code] = AddEmailAction(code, email, guildId, _mailingList)
    }

    private fun adminEmailAdd(event: SlashCommandInteractionEvent, email: String) {
        val guildId = event.guild!!.id

        _logger.info("[$guildId] Adding email to the mailing list")
        _mailingList.add(guildId, email)
        event.reply("Added \"$email\" to the mailing list.")
            .setEphemeral(true)
            .queue()
    }

    private fun cmdEmailRemove(event: SlashCommandInteractionEvent) {
        val email = event.getOption("email", OptionMapping::getAsString)
        val guildId = event.guild!!.id

        if(email == null) {
            _logger.info("[$guildId] User failed to provide an email to remove")
            event.reply("You must supply an email to remove!")
                .setEphemeral(true)
                .queue()
            return
        }

        if(event.member?.isAdmin() == true) {
            adminEmailRemove(event, email)
        } else {
            userEmailRemove(event, email)
        }
    }

    private fun adminEmailRemove(event: SlashCommandInteractionEvent, email: String) {
        val guildId = event.guild!!.id

        if(_mailingList.remove(guildId, email)) {
            _logger.info("[$guildId] User removed an email from the mailing list")
            event.reply("Successfully removed \"$email\" from the mailing list.")
                .setEphemeral(true)
                .queue()
        } else {
            _logger.info("[$guildId] User tried to remove an email from the mailing list, but it was not there.")
            event.reply("Could not find the given email in the mailing list!")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun userEmailRemove(event: SlashCommandInteractionEvent, email: String) {
        val guildId = event.guild!!.id
        val code = sendVerificationEmail(event, email, """
                Either you, or another user has attempted to <em>remove</em> this email from the mailing list for ${event.guild!!.name}.
                If you did not try to do this, simply delete this email.
        """.trimIndent())

        _verifyActions[code] = RemoveEmailAction(code, email, guildId, _mailingList)
    }

    private fun cmdEmailTest(event: SlashCommandInteractionEvent) {
        val guildId = event.guild!!.id
        if(Persistence(guildId).get(KEY_EMAIL_USER) == null) {
            event.reply("You can only run this command if you have setup an email with the /setup or /setupadvanced commands!")
                .setEphemeral(true)
                .queue()
            return
        }

        try {
            _logger.info("[$guildId] User is testing custom email configuration")
            val session = EmailSession(
                guildId,
                "Mail Blaster Test",
                "This is a test of the Mail Blaster Discord bot. If you see this message, congratulations! Your email is setup and working properly."
            )
            session.sendHTML()
            event.reply("Mail sent successfully! Check the inbox of the email you setup.")
                .setEphemeral(true)
                .queue()
        } catch(e: Exception) {
            _logger.info("[$guildId] Mail test failed:", e)
            var response = "Mail test failed! Exception message: ${e.message}."
            e.cause?.let { response += " Caused by: ${it.message}" }
            event.reply(response)
                .setEphemeral(true)
                .queue()
        }
    }

    private fun cmdSubjectPrefix(event: SlashCommandInteractionEvent) {
        val value = event.getOption("prefix", OptionMapping::getAsString)
        val guildId = event.guild!!.id
        val persistence = Persistence(guildId)
        if(value == null) {
            val subjectPrefix = persistence.get(KEY_EMAIL_SUBJECT_PREFIX) ?: MailBlasterProperties.emailSubjectPrefix
            event.reply("Current subject prefix is \"$subjectPrefix\"")
                .setEphemeral(true)
                .queue()
        } else {
            _logger.info("[$guildId] User changed subject prefix")
            persistence.set(KEY_EMAIL_SUBJECT_PREFIX, value)
            persistence.save()
            event.reply("Set subject prefix to \"$value\"")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun cmdSubjectDefault(event: SlashCommandInteractionEvent) {
        val value = event.getOption("subject", OptionMapping::getAsString)
        val guildId = event.guild!!.id
        val persistence = Persistence(guildId)
        if(value == null) {
            val defaultSubject = persistence.get(KEY_EMAIL_SUBJECT_DEFAULT) ?: MailBlasterProperties.emailDefaultSubject
            event.reply("Current default subject is \"${defaultSubject}\"")
                .setEphemeral(true)
                .queue()
        } else {
            _logger.info("[$guildId] User changed default subject")
            persistence.set(KEY_EMAIL_SUBJECT_DEFAULT, value)
            persistence.save()
            event.reply("Set default subject to \"$value\"")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun cmdSetup(event: SlashCommandInteractionEvent) {
        val emailAddress = event.getOption("email", OptionMapping::getAsString)!!
        val password = event.getOption("password", OptionMapping::getAsString)!!
        val guildId = event.guild!!.id

        val domain = emailAddress.split("@").last().lowercase()
        val (hostName, smtpPort, sslEnabled) = when(domain) {
            "gmail.com" -> EmailSettings("smtp.gmail.com", 25, true)
            else -> EmailSettings("", -1, false)
        }

        if(smtpPort == -1) {
            _logger.info("[$guildId] User tried to setup email with $domain, but the domain was not recognized")
            event.reply("Unrecognized domain \"$domain\". Try /setupadvanced instead.")
                .setEphemeral(true)
                .queue()
            return
        }

        _logger.info("[$guildId] User setup a custom email")
        setup(guildId, emailAddress, password, hostName, smtpPort, sslEnabled)

        event.reply("Setup email successfully. Run /mailtest to check if mail can be sent.")
            .setEphemeral(true)
            .queue()
    }

    private fun cmdSetupAdvanced(event: SlashCommandInteractionEvent) {
        val emailAddress = event.getOption("email", OptionMapping::getAsString)!!
        val password = event.getOption("password", OptionMapping::getAsString)!!
        val hostName = event.getOption("hostname", OptionMapping::getAsString)!!
        val smtpPort = event.getOption("port", OptionMapping::getAsInt)!!
        val sslEnabled = event.getOption("ssl", OptionMapping::getAsBoolean)!!
        val guildId = event.guild!!.id

        _logger.info("[$guildId] User setup a custom email using advanced setup")
        setup(guildId, emailAddress, password, hostName, smtpPort, sslEnabled)

        event.reply("Setup email successfully. Run /mailtest to check if mail can be sent")
            .setEphemeral(true)
            .queue()
    }

    private fun setup(guildId: String, user: String, password: String, hostName: String, port: Int, ssl: Boolean) {
        val persistence = Persistence(guildId)
        persistence.set(KEY_EMAIL_USER, user)
        val crypt = Crypt(MailBlasterProperties.secret)
        persistence.set(KEY_EMAIL_PASS, crypt.encrypt(password))
        persistence.set(KEY_EMAIL_HOST, hostName)
        persistence.set(KEY_EMAIL_PORT, port.toString())
        persistence.set(KEY_EMAIL_SSL, ssl.toString())
        persistence.save()
    }

    private fun cmdSetupClear(event: SlashCommandInteractionEvent) {
        val guildId = event.guild!!.id
        val persistence = Persistence(guildId)
        persistence.delete(KEY_EMAIL_USER)
        persistence.delete(KEY_EMAIL_PASS)
        persistence.delete(KEY_EMAIL_HOST)
        persistence.delete(KEY_EMAIL_PORT)
        persistence.delete(KEY_EMAIL_SSL)
        persistence.save()

        event.reply("Successfully cleared custom email configuration")
            .setEphemeral(true)
            .queue()
    }

    private fun cmdVerify(event: SlashCommandInteractionEvent) {
        val code = event.getOption("code", OptionMapping::getAsInt)!!

        if(_verifyActions.contains(code)) {
            val result = _verifyActions[code]!!.run(code)
            event.reply(result)
                .setEphemeral(true)
                .queue()
        } else {
            event.reply("Invalid verification code")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun cmdInfo(event: SlashCommandInteractionEvent) {
        val unset = "`unset`"
        val persistence = Persistence(event.guild!!.id)
        val channelId = persistence.get(KEY_DISCORD_CHANNEL)
        val channel = if(channelId.isNullOrBlank()) unset else "<#$channelId>"
        val email = persistence.get(KEY_EMAIL_USER) ?: unset
        val subjectPrefix = persistence.get(KEY_EMAIL_SUBJECT_PREFIX) ?: MailBlasterProperties.emailSubjectPrefix
        val subjectDefault = persistence.get(KEY_EMAIL_SUBJECT_DEFAULT) ?: MailBlasterProperties.emailDefaultSubject
        val subject = "$subjectPrefix $subjectDefault"

        event.reply(
            "Announcements channel: $channel\n" +
                    "Email Account: $email\n" +
                    "Default Subject: $subject"
        )
            .setEphemeral(true)
            .queue()
    }

    private fun cleanupActions() {
        val now = Instant.now()
        val expiredCodes = mutableListOf<Int>()
        for(action in _verifyActions.values) {
            if(now.isAfter(action.expirationTime)) {
                expiredCodes.add(action.expectedCode)
            }
        }

        expiredCodes.forEach { _verifyActions.remove(it) }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        cleanupActions()

        when(event.name) {
            "setchannel" -> cmdSetChannel(event)
            "maillist" -> cmdEmailList(event)
            "mailadd" -> cmdEmailAdd(event)
            "mailremove" -> cmdEmailRemove(event)
            "mailtest" -> cmdEmailTest(event)
            "subjectprefix" -> cmdSubjectPrefix(event)
            "subjectdefault" -> cmdSubjectDefault(event)
            "setup" -> cmdSetup(event)
            "setupadvanced" -> cmdSetupAdvanced(event)
            "setupclear" -> cmdSetupClear(event)
            "verify" -> cmdVerify(event)
            "info" -> cmdInfo(event)
        }
    }
}