package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Main")

    val mailingList = MailingList()

    val messageListener = MessageReceiveListener(mailingList)
    val commandListener = SlashCommandListener(messageListener, mailingList)

    logger.info("Initializing JDA...")
    val jda = JDABuilder.createLight(
        MailBlasterProperties.discordToken,
        listOf(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
        )
    )
        .addEventListeners(
            messageListener,
            commandListener,
        )
        .build()
        .awaitReady()
    logger.info("JDA is ready")


    jda.updateCommands()
        .addCommands(
            Commands.slash("setchannel", "Sets the announcement channel to listen to")
                .setGuildOnly(true)
                .addOption(OptionType.CHANNEL, "channel", "The announcement channel", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
            Commands.slash("addemail", "Adds an email to the mailing list")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "email", "The email to add to the mailing list", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
            Commands.slash("removeemail", "Removes an email from the mailing list")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "email", "The email to remove from the mailing list", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        )
        .queue()
    logger.info("Sent commands")
}
