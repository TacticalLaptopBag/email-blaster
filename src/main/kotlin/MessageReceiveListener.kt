package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.util.Properties
import kotlin.io.path.*

class MessageReceiveListener(private val _mailingList: MailingList) : ListenerAdapter() {
    private val _channelPath = Dirs.dataPath.resolve("channels.properties")
    private val _channelProps = Properties()

    companion object {
        private val _logger = LoggerFactory.getLogger(MessageReceiveListener::class.java)
    }

    init {
        if(_channelPath.isRegularFile()) {
            _channelPath.toFile().inputStream().use { stream ->
                _channelProps.load(stream)
                _logger.debug("Loaded announcement channel IDs from file")
            }
        }
    }

    private fun getPropertyName(guildId: String): String {
        return "mail-blaster.discord.channel.$guildId"
    }

    private fun saveChannelIds() {
        _channelPath.outputStream().use { stream ->
            _channelProps.store(stream, null)
            _logger.debug("Saved channel IDs")
        }
    }

    fun setChannelId(guildId: String, channelId: String) {
        _logger.debug("Setting channel for $guildId to $channelId")
        _channelProps.setProperty(getPropertyName(guildId), channelId)
        saveChannelIds()
    }

    fun deleteChannelId(guildId: String): Boolean {
        _logger.debug("Deleting channel for $guildId")
        val success = _channelProps.remove(guildId) != null
        saveChannelIds()
        return success
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channelId = _channelProps.getProperty(getPropertyName(event.guild.id))
        _logger.debug("Message received in ${event.channel.id}, announcement channel is $channelId")
        if(channelId.isNullOrBlank()) return
        if(event.channel.id != channelId) return

        _logger.info("Preparing email...")

        val rawContent = event.message.contentRaw
        val messageBody = event.message.contentDisplay

        val subject = if(rawContent.startsWith("#")) {
            val subjectLine = rawContent.lines().first().replace("#", "").trim()
            MailBlasterProperties.emailSubjectPrefix + subjectLine
        } else {
            MailBlasterProperties.emailSubjectPrefix + MailBlasterProperties.emailDefaultSubject
        }

        _logger.debug("Adding emails to email session...")
        val session = EmailSession(subject, messageBody)
        for(email in _mailingList.loadAllEmails(event.guild.id)) {
            session.addTo(email)
        }
        _logger.debug("Emails added to session")
        session.sendHTML()

        event.message.addReaction(Emoji.fromUnicode("U+1F4E7")).queue()

        _logger.info("Email sent")
    }
}