package com.github.tacticallaptopbag.mail_blaster.verifyaction

import com.github.tacticallaptopbag.mail_blaster.MailingList

class AddEmailAction(
    override val expectedCode: Int,
    private val email: String,
    private val guildId: String,
    private val mailingList: MailingList,
) : VerifyAction() {
    override fun run(code: Int): String {
        if(expectedCode != code) return "Invalid verification code"

        mailingList.add(guildId, email)
        return "Successfully added email to the mailing list"
    }
}