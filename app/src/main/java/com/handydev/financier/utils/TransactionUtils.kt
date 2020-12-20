package com.handydev.financier.utils

import android.R
import android.content.Context
import android.database.Cursor
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import com.handydev.financier.adapter.CategoryListAdapter
import com.handydev.financier.adapter.MyEntityAdapter
import com.handydev.financier.db.CategoriesCache
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.db.DatabaseHelper.AccountColumns
import com.handydev.financier.db.MyEntityManager
import com.handydev.financier.model.*

object TransactionUtils {
    @JvmStatic
    fun createAccountAdapter(context: Context?, accountCursor: Cursor?): ListAdapter {
        return SimpleCursorAdapter(context, R.layout.simple_list_item_single_choice, accountCursor, arrayOf("e_" + AccountColumns.TITLE), intArrayOf(R.id.text1))
    }

    fun createAccountMultiChoiceAdapter(context: Context?, accountCursor: Cursor?): ListAdapter {
        return SimpleCursorAdapter(context, R.layout.simple_list_item_multiple_choice, accountCursor, arrayOf("e_" + AccountColumns.TITLE), intArrayOf(R.id.text1))
    }

    @JvmStatic
    fun createCurrencyAdapter(context: Context?, currencyCursor: Cursor?): SimpleCursorAdapter {
        return SimpleCursorAdapter(context, R.layout.simple_list_item_single_choice, currencyCursor, arrayOf("e_name"), intArrayOf(R.id.text1))
    }

    fun createCategoryAdapter(db: DatabaseAdapter?, context: Context?, categoryCursor: Cursor?): ListAdapter {
        return CategoryListAdapter(db, context, R.layout.simple_list_item_single_choice, categoryCursor)
    }

    fun createCategoryMultiChoiceAdapter(db: DatabaseAdapter?, context: Context?, categoryCursor: Cursor?): ListAdapter {
        return CategoryListAdapter(db, context, R.layout.simple_list_item_multiple_choice, categoryCursor)
    }

    @JvmStatic
    fun createProjectAdapter(context: Context?, projects: List<Project>?): ListAdapter {
        return MyEntityAdapter(context, R.layout.simple_list_item_single_choice, R.id.text1, projects)
    }

    @JvmStatic
    fun createLocationAdapter(context: Context?, locations: List<MyLocation>?): ListAdapter {
        return MyEntityAdapter(context, R.layout.simple_list_item_single_choice, R.id.text1, locations)
    }

    @JvmStatic
    fun createPayeeAdapter(context: Context?, payees: List<Payee>?): ListAdapter {
        return MyEntityAdapter(context, R.layout.simple_list_item_single_choice, R.id.text1, payees)
    }

    @JvmStatic
    fun createCurrencyAdapter(context: Context?, currencies: List<Currency>?): ListAdapter {
        return MyEntityAdapter(context, R.layout.simple_list_item_single_choice, R.id.text1, currencies)
    }

    fun createLocationAdapter(context: Context?, cursor: Cursor?): ListAdapter {
        return SimpleCursorAdapter(context, R.layout.simple_list_item_single_choice, cursor, arrayOf("e_name"), intArrayOf(R.id.text1))
    }

    fun createPayeeAutoCompleteAdapter(context: Context?, db: MyEntityManager): SimpleCursorAdapter {
        return object : FilterSimpleCursorAdapter<MyEntityManager?, Payee>(context, db, Payee::class.java) {
            override fun filterRows(constraint: CharSequence): Cursor {
                return db.filterActiveEntities(Payee::class.java, constraint.toString())
            }

            override val allRows: Cursor
                get() = db.filterActiveEntities(Payee::class.java, null)
        }
    }

    fun createProjectAutoCompleteAdapter(context: Context?, db: MyEntityManager): SimpleCursorAdapter {
        return FilterSimpleCursorAdapter(context, db, Project::class.java)
    }

    fun createLocationAutoCompleteAdapter(context: Context?, db: MyEntityManager): SimpleCursorAdapter {
        return object : FilterSimpleCursorAdapter<MyEntityManager?, MyLocation>(context, db, MyLocation::class.java) {
            override fun filterRows(constraint: CharSequence): Cursor {
                return db.filterActiveEntities(MyLocation::class.java, constraint.toString())
            }

            override val allRows: Cursor
                get() = db.filterActiveEntities(MyLocation::class.java, null)
        }
    }

    fun createCategoryFilterAdapter(context: Context?, db: DatabaseAdapter): SimpleCursorAdapter {
        var res = object : FilterSimpleCursorAdapter<DatabaseAdapter?, MyLocation>(context, db, MyLocation::class.java, "title") {
            override val allRows: Cursor
                get() = db.getCategories(false)

            override fun filterRows(constraint: CharSequence): Cursor {
                return db.filterCategories(constraint)
            }
        }
        res.setViewBinder { view, cursor, i ->
            val id = cursor.getLong(DatabaseHelper.CategoryViewColumns._id.ordinal)
            val category = CategoriesCache.getCategory(id)
            (view as? TextView)?.text = category?.getNestedTitle() ?: ""
            true
        }
        return res
    }

    @JvmStatic
    fun payeeFilterAdapter(context: Context, em: MyEntityManager): FilterEntityAdapter<Payee> {
        return FilterEntityAdapter(context, em.allActivePayeeList)
    }

    @JvmStatic
    fun projectFilterAdapter(context: Context, em: MyEntityManager): FilterEntityAdapter<Project> {
        return FilterEntityAdapter(context, em.allActiveProjectsList)
    }

    @JvmStatic
    fun locationFilterAdapter(context: Context, em: MyEntityManager): FilterEntityAdapter<MyLocation> {
        return FilterEntityAdapter(context, em.allActiveLocationsList)
    }

    internal open class FilterSimpleCursorAdapter<T : MyEntityManager?, E : MyEntity?> @JvmOverloads constructor(context: Context?, private val db: T, private val entityClass: Class<E>, private val filterColumn: String = "e_title") : SimpleCursorAdapter(context, R.layout.simple_dropdown_item_1line, null, arrayOf(filterColumn), intArrayOf(R.id.text1)) {
        override fun convertToString(cursor: Cursor): CharSequence {
            return cursor.getString(cursor.getColumnIndex(filterColumn))
        }

        override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor {
            return if (constraint == null || StringUtil.isEmpty(constraint.toString())) {
                allRows
            } else {
                filterRows(constraint)
            }
        }

        open fun filterRows(constraint: CharSequence): Cursor {
            return db!!.filterActiveEntities(entityClass, constraint.toString())
        }

        open val allRows: Cursor
            get() = db!!.filterActiveEntities(entityClass, null)
    }

    class FilterEntityAdapter<E : MyEntity?> internal constructor(context: Context, objects: List<E>) : ArrayAdapter<E>(context, R.layout.simple_dropdown_item_1line, R.id.text1, objects)
}