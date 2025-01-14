package com.github.tacticallaptopbag.email_blaster

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.Path

object Dirs {
    val dataPath = Path(
        AppDirsFactory.getInstance().getUserDataDir(
            "email-blaster",
            null,
            "tacticallaptopbag"
        )
    )

    fun getListPath(guildId: String): Path {
        return dataPath.resolve("$guildId-mailing-list.txt")
    }
}