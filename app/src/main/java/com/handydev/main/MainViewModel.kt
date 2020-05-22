package com.handydev.main

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.handydev.financisto.R
import com.handydev.financisto.utils.MyPreferences
import com.handydev.main.fragments.*


class MainViewModelFactory(val activity: FragmentActivity, val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(activity, context) as T
    }
}

class MainViewModel(activity: FragmentActivity, val context: Context): ViewModel() {
    var currentTab = MutableLiveData(0)
    var bottomNavigationSelectedItem = MutableLiveData(0)

    class MainPagerAdapter(activity: FragmentActivity, val context: Context) : FragmentStateAdapter(activity)
    {
        override fun getItemCount(): Int {
            return 5
        }

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> {
                    return AccountsFragment()
                }
                1 -> {
                    val blotter = BlotterFragment()
                    val bundle = Bundle()
                    bundle.putBoolean(BlotterFragment.SAVE_FILTER, true)
                    blotter.arguments = bundle
                    return blotter
                }
                2 -> {
                    return BudgetListFragment()
                }
                3 -> {
                    return ReportsListFragment()
                }
                4 -> {
                    return MenuListFragment()
                }
            }
            throw Exception("wrong tab!")
        }
    }

    fun onNavigationClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_page_accounts -> {
                currentTab.postValue(0)
                return true
            }
            R.id.main_page_blotter -> {
                currentTab.postValue(1)
                return true
            }
            R.id.main_page_budgets -> {
                currentTab.postValue(2)
                return true
            }
            R.id.main_page_reports -> {
                currentTab.postValue(3)
                return true
            }
            R.id.main_page_menu -> {
                currentTab.postValue(4)
                return true
            }
        }
        return false
    }

    var tabPageAdapter: MainPagerAdapter

    init {
        tabPageAdapter = MainPagerAdapter(activity, context)
        when (MyPreferences.getStartupScreen(context)) {
            MyPreferences.StartupScreen.ACCOUNTS -> { }
            MyPreferences.StartupScreen.BLOTTER -> {
                currentTab.value = 1
                bottomNavigationSelectedItem.value = R.id.main_page_blotter
            }
            MyPreferences.StartupScreen.BUDGETS -> {
                currentTab.value = 2
                bottomNavigationSelectedItem.value = R.id.main_page_budgets
            }
            MyPreferences.StartupScreen.REPORTS -> {
                currentTab.value = 3
                bottomNavigationSelectedItem.value = R.id.main_page_reports
            }
        }
    }
}