package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import org.slf4j.LoggerFactory

class SlashCommandListener(
    private val _messageListener: MessageReceiveListener,
    private val _mailingList: MailingList
) : ListenerAdapter() {

    // https://emailregex.com/
    private val _emailRegex = """(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".toRegex()

    companion object {
        private val _logger = LoggerFactory.getLogger(SlashCommandListener::class.java)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when(event.name) {
            "setchannel" -> {
                val channel = event.getOption("channel", OptionMapping::getAsChannel)

                if(channel == null) {
                    _logger.info("User failed to provide announcement channel")

                    event.reply("You must supply a channel!")
                        .setEphemeral(true)
                        .queue()
                } else if(channel.type != ChannelType.TEXT) {
                    _logger.info("User failed to provide a text channel")

                    event.reply("You must supply a text channel!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    _logger.info("Setting announcement channel to ${channel.id}")
                    _messageListener.setChannelId(event.guild!!.id, channel.id)
                    event.reply("Successfully set announcement channel to <#${channel.id}>")
                        .queue()
                }
            }
            "addemail" -> {
                val email = event.getOption("email", OptionMapping::getAsString)
                if(email == null) {
                    _logger.info("User failed to provide an email to add")
                    event.reply("You must supply an email to add!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    if(email.matches(_emailRegex)) {
                        _logger.info("Adding email to the mailing list")
                        _mailingList.add(event.guild!!.id, email)
                        event.reply("Added \"$email\" to the mailing list.")
                            .setEphemeral(true)
                            .queue()
                    } else {
                        _logger.info("User tried to add an invalid email")
                        event.reply("The supplied email is in an incorrect format!")
                            .setEphemeral(true)
                            .queue()
                    }
                }
            }
            "removeemail" -> {
                val email = event.getOption("email", OptionMapping::getAsString)
                if(email == null) {
                    _logger.info("User failed to provide an email to remove")
                    event.reply("You must supply an email to remove!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    if(_mailingList.remove(event.guild!!.id, email)) {
                        _logger.info("User removed an email from the mailing list")
                        event.reply("Successfully removed \"$email\" from the mailing list.")
                            .setEphemeral(true)
                            .queue()
                    } else {
                        _logger.info("User tried to remove an email from the mailing list, but it was not there.")
                        event.reply("Could not find the given email in the mailing list!")
                            .setEphemeral(true)
                            .queue()
                    }
                }
            }
        }
    }
}