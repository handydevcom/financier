package com.handydev.financier.utils

import com.handydev.financier.model.Category.Companion.isSplit

object TransactionTitleUtils {
    @JvmStatic
    fun generateTransactionTitle(sb: StringBuilder, payee: String?, note: String?, location: String?, categoryId: Long, category: String): String {
        return if (isSplit(categoryId)) {
            generateTransactionTitleForSplit(sb, payee, note, location, category)
        } else {
            generateTransactionTitleForRegular(sb, payee, note, location, category)
        }
    }

    private fun generateTransactionTitleForRegular(sb: StringBuilder, payee: String?, note: String?, location: String?, category: String): String {
        val secondPart = joinAdditionalFields(sb, payee, note, location)
        return if (Utils.isNotEmpty(category)) {
            if (Utils.isNotEmpty(secondPart)) {
                sb.append(category).append(" (").append(secondPart).append(")")
                sb.toString()
            } else {
                category
            }
        } else {
            secondPart
        }
    }

    private fun joinAdditionalFields(sb: StringBuilder, payee: String?, note: String?, location: String?): String {
        sb.setLength(0)
        append(sb, payee ?: "")
        append(sb, location ?: "")
        append(sb, note ?: "")
        val secondPart = sb.toString()
        sb.setLength(0)
        return secondPart
    }

    private fun generateTransactionTitleForSplit(sb: StringBuilder, payee: String?, note: String?, location: String?, category: String): String {
        val secondPart = joinAdditionalFields(sb, note, location)
        return if (Utils.isNotEmpty(payee)) {
            if (Utils.isNotEmpty(secondPart)) {
                sb.append("[").append(payee).append("...] ").append(secondPart).toString()
            } else sb.append("[").append(payee).append("...]").toString()
        } else {
            if (Utils.isNotEmpty(secondPart)) {
                sb.append("[...] ").append(secondPart).toString()
            } else category
        }
    }

    private fun joinAdditionalFields(sb: StringBuilder, note: String?, location: String?): String {
        sb.setLength(0)
        append(sb, location ?: "")
        append(sb, note ?: "")
        val secondPart = sb.toString()
        sb.setLength(0)
        return secondPart
    }

    private fun append(sb: StringBuilder, s: String) {
        if (Utils.isNotEmpty(s)) {
            if (sb.isNotEmpty()) {
                sb.append(": ")
            }
            sb.append(s)
        }
    }
}