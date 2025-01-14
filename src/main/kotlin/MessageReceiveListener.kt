package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory

class MessageReceiveListener(private val _mailingList: MailingList) : ListenerAdapter() {
    companion object {
        private val _logger = LoggerFactory.getLogger(MessageReceiveListener::class.java)
    }

    fun setChannelId(guildId: String, channelId: String) {
        _logger.debug("[$guildId] Setting channel to $channelId")
        val persistence = Persistence(guildId)
        persistence.set(KEY_DISCORD_CHANNEL, channelId)
        persistence.save()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val persistence = Persistence(event.guild.id)
        val channelId = persistence.get(KEY_DISCORD_CHANNEL)
        if(channelId.isNullOrBlank()) return
        if(event.channel.id != channelId) return

        try {
            _logger.info("Preparing email...")

            val rawContent = event.message.contentRaw
            val messageBody = event.message.contentDisplay

            val subjectPrefix = persistence.get(KEY_EMAIL_SUBJECT_PREFIX) ?: MailBlasterProperties.emailSubjectPrefix
            val defaultSubject = persistence.get(KEY_EMAIL_SUBJECT_DEFAULT) ?: MailBlasterProperties.emailDefaultSubject

            val subject = if (rawContent.startsWith("#")) {
                val subjectLine = rawContent.lines().first().replace("#", "").trim()
                "$subjectPrefix $subjectLine"
            } else {
                "$subjectPrefix $defaultSubject"
            }

            _logger.debug("Adding emails to email session...")
            val session = EmailSession(event.guild.id, subject, messageBody)
            for (email in _mailingList.loadAllEmails(event.guild.id)) {
                session.addTo(email)
            }
            _logger.debug("Emails added to session")
            session.sendHTML()

            event.message.addReaction(Emoji.fromUnicode("U+1F4E7")).queue()

            _logger.info("Email sent")
        } catch(exception: Exception) {
            _logger.error("Failed to send email", exception)
            event.message.addReaction(Emoji.fromUnicode("U+274C")).queue()
        }
    }
}