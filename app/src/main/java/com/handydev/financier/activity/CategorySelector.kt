/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.handydev.financier.activity

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.handydev.financier.R
import com.handydev.financier.activity.ActivityLayout.FilterNode
import com.handydev.financier.db.CategoriesCache
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.model.*
import com.handydev.financier.utils.ArrUtils
import com.handydev.financier.utils.TransactionUtils
import com.handydev.financier.utils.Utils
import com.handydev.financier.view.AttributeView
import com.handydev.financier.view.AttributeViewFactory
import java.util.*

class CategorySelector<A : AbstractActivity?> @JvmOverloads constructor(private val activity: A, private val db: DatabaseAdapter, private val x: ActivityLayout, private val excludingSubTreeId: Long = -1, private val darkUI: Boolean = false) {
    private var filterNode: FilterNode? = null
    private var categoryText: TextView? = null
    private var autoCompleteTextView: AutoCompleteTextView? = null
    private var autoCompleteAdapter: SimpleCursorAdapter? = null
    private var categoryCursor: Cursor? = null
    private var categoryAdapter: ListAdapter? = null
    private var attributesLayout: LinearLayout? = null
    var selectedCategoryId = Category.NO_CATEGORY_ID
        private set
    private var listener: CategorySelectorListener? = null
    private var showSplitCategory = true
    private var multiSelect = false
    private var useMultiChoicePlainSelector = false
    var categories: List<Category> = emptyList()
        private set
    var emptyResId = 0
    private var initAutocomplete = true
    fun setListener(listener: CategorySelectorListener?) {
        this.listener = listener
    }

    fun doNotShowSplitCategory() {
        showSplitCategory = false
    }

    fun initMultiSelect() {
        multiSelect = true
        categories = db.getCategoriesList(true)
        doNotShowSplitCategory()
    }

    fun setUseMultiChoicePlainSelector() {
        useMultiChoicePlainSelector = true
    }

    val checkedTitles: String
        get() = MyEntitySelector.getCheckedTitles(categories)
    val checkedIdsAsStr: String
        get() = MyEntitySelector.getCheckedIdsAsStr(categories)
    val checkedCategoryIds: Array<String>
        get() = MyEntitySelector.getCheckedIds(categories)

    // special case as it must include only itself
    val checkedCategoryLeafs: Array<String>
        get() {
            val res = LinkedList<String>()
            for (c in categories) {
                if (c.checked) {
                    if (c.id == Category.NO_CATEGORY_ID) { // special case as it must include only itself
                        res.add("0")
                        res.add("0")
                    } else {
                        res.add(c.left.toString())
                        res.add(c.right.toString())
                    }
                }
            }
            return ArrUtils.strListToArr(res)
        }

    fun fetchCategories(fetchAll: Boolean) {
        if (!multiSelect) {
            categoryCursor = if (fetchAll) {
                db.allCategories
            } else {
                if (excludingSubTreeId > 0) {
                    db.getCategoriesWithoutSubtree(excludingSubTreeId, true)
                } else {
                    db.getCategories(true)
                }
            }
            activity!!.startManagingCursor(categoryCursor)
            categoryAdapter = TransactionUtils.createCategoryAdapter(db, activity, categoryCursor)
        }
    }

    fun createNode(layout: LinearLayout?, type: SelectorType): TextView? {
        when (type) {
            SelectorType.TRANSACTION -> {
                if (emptyResId <= 0) emptyResId = R.string.no_category
                filterNode = x.addCategoryNodeForTransaction(layout, emptyResId, true)
            }
            SelectorType.PARENT, SelectorType.SPLIT, SelectorType.TRANSFER -> {
                if (emptyResId <= 0) emptyResId = R.string.no_category
                filterNode = x.addCategoryNodeForTransfer(layout, emptyResId)
            }
            SelectorType.FILTER -> {
                if (emptyResId <= 0) emptyResId = R.string.no_filter
                filterNode = x.addCategoryNodeForFilter(layout, emptyResId)
            }
        }
        categoryText = filterNode?.textView
        autoCompleteTextView = filterNode?.autoCompleteTextView
        if(darkUI) {
            categoryText?.setTextColor(activity!!.resources.getColor(R.color.main_text_color, null))
            autoCompleteTextView?.setTextColor(activity!!.resources.getColor(R.color.main_text_color, null))
        }
        return categoryText
    }

