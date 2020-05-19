package com.handydev.financisto.utils;

import android.content.Context;

import com.handydev.financisto.R;

import static com.handydev.financisto.utils.AndroidUtils.isInstalledOnSdCard;

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
