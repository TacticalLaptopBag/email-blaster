package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import kotlin.io.path.*

const val CHANNEL_PROP_NAME = "mail-blaster.discord.announcement-channel-id"

class MessageReceiveListener(private val _mailingList: MailingList) : ListenerAdapter() {
    private val _channelPath = Dirs.dataPath.resolve("persistence.properties")
    private val _channelProps = Properties()

    var announcementChannelId: String? = null
        set(value) {
            field = value
            _channelProps.setProperty(CHANNEL_PROP_NAME, value)
            if(_channelPath.notExists()) {
                _channelPath.createParentDirectories()
                _channelPath.createFile()
            }
            _channelPath.outputStream().use { stream ->
                _channelProps.store(stream, null)
            }
        }

    init {
        if(_channelPath.isRegularFile()) {
            _channelPath.toFile().inputStream().use { stream ->
                _channelProps.load(stream)
                announcementChannelId = _channelProps.getProperty(CHANNEL_PROP_NAME)
            }
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(announcementChannelId.isNullOrBlank()) return
        if(event.channel.id != announcementChannelId) return

        val rawContent = event.message.contentRaw
        val messageBody = event.message.contentDisplay

        val subject = if(rawContent.startsWith("#")) {
            MailBlasterProperties.emailSubjectPrefix + rawContent.lines().first()
        } else {
            MailBlasterProperties.emailSubjectPrefix + MailBlasterProperties.emailDefaultSubject
        }

        val session = EmailSession(subject, messageBody)
        for(email in _mailingList.loadAllEmails(event.guild.id)) {
            session.addTo(email)
        }
        session.send()

        event.message.addReaction(Emoji.fromFormatted("�")).queue()
    }
}