package com.handydev.financier.fragments

import androidx.preference.PreferenceDialogFragmentCompat
import android.os.Bundle

class FinancierPreferenceDialogFragment: PreferenceDialogFragmentCompat() {
    override fun onDialogClosed(positiveResult: Boolean) {
    }

    companion object {
        fun newInstance(key: String?): FinancierPreferenceDialogFragment? {
            val fragment = FinancierPreferenceDialogFragment()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}