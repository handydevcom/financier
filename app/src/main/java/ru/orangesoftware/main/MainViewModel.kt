package ru.orangesoftware.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.orangesoftware.financisto.R
import ru.orangesoftware.main.fragments.*
import java.lang.Exception


class MainViewModelFactory(val fm: FragmentManager, val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(fm, context) as T
    }
}

class MainViewModel(fm: FragmentManager, val context: Context): ViewModel() {
    class MainPagerAdapter(fm: FragmentManager, behavior: Int, val context: Context) : FragmentPagerAdapter(fm, behavior) {
        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> {
                    return AccountsFragment()
                }
                1 -> {
                    return BlotterFragment()
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

        override fun getCount(): Int {
            return 5
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when(position) {
                0 -> {
                    return context.getString(R.string.accounts)
                }
                1 -> {
                    return context.getString(R.string.blotter)
                }
                2 -> {
                    return context.getString(R.string.budgets)
                }
                3 -> {
                    return context.getString(R.string.reports)
                }
                4 -> {
                    return context.getString(R.string.menu)
                }
            }
            return super.getPageTitle(position)
        }
    }
    var tabPageAdapter: FragmentPagerAdapter

    init {
        tabPageAdapter = MainPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, context)
    }
}