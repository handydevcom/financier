package com.handydev.financier

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.fragments.*
import com.handydev.main.fragments.MenuListFragment


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

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
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
                    bundle.putBoolean(BlotterFragment.SHOW_TITLE, false)
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

    fun navigateDirectional(left: Boolean) {
        if(bottomNavigationSelectedItem.value == null) {
            return
        }
        var activePage: Int = currentTab.value!!
        activePage += (if (left) -1 else 1)
        if(activePage < 0 || activePage > 4) {
            return
        }
        currentTab.postValue(activePage)
        updateBottomNavigationPosition(activePage)
    }

    var tabPageAdapter: MainPagerAdapter = MainPagerAdapter(activity, context)

    private fun updateBottomNavigationPosition(page: Int) {
        when(page) {
            0 -> bottomNavigationSelectedItem.value = R.id.main_page_accounts
            1 -> bottomNavigationSelectedItem.value = R.id.main_page_blotter
            2 -> bottomNavigationSelectedItem.value = R.id.main_page_budgets
            3 -> bottomNavigationSelectedItem.value = R.id.main_page_reports
            4 -> bottomNavigationSelectedItem.value = R.id.main_page_menu
        }
    }

    init {
        when (MyPreferences.getStartupScreen(context)) {
            MyPreferences.StartupScreen.ACCOUNTS -> { }
            MyPreferences.StartupScreen.BLOTTER -> {
                currentTab.value = 1
                updateBottomNavigationPosition(1)
            }
            MyPreferences.StartupScreen.BUDGETS -> {
                currentTab.value = 2
                updateBottomNavigationPosition(2)
            }
            MyPreferences.StartupScreen.REPORTS -> {
                currentTab.value = 3
                updateBottomNavigationPosition(3)
            }
            null -> { }
        }
    }
}