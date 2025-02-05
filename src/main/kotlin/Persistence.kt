package com.github.tacticallaptopbag.email_blaster

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Properties
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isRegularFile

class Persistence(private val _guildId: String) {
    private val _logger = LoggerFactory.getLogger(Persistence::class.java)

    private val _path = Dirs.dataPath.resolve("$_guildId-persistence.properties")
    private val _props = Properties()

    init {
        if(_path.isRegularFile()) {
            _path.toFile().inputStream().use { stream ->
                _logger.debug("[$_guildId] Loaded persistence from file")
                _props.load(stream)
            }
        }
    }

    fun get(key: String, default: String? = null): String? {
        return _props.getProperty(key, default)
    }

    fun set(key: String, value: String) {
        _props.setProperty(key, value)
    }

    fun delete(key: String): Boolean {
        return _props.remove(key) != null
    }

    fun save() {
        _path.createParentDirectories()
        _path.toFile().outputStream().use { stream ->
            _props.store(stream, null)
            _logger.debug("[$_guildId] Saved persistence to file")
        }

        try {
            val perms = PosixFilePermissions.fromString("rw-------")
            Files.setPosixFilePermissions(_path, perms)
        } catch(e: Exception) {
            _logger.warn("Unable to set persistence permissions. Is this running on a POSIX-compliant OS?")
        }
    }
}