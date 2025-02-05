package com.github.tacticallaptopbag.email_blaster

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*

class MailingList {
    private val _logger = LoggerFactory.getLogger(MailingList::class.java)

    private fun confirmPath(path: Path) {
        if(path.notExists()) {
            path.createParentDirectories()
            path.createFile()

            try {
                val perms = PosixFilePermissions.fromString("rw-------")
                Files.setPosixFilePermissions(path, perms)
            } catch(e: Exception) {
                _logger.warn("Unable to set mailing list permissions. Is this running on a POSIX-compliant OS?")
            }
        }
    }

    fun loadAllEmails(guildId: String): Set<String> {
        val listPath = Dirs.getListPath(guildId)
        confirmPath(listPath)
        val emails = mutableSetOf<String>()

        File(listPath.pathString).bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                if(line.isNotBlank()) {
                    emails.add(line.trim().lowercase())
                }
            }
        }

        return emails
    }

    fun add(guildId: String, email: String) {
        val listPath = Dirs.getListPath(guildId)
        confirmPath(listPath)
        File(listPath.pathString).appendText("${email.lowercase()}\n")
    }

    fun remove(guildId: String, email: String): Boolean {
        val listPath = Dirs.getListPath(guildId)
        confirmPath(listPath)
        val emailLower = email.lowercase()
        val tmpFile = createTempFile()
        var lineRemoved = false

        File(listPath.pathString).bufferedReader().use { reader ->
            tmpFile.bufferedWriter().use { writer ->
                reader.lineSequence().forEach { line ->
                    if(!line.lowercase().contains(emailLower)) {
                        writer.write(line)
                        writer.newLine()
                    } else {
                        lineRemoved = true
                    }
                }
            }
        }

        if(lineRemoved) {
            tmpFile.moveTo(listPath, overwrite=true)
        }

        return lineRemoved
    }
}