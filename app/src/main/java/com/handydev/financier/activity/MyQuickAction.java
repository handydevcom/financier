package com.handydev.financier.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;

import greendroid.widget.QuickAction;
import com.handydev.financier.R;

public class MyQuickAction extends QuickAction {

    public MyQuickAction(Context ctx, int drawableId, int titleId) {
        super(ctx, buildDrawable(ctx, drawableId), titleId);
    }

    private static Drawable buildDrawable(Context ctx, int drawableId) {
        Drawable d = ctx.getResources().getDrawable(drawableId).mutate();
        d.setColorFilter(new LightingColorFilter(Color.BLACK, ctx.getResources().getColor(R.color.colorPrimary)));
        return d;
    }

}
