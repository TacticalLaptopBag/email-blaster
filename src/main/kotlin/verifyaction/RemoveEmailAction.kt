package com.github.tacticallaptopbag.mail_blaster.verifyaction

import com.github.tacticallaptopbag.mail_blaster.MailingList

class RemoveEmailAction(
    override val expectedCode: Int,
    private val email: String,
    private val guildId: String,
    private val mailingList: MailingList,
) : VerifyAction() {
    override fun run(code: Int): String {
        if(expectedCode != code) return "Invalid verification code"

        return if(mailingList.remove(guildId, email)) {
            "Successfully removed email from the mailing list"
        } else {
            "Email is not in the mailing list!"
        }
    }
}