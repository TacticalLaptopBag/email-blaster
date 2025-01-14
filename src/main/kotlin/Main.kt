package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands

fun main() {
    val mailingList = MailingList()

    val messageListener = MessageReceiveListener(mailingList)
    val commandListener = SlashCommandListener(messageListener, mailingList)

    val jda = JDABuilder.createLight(
        MailBlasterProperties.discordToken,
        emptyList()
    )
        .addEventListeners(
            messageListener,
            commandListener,
        )
        .build()
        .awaitReady()


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
}
