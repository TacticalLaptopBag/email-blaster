package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import kotlin.io.path.deleteExisting
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

fun cleanup(predicate: (guildId: Long) -> Boolean) {
    for(filePath in Dirs.dataPath.listDirectoryEntries()) {
        if(!filePath.isRegularFile()) return
        if(!filePath.endsWith(".txt") && !filePath.endsWith(".properties")) return

        val guildIdString = filePath.nameWithoutExtension.substringBefore("-", "-1")
        guildIdString.toLongOrNull()?.let { guildId ->
            if(predicate(guildId)) {
                filePath.deleteExisting()
            }
        }
    }
}

fun main() {
    val logger = LoggerFactory.getLogger("Main")

    val mailingList = MailingList()

    val messageListener = MessageReceiveListener(mailingList)
    val commandListener = SlashCommandListener(messageListener, mailingList)
    val leaveListener = LeaveGuildListener()

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
            leaveListener,
        )
        .build()
        .awaitReady()
    logger.info("JDA is ready")

    val commands = listOf(
        Commands.slash("setchannel", "Sets the announcement channel to listen to")
            .setGuildOnly(true)
            .addOption(OptionType.CHANNEL, "channel", "The announcement channel", true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash("maillist", "Lists all emails on the mailing list")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash("mailadd", "Adds an email to the mailing list")
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "email", "The email to add to the mailing list", true)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
        Commands.slash("mailremove", "Removes an email from the mailing list")
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "email", "The email to remove from the mailing list", true)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
        Commands.slash("mailtest", "Tests whether the setup run by /setup or /setupadvanced is functioning correctly")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash(
            "subjectprefix",
            "Sets the subject prefix to use in emails.",
        )
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "prefix", "The prefix to use in emails")
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash(
            "subjectdefault",
            "Sets the default subject to use if the first line in an announcement message doesn't start with a #"
        )
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "subject", "The default subject to use if none is provided by a message")
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash(
            "setup",
            "Sets email to come from a different email account. WARNING: **PLEASE BE CAREFUL WITH THIS!**"
        )
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "email", "The email address messages will come from", true)
            .addOption(OptionType.STRING, "password", "The password of the email account", true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash(
            "setupadvanced",
            "Sets email to come from a different email account, with settings for unrecognized email domains."
        )
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "email", "The email address messages will come from", true)
            .addOption(OptionType.STRING, "password", "The password of the email account", true)
            .addOption(OptionType.STRING, "hostname", "The server address of the email service (e.g. smtp.gmail.com)", true)
            .addOption(OptionType.INTEGER, "port", "The port for email traffic (e.g. 25)", true)
            .addOption(OptionType.BOOLEAN, "ssl", "Whether or not the email service uses SSL", true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash("setupclear", "Clears the email configuration and uses the default email")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
        Commands.slash("verify", "Verifies that you are in control of an email to perform an action on it")
            .setGuildOnly(true)
            .addOption(OptionType.INTEGER, "code", "The verification code received in your email", true)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
    )

    jda.getGuildById(1189030160905809961)
        ?.updateCommands()
        ?.addCommands(commands)
        ?.queue()

    jda.updateCommands()
        .addCommands(commands)
        .queue()
    logger.info("Sent commands")

    // The bot could have been removed from a guild while offline.
    // We don't want to continue storing emails and configs for irrelevant guilds
    val guilds = jda.guilds.map { it.idLong }.toSet()
    cleanup { !guilds.contains(it) }
}
