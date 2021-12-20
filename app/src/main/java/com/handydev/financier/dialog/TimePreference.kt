package com.handydev.financier.dialog

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.handydev.financier.R

class TimePreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs) {

    // Save preference
    fun persistMinutes(minutesFromMidnight: Int) {
        super.persistInt(minutesFromMidnight)
        notifyChanged()
    }

    private fun getPersistedMinutes(): Int {
        return super.getPersistedInt(DEFAULT_VALUE)
    }

    fun updateSummary() {
        summary = context.getString(R.string.auto_backup_time_summary, getHour(), getMinute())
    }

    fun getHour(): Int {
        return getPersistedMinutes()/100
    }

    fun getMinute(): Int {
        val hm = getPersistedMinutes()
        val h = hm / 100
        return hm - 100 * h
    }

    // Mostly for default values
    companion object {
        // By default we want notification to appear at 9 AM each time.
        private const val DEFAULT_VALUE = 600
    }
}