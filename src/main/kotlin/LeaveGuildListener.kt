package com.github.tacticallaptopbag.mail_blaster

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory

class LeaveGuildListener : ListenerAdapter() {
    companion object {
        private val _logger = LoggerFactory.getLogger(LeaveGuildListener::class.java)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        _logger.info("Bot left guild \"${event.guild.name}\" (${event.guild.id}). Cleaning up...")

        cleanup { it == event.guild.idLong }
    }
}