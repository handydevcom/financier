package com.handydev.main.binding

import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

@BindingAdapter("onNavigationItemSelected")
fun setOnNavigationItemSelectedListener(
        view: BottomNavigationView, listener: BottomNavigationView.OnNavigationItemSelectedListener?) {
    view.setOnNavigationItemSelectedListener(listener)
}

@BindingAdapter("selectedItemPosition")
fun setSelectedItemPosition(
        view: BottomNavigationView, position: Int) {
    view.selectedItemId = position
}

@BindingAdapter("currentTab")
fun setNewTab(pager: ViewPager2, newTab: MutableLiveData<Int>) {
    newTab.value?.let {
        //don't forget to break possible infinite loops!
        if (pager.currentItem != newTab.value && newTab.value != null) {
            pager.setCurrentItem(newTab.value!!, true)
        }
    }
}