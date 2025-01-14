package com.github.tacticallaptopbag.email_blaster

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

fun Member.isAdmin(): Boolean {
    return this.isOwner
            || this.hasPermission(Permission.ADMINISTRATOR)
            || this.roles.any { it.hasPermission(Permission.ADMINISTRATOR) }
}
