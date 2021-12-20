package com.handydev.financier.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat
import com.handydev.financier.datetime.DateUtils

class TimePreferenceDialog: PreferenceDialogFragmentCompat() {
    lateinit var timepicker: TimePicker

    override fun onCreateDialogView(context: Context?): View {
        timepicker = TimePicker(context)
        return timepicker
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        val timePreference = preference as TimePreference
        timepicker.setIs24HourView(DateUtils.is24HourFormat(activity))
        timepicker.hour = timePreference.getHour()
        timepicker.minute = timePreference.getMinute()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // Save settings
        if(positiveResult) {
            val minutes = (timepicker.hour * 100) + timepicker.minute
            (preference as? TimePreference)?.persistMinutes(minutes)
            (preference as? TimePreference)?.updateSummary()
        }
    }

    companion object {
        fun newInstance(key: String): TimePreferenceDialog {
            val fragment = TimePreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}