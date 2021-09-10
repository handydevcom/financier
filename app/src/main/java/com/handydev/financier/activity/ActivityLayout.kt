package com.handydev.financier.activity

import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.handydev.financier.R
import com.handydev.financier.model.MultiChoiceItem
import com.handydev.financier.utils.Utils
import com.handydev.financier.view.NodeInflater

class ActivityLayout(private val inflater: NodeInflater, private val listener: ActivityLayoutListener) {

    fun getInflater(): NodeInflater {
        return inflater
    }

    fun addTitleNodeNoDivider(layout: LinearLayout?, labelId: Int): View {
        val b = inflater.Builder(layout!!, R.layout.select_entry_title)
        return b.withLabel(labelId).withNoDivider().create()
    }

    fun addTitleNodeNoDivider(layout: LinearLayout?, label: String?): View {
        val b = inflater.Builder(layout!!, R.layout.select_entry_title)
        return b.withLabel(label).withNoDivider().create()
    }

    fun addInfoNodeSingle(layout: LinearLayout?, id: Int, labelId: Int) {
        val b = inflater.Builder(layout!!, R.layout.select_entry_single)
        b.withId(id, listener).withLabel(labelId).create()
    }

    fun addInfoNodeSingle(layout: LinearLayout?, id: Int, label: String?): TextView {
        val b = inflater.Builder(layout!!, R.layout.select_entry_single)
        val v = b.withId(id, listener).withLabel(label).create()
        val labelView = v.findViewById<TextView>(R.id.label)
        labelView.tag = v
        return labelView
    }

    fun addInfoNode(layout: LinearLayout?, id: Int, labelId: Int, defaultValueResId: Int): TextView {
        val b = inflater.Builder(layout!!, R.layout.select_entry_simple)
        val v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create()
        val data = v.findViewById<TextView>(R.id.data)
        data.tag = v
        return data
    }

    fun addInfoNode(layout: LinearLayout?, id: Int, labelId: Int, defaultValue: String?): TextView {
        val b = inflater.Builder(layout!!, R.layout.select_entry_simple)
        val v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create()
        return v.findViewById<View>(R.id.data) as TextView
    }

    fun addListNodeIcon(layout: LinearLayout?, id: Int, labelId: Int, defaultValueResId: Int): View {
        val b = inflater.Builder(layout!!, R.layout.select_entry_icon)
        return b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create()
    }

    fun addListNode(layout: LinearLayout?, id: Int, labelId: Int, defaultValueResId: Int): TextView {
        val b = inflater.Builder(layout!!, R.layout.select_entry)
        val v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create()
        val data = v.findViewById<TextView>(R.id.data)
        data.tag = v
        return data
    }

    fun addListNode(layout: LinearLayout?, id: Int, labelId: Int, defaultValue: String?): TextView {
        val b = inflater.Builder(layout!!, R.layout.select_entry)
        val v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create()
        return v.findViewById<View>(R.id.data) as TextView
    }

    fun addCheckboxNode(layout: LinearLayout?, id: Int, labelId: Int, dataId: Int, checked: Boolean): CheckBox {
        val b = inflater.CheckBoxBuilder(layout!!)
        val v = b.withCheckbox(checked).withLabel(labelId).withId(id, listener).withData(dataId).create()
        return v.findViewById<View>(R.id.checkbox) as CheckBox
    }

