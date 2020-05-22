package com.handydev.financier.utils;

import android.content.Context;

import com.handydev.financier.R;

import static com.handydev.financier.utils.AndroidUtils.isInstalledOnSdCard;

public class IntegrityCheckInstalledOnSdCard implements IntegrityCheck {

    private final Context context;

    public IntegrityCheckInstalledOnSdCard(Context context) {
        this.context = context;
    }

    @Override
    public Result check() {
        if (isInstalledOnSdCard(context)) {
            return new Result(Level.WARN, context.getString(R.string.installed_on_sd_card_warning));
        }
        return Result.OK;
    }

}
