package com.cash.guide.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cash.guide.R
import com.cash.guide.data.db.ContactEntity

object ContactActionHelper {

    fun normalizeMoroccanPhoneForWhatsApp(raw: String): String {
        val digits = raw.filter { it.isDigit() || it == '+' }
        val clean = digits.removePrefix("+").removePrefix("00")
        return when {
            clean.startsWith("06") || clean.startsWith("07") -> "212" + clean.substring(1)
            (clean.startsWith("6") || clean.startsWith("7")) && clean.length == 9 -> "212"
            clean.startsWith("212") -> clean
            else -> clean
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String? = null) {
        val normalized = normalizeMoroccanPhoneForWhatsApp(phoneNumber)
        if (normalized.isBlank()) {
            Toast.makeText(context, context.getString(R.string.contact_invalid_phone), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val url = buildString {
                append("https://api.whatsapp.com/send?phone=").append(normalized)
                if (!message.isNullOrBlank()) {
                    append("&text=").append(Uri.encode(message))
                }
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.contact_whatsapp_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    fun dialPhone(context: Context, phoneNumber: String) {
        val clean = phoneNumber.filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
        if (clean.isBlank()) {
            Toast.makeText(context, context.getString(R.string.contact_invalid_phone), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.contact_dialer_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareContact(context: Context, contact: ContactEntity) {
        try {
            val content = buildString {
                append(contact.name)
                append("\n📱 ").append(contact.phoneNumber)
                contact.secondaryPhone?.let {
                    append("\n📞 ").append(it)
                }
                contact.note?.let {
                    append("\n📝 ").append(it)
                }
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, contact.name).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
