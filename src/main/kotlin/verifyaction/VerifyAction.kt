package com.github.tacticallaptopbag.mail_blaster.verifyaction

import java.time.Instant
import java.time.temporal.ChronoUnit

abstract class VerifyAction {
    abstract val expectedCode: Int
    val expirationTime: Instant = Instant.now().plus(15, ChronoUnit.MINUTES)

    abstract fun run(code: Int): String
}