package com.handydev.financier.activity;

import android.content.Context;
import android.widget.ImageButton;

import com.handydev.financier.R;
import com.handydev.financier.filter.WhereFilter;

public class FilterState {
    static public void updateFilterColor(Context context, WhereFilter filter, ImageButton button) {
        int color = filter.isEmpty() ? context.getResources().getColor(R.color.bottom_bar_tint) : context.getResources().getColor(R.color.holo_blue_dark);
        button.setColorFilter(color);
    }

}
