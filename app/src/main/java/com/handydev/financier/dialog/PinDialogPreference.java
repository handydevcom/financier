package com.handydev.financier.dialog;

import com.handydev.financier.R;
import com.handydev.financier.view.PinView;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

public class PinDialogPreference extends DialogPreference implements PinView.PinListener {

	private Dialog dialog;
	
	public PinDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PinDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    protected void showDialog(Bundle state) {
		Context context = getContext();
		PinView pinView = new PinView(context, this, R.layout.lock);
		dialog = new AlertDialog.Builder(context)
        	.setTitle(R.string.set_pin)
        	.setView(pinView.getView())
        	.create();
		//dialog.setOnDismissListener(this);
		dialog.show();
    }

	@Override
	public void onConfirm(String pinBase64) {
		dialog.setTitle(R.string.confirm_pin);
	}
		
	@Override
	public void onSuccess(String pinBase64) {
		persistString(pinBase64);
		dialog.dismiss();
	}

}
