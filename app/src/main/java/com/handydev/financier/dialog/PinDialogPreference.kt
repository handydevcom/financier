package com.handydev.financier.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import com.handydev.financier.R
import com.handydev.financier.view.PinView
import com.handydev.financier.view.PinView.PinListener

class PinDialogPreference : DialogPreference, PinListener {
    private var dialog: Dialog? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    protected fun showDialog(state: Bundle?) {
        val context = context
        val pinView = PinView(context, this, R.layout.lock)
        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.set_pin)
            .setView(pinView.view)
            .create()
        dialog?.show()
    }

    override fun onConfirm(pinBase64: String) {
        dialog?.setTitle(R.string.confirm_pin)
    }

    override fun onSuccess(pinBase64: String) {
        persistString(pinBase64)
        dialog?.dismiss()
    }
}