    fun addInfoNodePlus(layout: LinearLayout?, id: Int, plusId: Int, labelId: Int) {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_simple_plus)
        b.withButtonId(plusId, listener).withLabel(labelId).withId(id, listener).create()
    }

    fun addListNodePlus(layout: LinearLayout?, id: Int, plusId: Int, labelId: Int, defaultValueResId: Int): TextView {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_plus)
        val v = b.withButtonId(plusId, listener)
                .withId(id, listener)
                .withLabel(labelId)
                .withData(defaultValueResId)
                .create()
        val textView = v.findViewById<TextView>(R.id.data)
        textView.setTag(R.id.bMinus, v.findViewById(plusId))
        textView.tag = v
        return textView
    }

    class FilterNode(val nodeLayout: View, val listLayout: View, val filterLayout: View, val textView: TextView, val autoCompleteTextView: AutoCompleteTextView) {
        fun showFilter(initializeWithSelectedItem: Boolean = false) {
            listLayout.visibility = View.GONE
            filterLayout.visibility = View.VISIBLE
            if(initializeWithSelectedItem) {
                autoCompleteTextView.setText(textView.text)
            }
            Utils.openSoftKeyboard(autoCompleteTextView, autoCompleteTextView.context)
        }

        fun hideFilter() {
            Utils.closeSoftKeyboard(autoCompleteTextView, autoCompleteTextView.context)
            filterLayout.visibility = View.GONE
            listLayout.visibility = View.VISIBLE
        }

        val isFilterOn: Boolean
            get() = filterLayout.visibility == View.VISIBLE
    }

    fun addFilterNode(layout: LinearLayout?, id: Int, actBtnId: Int, clearBtnId: Int, labelId: Int, defaultValueResId: Int, showListId: Int, closeFilterId: Int, showFilterId: Int, darkUI: Boolean = false): FilterNode {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_filter)
        val v = b.withButtonId(actBtnId, listener)
                .withClearButtonId(clearBtnId, listener)
                .withAutoCompleteFilter(listener, showListId)
                .withId(id, listener)
                .withLabel(labelId, darkUI)
                .create()
        val filterTxt = getAutoCompleteTextView(showListId, v)
        filterTxt.setHint(defaultValueResId)
        var filterToggle = v.findViewById<ImageView>(R.id.closeFilter)
        filterToggle.id = closeFilterId
        filterToggle.setOnClickListener(listener)
        filterToggle = v.findViewById(R.id.showFilter)
        filterToggle.id = showFilterId
        filterToggle.setOnClickListener(listener)
        val textView = v.findViewById<TextView>(R.id.data)
        textView.setText(defaultValueResId)
        textView.setTag(R.id.bMinus, v.findViewById(clearBtnId))
        textView.tag = v
        return FilterNode(v, v.findViewById(R.id.list_node_row), v.findViewById(R.id.filter_node_row), textView, filterTxt)
    }

    fun addCategoryNodeForTransaction(layout: LinearLayout?, emptyResId: Int, darkUI: Boolean): FilterNode {
        val filterNode = addFilterNode(layout, R.id.category, R.id.category_add, R.id.category_clear, R.string.category, emptyResId,
                R.id.category_show_list, R.id.category_close_filter, R.id.category_show_filter, darkUI)
        val splitImage = filterNode.nodeLayout.findViewById<ImageView>(R.id.split)
        splitImage.visibility = View.VISIBLE
        splitImage.id = R.id.category_split
        splitImage.setOnClickListener(listener)
        return filterNode
    }

    fun addCategoryNodeForTransfer(layout: LinearLayout?, emptyResId: Int, darkUI: Boolean): FilterNode {
        return addFilterNode(layout, R.id.category, R.id.category_add, R.id.category_clear, R.string.category, emptyResId,
                R.id.category_show_list, R.id.category_close_filter, R.id.category_show_filter, darkUI)
    }

    fun addCategoryNodeForFilter(layout: LinearLayout?, emptyResId: Int, darkUI: Boolean): FilterNode {
        return addFilterNode(layout, R.id.category, -1, R.id.category_clear, R.string.category, emptyResId,
                R.id.category_show_list, R.id.category_close_filter, R.id.category_show_filter, darkUI)
    }

    private fun getAutoCompleteTextView(showListId: Int, v: View): AutoCompleteTextView {
        val filterTxt = v.findViewById<AutoCompleteTextView>(R.id.autocomplete_filter)
        val showList = v.findViewById<View>(showListId)
        filterTxt.setTag(R.id.list, showList)
        return filterTxt
    }

    fun addNodeUnsplit(layout: LinearLayout?): View {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_unsplit)
        val v = b.withButtonId(R.id.add_split, listener).withId(R.id.unsplit_action, listener).withLabel(R.string.unsplit_amount).withData("0").create()
        val transferImageView = v.findViewById<ImageView>(R.id.add_split_transfer)
        transferImageView.setOnClickListener(listener)
        return v
    }

    fun addSplitNodeMinus(layout: LinearLayout?, id: Int, minusId: Int, labelId: Int, defaultValue: String?): View {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_minus)
        return b.withButtonId(minusId, listener).withoutMoreButton().withId(id, listener).withLabel(labelId).withData(defaultValue).create()
    }

    @JvmOverloads
    fun addFilterNodeMinus(layout: LinearLayout?, id: Int, minusId: Int, labelId: Int, defaultValueResId: Int, defaultValue: String? = null): TextView {
        val b = inflater.ListBuilder(layout!!, R.layout.select_entry_minus).withButtonId(minusId, listener).withId(id, listener).withLabel(labelId)
        if (defaultValue != null) {
            b.withData(defaultValue)
        } else {
            b.withData(defaultValueResId)
        }
        val v = b.create()
        val clearBtn = hideButton(v, minusId)
        val text = v.findViewById<TextView>(R.id.data)
        text.setTag(R.id.bMinus, clearBtn) // needed for dynamic toggling in any activity with filters
        return text
    }

    private fun hideButton(v: View, btnId: Int): ImageView {
        val plusImageView = v.findViewById<ImageView>(btnId)
        plusImageView.visibility = View.GONE
        return plusImageView
    }

    fun addPictureNodeMinus(context: Context?, layout: LinearLayout?, id: Int, minusId: Int, labelId: Int, defaultLabelResId: Int): ImageView {
        val b = inflater.PictureBuilder(layout!!)
        val v = b.withPicture(context!!, null).withButtonId(minusId, listener).withId(id, listener)
                .withLabel(labelId).withData(defaultLabelResId).create()
        return v.findViewById<View>(R.id.picture) as ImageView
    }

    fun addEditNode(layout: LinearLayout?, labelId: Int, view: View?): View {
        val b = inflater.EditBuilder(layout!!, view)
        return b.withLabel(labelId).create()
    }

    private fun selectSingleChoice(context: Context, titleId: Int, adapter: ListAdapter, checkedItem: Int,
                                   onClickListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(context)
                .setSingleChoiceItems(adapter, checkedItem, onClickListener)
                .setTitle(titleId)
                .show()
    }

    fun selectMultiChoice(context: Context?, id: Int, titleId: Int, items: List<MultiChoiceItem?>) {
        val count = items.size
        val titles = arrayOfNulls<String>(count)
        val checked = BooleanArray(count)
        for (i in 0 until count) {
            titles[i] = items[i]!!.title
            checked[i] = items[i]!!.isChecked
        }
        AlertDialog.Builder(context!!)
                .setMultiChoiceItems(titles, checked) { dialog: DialogInterface?, which: Int, isChecked: Boolean -> items[which]!!.isChecked = isChecked }
                .setPositiveButton(R.string.ok) { dialog: DialogInterface?, which: Int -> listener.onSelected(id, items) }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, which: Int -> }
                .setTitle(titleId)
                .show()
    }

    fun selectPosition(context: Context, id: Int, titleId: Int,
                       adapter: ListAdapter, selectedPosition: Int) {
        selectSingleChoice(context, titleId, adapter, selectedPosition
        ) { dialog: DialogInterface, which: Int ->
            dialog.cancel()
            listener.onSelectedPos(id, which)
        }
    }

    fun selectItemId(context: Context, id: Int, titleId: Int,
                     adapter: ListAdapter, selectedPosition: Int) {
        selectSingleChoice(context, titleId, adapter, selectedPosition
        ) { dialog: DialogInterface, which: Int ->
            dialog.cancel()
            val selectedId = adapter.getItemId(which)
            listener.onSelectedId(id, selectedId)
        }
    }

    fun select(context: Context, id: Int, titleId: Int,
               cursor: Cursor, adapter: ListAdapter,
               idColumn: String?, valueId: Long) {
        val pos = Utils.moveCursor(cursor, idColumn, valueId)
        selectSingleChoice(context, titleId, adapter, pos
        ) { dialog: DialogInterface, which: Int ->
            dialog.cancel()
            cursor.moveToPosition(which)
            val selectedId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn))
            listener.onSelectedId(id, selectedId)
        }
    }

    fun addRateNode(layout: LinearLayout?): View {
        return inflater.Builder(layout!!, R.layout.select_entry_rate)
                .withLabel(R.string.rate)
                .withData(R.string.no_rate)
                .create()
    }
}