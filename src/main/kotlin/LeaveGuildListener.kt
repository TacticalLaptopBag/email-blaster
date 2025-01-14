package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import kotlin.io.path.deleteIfExists

class LeaveGuildListener(private val _messageListener: MessageReceiveListener) : ListenerAdapter() {
    companion object {
        private val _logger = LoggerFactory.getLogger(LeaveGuildListener::class.java)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        _logger.info("Bot left guild \"${event.guild.name}\" (${event.guild.id}). Cleaning up...")

        _messageListener.deleteChannelId(event.guild.id)

        val listPath = Dirs.getListPath(event.guild.id)
        listPath.deleteIfExists()
    }
}