    private fun initAutoCompleteFilter() {
        if (initAutocomplete) {
            autoCompleteAdapter = TransactionUtils.createCategoryFilterAdapter(activity, db)
            autoCompleteTextView!!.inputType = (InputType.TYPE_CLASS_TEXT
                    or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    or InputType.TYPE_TEXT_VARIATION_FILTER)
            autoCompleteTextView!!.threshold = 1
            autoCompleteTextView!!.onFocusChangeListener = OnFocusChangeListener { view: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    autoCompleteTextView!!.setAdapter<SimpleCursorAdapter>(Objects.requireNonNull(autoCompleteAdapter))
                    autoCompleteTextView!!.selectAll()
                }
            }
            autoCompleteTextView!!.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long -> activity!!.onSelectedId(R.id.category, id) }
            initAutocomplete = false
        }
    }

    fun createDummyNode() {
        categoryText = EditText(activity)
    }

    fun onClick(id: Int) {
        when (id) {
            R.id.category -> if (isListPick) {
                pickCategory()
            } else {
                showFilter()
            }
            R.id.category_show_list -> pickCategory()
            R.id.category_add -> addCategory()
            R.id.category_split -> selectCategory(Category.SPLIT_CATEGORY_ID)
            R.id.category_clear -> clearCategory()
            R.id.category_show_filter -> showFilter()
            R.id.category_close_filter -> filterNode!!.hideFilter()
        }
    }

    private fun showFilter() {
        initAutoCompleteFilter()
        filterNode!!.showFilter()
    }

    private val isListPick: Boolean
        private get() = false

    private fun addCategory() {
        val intent = Intent(activity, CategoryActivity::class.java)
        activity!!.startActivityForResult(intent, CategorySelectorActivity.CATEGORY_ADD)
    }

    private fun pickCategory() {
        if (isMultiSelect()) {
            x.selectMultiChoice(activity, R.id.category, R.string.categories, categories)
        } else if (!CategorySelectorActivity.pickCategory(activity, multiSelect, selectedCategoryId, excludingSubTreeId, showSplitCategory)) {
            x.select(activity!!, R.id.category, R.string.category, categoryCursor!!, categoryAdapter!!,
                    DatabaseHelper.CategoryViewColumns._id.name, selectedCategoryId)
        }
    }

    private fun clearCategory() {
        categoryText!!.setText(emptyResId)
        selectedCategoryId = Category.NO_CATEGORY_ID
        for (e in categories) e.isChecked = false
        showHideMinusBtn(false)
        if (listener != null) listener!!.onCategorySelected(Category.noCategory(), false)
    }

    @JvmOverloads
    fun onSelectedId(id: Int, selectedId: Long, selectLast: Boolean = true) {
        if (id == R.id.category) {
            selectCategory(selectedId, selectLast)
        }
    }

    fun onSelected(id: Int, ignore: List<MultiChoiceItem?>?) {
        if (id == R.id.category) fillCategoryInUI()
    }

    fun fillCategoryInUI() {
        val selected = checkedTitles
        if (Utils.isEmpty(selected)) {
            clearCategory()
        } else {
            categoryText!!.text = selected
            showHideMinusBtn(true)
        }
        if (filterNode != null) {
            filterNode!!.hideFilter()
        }
    }

    @JvmOverloads
    fun selectCategory(categoryId: Long, selectLast: Boolean = true) {
        if (multiSelect) {
            updateCheckedEntities("" + categoryId)
            selectedCategoryId = categoryId
            fillCategoryInUI()
            if (listener != null) listener!!.onCategorySelected(null, false)
        } else {
            if (selectedCategoryId != categoryId) {
                db.updateCategoriesCache(false)
                val category = CategoriesCache.getCategory(categoryId)
                if (category != null) {
                    categoryText!!.text = category.getNestedTitle() //Category.getTitle(category.title, category.level)
                    showHideMinusBtn(true)
                }
                selectedCategoryId = categoryId
                if (listener != null) listener!!.onCategorySelected(category, selectLast)
            }
            if (filterNode != null) {
                filterNode!!.hideFilter()
            }
        }
    }

    fun updateCheckedEntities(checkedCommaIds: String) {
        MyEntitySelector.updateCheckedEntities(categories, checkedCommaIds)
    }

    fun updateCheckedEntities(checkedIds: Array<String>) {
        MyEntitySelector.updateCheckedEntities(categories, checkedIds)
    }

    fun updateCheckedEntities(checkedIds: List<Long>) {
        for (id in checkedIds) {
            for (e in categories) {
                if (e.id == id) {
                    e.checked = true
                    break
                }
            }
        }
    }

    private fun showHideMinusBtn(show: Boolean) {
        val minusBtn = categoryText!!.getTag(R.id.bMinus) as ImageView
        if (minusBtn != null) minusBtn.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun createAttributesLayout(layout: LinearLayout) {
        attributesLayout = LinearLayout(activity)
        attributesLayout!!.orientation = LinearLayout.VERTICAL
        layout.addView(attributesLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    val attributes: MutableList<TransactionAttribute>
        get() {
            val list: MutableList<TransactionAttribute> = LinkedList()
            val count = attributesLayout!!.childCount.toLong()
            for (i in 0 until count) {
                val v = attributesLayout!!.getChildAt(i.toInt())
                val o = v.tag
                if (o is AttributeView) {
                    val ta = o.newTransactionAttribute()
                    list.add(ta)
                }
            }
            return list
        }

    fun addAttributes(transaction: Transaction) {
        attributesLayout!!.removeAllViews()
        val attributes = db.getAllAttributesForCategory(selectedCategoryId)
        val values = transaction.categoryAttributes
        for (a in attributes) {
            val av = inflateAttribute(a)
            var value = values?.get(a.id)
            if (value == null) {
                value = a.defaultValue
            }
            val v = av.inflateView(attributesLayout, value)
            v.tag = av
        }
    }

    private fun inflateAttribute(attribute: Attribute): AttributeView {
        return AttributeViewFactory.createViewForAttribute(activity, attribute)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CategorySelectorActivity.CATEGORY_ADD -> {
                    categoryCursor!!.requery()
                    val categoryId = data?.getLongExtra(DatabaseHelper.CategoryColumns._id.name, -1)
                    if (categoryId != null && categoryId != -1L) {
                        selectCategory(categoryId)
                    }
                }
                CategorySelectorActivity.CATEGORY_PICK -> {
                    val categoryId = data?.getLongExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, 0)
                    if(categoryId != null) {
                        selectCategory(categoryId)
                    }
                }
            }
        }
    }

    val isSplitCategorySelected: Boolean
        get() = Category.isSplit(selectedCategoryId)

    @Deprecated("") // todo.mb: it seems not much sense in it, better do it in single place - activity.onSelectedId
    interface CategorySelectorListener {
        @Deprecated("")
        fun onCategorySelected(category: Category?, selectLast: Boolean)
    }

    fun isMultiSelect(): Boolean {
        return multiSelect || useMultiChoicePlainSelector
    }

    fun onDestroy() {
        if (autoCompleteAdapter != null) {
            autoCompleteAdapter!!.changeCursor(null)
            autoCompleteAdapter = null
        }
    }

    enum class SelectorType {
        TRANSACTION, SPLIT, TRANSFER, FILTER, PARENT
    }
}