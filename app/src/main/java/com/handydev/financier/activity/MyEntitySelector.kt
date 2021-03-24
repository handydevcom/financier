package com.handydev.financier.activity

import android.app.Activity
import android.content.Intent
import android.text.InputType
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.handydev.financier.R
import com.handydev.financier.activity.ActivityLayout.FilterNode
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.db.MyEntityManager
import com.handydev.financier.model.MultiChoiceItem
import com.handydev.financier.model.MyEntity
import com.handydev.financier.utils.ArrUtils
import com.handydev.financier.utils.Utils
import java.util.*

abstract class MyEntitySelector<T : MyEntity, A : AbstractActivity?> internal constructor(
        private val entityClass: Class<T>,
        val activity: A,
        protected val em: MyEntityManager,
        private val x: ActivityLayout,
        val isShow: Boolean,
        private val layoutId: Int,
        private val actBtnId: Int,
        private val clearBtnId: Int,
        private val labelResId: Int,
        private val defaultValueResId: Int,
        private val showListId: Int,
        private val closeFilterId: Int,
        private val showFilterId: Int,
        private val requestCode: Int) {
    private var node: View? = null
    private var filterNode: FilterNode? = null
    private var text: TextView? = null
    private var autoCompleteView: AutoCompleteTextView? = null
    var entities: List<T> = emptyList()
        private set
    private var adapter: ListAdapter? = null
    private var isMultiSelect = false
    private var initAutoComplete = true
    private var selectedEntityId: Long = 0
    protected abstract val editActivityClass: Class<*>?
    fun setEntities(entities: List<T>) {
        this.entities = entities
    }

    fun fetchEntities() {
        entities = fetchEntities(em)
        if (!isMultiSelect) {
            adapter = createAdapter(activity, entities)
        }
    }

    protected abstract fun fetchEntities(em: MyEntityManager?): List<T>
    protected abstract fun createAdapter(activity: Activity?, entities: List<T>?): ListAdapter?
    protected abstract fun createFilterAdapter(): ArrayAdapter<T>
    fun createNode(layout: LinearLayout?): TextView? {
        return createNode(layout, false)
    }

    fun createNode(layout: LinearLayout?, darkUI: Boolean): TextView? {
        if (isShow) {
            filterNode = x.addFilterNode(layout, layoutId, if (isMultiSelect) -1 else actBtnId, clearBtnId,
                    labelResId, defaultValueResId, showListId, closeFilterId, showFilterId, darkUI)
            text = filterNode!!.textView
            node = filterNode!!.nodeLayout
            autoCompleteView = filterNode!!.autoCompleteTextView
            if(darkUI) {
                text?.setTextColor(activity!!.resources.getColor(R.color.main_text_color, null))
                autoCompleteView?.setTextColor(activity!!.resources.getColor(R.color.main_text_color, null))
                autoCompleteView?.setHintTextColor(activity!!.resources.getColor(R.color.main_text_color, null))
            }
        }
        return text
    }

    private fun initAutoCompleteFilter() {
        if (initAutoComplete) {
            val filterAdapter = createFilterAdapter()
            autoCompleteView!!.setAdapter(filterAdapter)
            autoCompleteView!!.inputType = (InputType.TYPE_CLASS_TEXT
                    or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    or InputType.TYPE_TEXT_VARIATION_FILTER)
            autoCompleteView!!.threshold = 1
            autoCompleteView!!.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                val e = filterAdapter.getItem(position)
                activity!!.onSelectedId(layoutId, e!!.id)
            }
            initAutoComplete = false
        }
    }

    fun onClick(id: Int) {
        if (id == layoutId) {
            if (isListPick) {
                pickEntity()
            } else {
                showFilter()
            }
        } else if (id == actBtnId) {
            createEntity()
        } else if (id == showListId) {
            pickEntity()
        } else if (id == clearBtnId) {
            clearSelection()
        } else if (id == showFilterId) {
            showFilter()
        } else if (id == closeFilterId) {
            filterNode!!.hideFilter()
        }
    }

    private fun createEntity() {
        val intent = Intent(activity, editActivityClass)
        activity!!.startActivityForResult(intent, requestCode)
    }

    private fun showFilter() {
        initAutoCompleteFilter()
        filterNode!!.showFilter()
    }

    protected val isListPick: Boolean
        protected get() = isListPickConfigured
    protected abstract val isListPickConfigured: Boolean
    private fun clearSelection() {
        selectedEntityId = 0
        if (text != null) {
            text!!.setText(defaultValueResId)
            showHideMinusBtn(false)
        }
        for (e in entities) e!!.isChecked = false
    }

    private fun showHideMinusBtn(show: Boolean) {
        val minusBtn = text!!.getTag(R.id.bMinus) as ImageView
        if (minusBtn != null) minusBtn.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun pickEntity() {
        if (isMultiSelect) {
            x.selectMultiChoice(activity, layoutId, labelResId, entities)
        } else {
            val selectedEntityPos = MyEntity.indexOf(entities, selectedEntityId)
            x.selectPosition(activity!!, layoutId, labelResId, adapter!!, selectedEntityPos)
        }
    }

    fun onSelectedPos(id: Int, selectedPos: Int) {
        if (id == layoutId) onEntitySelected(selectedPos)
    }

    fun onSelectedId(id: Int, selectedId: Long) {
        if (id != layoutId) return
        selectEntity(selectedId)
    }

    fun onSelected(id: Int, ignore: List<MultiChoiceItem?>?) {
        if (id == layoutId) fillCheckedEntitiesInUI()
    }

    fun fillCheckedEntitiesInUI() {
        val selectedEntities = checkedTitles
        if (Utils.isEmpty(selectedEntities)) {
            clearSelection()
        } else {
            text!!.text = selectedEntities
            showHideMinusBtn(true)
        }
        if (filterNode != null) {
            filterNode!!.hideFilter()
        }
    }

    val checkedTitles: String
        get() = getCheckedTitles(entities)
    val checkedIds: Array<String>
        get() = getCheckedIds(entities)
    val checkedIdsAsStr: String
        get() = getCheckedIdsAsStr(entities)

    private fun onEntitySelected(selectedPos: Int) {
        val e = entities[selectedPos]
        selectEntity(e)
    }

    fun selectEntity(entityId: Long) {
        if (isShow) {
            if (isMultiSelect) {
                updateCheckedEntities("" + entityId)
                fillCheckedEntitiesInUI()
            } else {
                val e = MyEntity.find(entities, entityId)
                selectEntity(e)
            }
        }
    }

    fun selectEntity(e: T?) {
        if (isShow) {
            if (e == null) {
                clearSelection()
            } else {
                selectedEntityId = e.id
                if (e.id > 0) {
                    text!!.text = e.title
                    showHideMinusBtn(true)
                } else {
                    text!!.setText(defaultValueResId)
                    showHideMinusBtn(false)
                }
            }
            if (filterNode != null) {
                filterNode!!.hideFilter()
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK && requestCode == actBtnId) {
            onNewEntity(data)
        }
    }

    private fun onNewEntity(data: Intent) {
        fetchEntities()
        val entityId = data.getLongExtra(DatabaseHelper.EntityColumns.ID, -1)
        if (entityId != -1L) {
            selectEntity(entityId)
        }
    }

    fun setNodeVisible(visible: Boolean) {
        if (isShow) {
            AbstractActivity.setVisibility(node, if (visible) View.VISIBLE else View.GONE)
        }
    }

    fun getSelectedEntityId(): Long {
        return if (node == null || node!!.visibility == View.GONE) 0 else selectedEntityId
    }

    fun initMultiSelect() {
        isMultiSelect = true
        fetchEntities()
    }

    fun updateCheckedEntities(checkedCommaIds: String) {
        updateCheckedEntities(entities, checkedCommaIds)
    }

    fun updateCheckedEntities(checkedIds: Array<String>) {
        updateCheckedEntities(entities, checkedIds)
    }

    fun createNewEntity() {
        if (filterNode != null && filterNode!!.isFilterOn && selectedEntityId == 0L) {
            val filterText = autoCompleteView!!.text.toString()
            val e = em.findOrInsertEntityByTitle(entityClass, filterText)
            selectEntity(e)
        }
    }

    companion object {
        const val REQUEST_LOCATION = 3001
        const val REQUEST_PAYEE = 3002
        const val REQUEST_PROJECT = 3003
        fun getCheckedTitles(list: List<MyEntity?>): String {
            val sb = StringBuilder()
            for (e in list) {
                if (e!!.checked) {
                    if (sb.length > 0) {
                        sb.append(", ")
                    }
                    sb.append(e.title)
                }
            }
            return sb.toString()
        }

        fun getCheckedIds(list: List<MyEntity?>): Array<String> {
            val res: MutableList<String> = LinkedList()
            for (e in list) {
                if (e!!.checked) {
                    res.add(e.id.toString())
                }
            }
            return ArrUtils.strListToArr(res)
        }

        fun getCheckedIdsAsStr(list: List<MyEntity>): String {
            val sb = StringBuilder()
            for (e in list) {
                if (e.checked) {
                    if (sb.length > 0) {
                        sb.append(",")
                    }
                    sb.append(e.id)
                }
            }
            return sb.toString()
        }

        fun updateCheckedEntities(list: List<MyEntity>, checkedCommaIds: String) {
            if (!Utils.isEmpty(checkedCommaIds)) {
                updateCheckedEntities(list, checkedCommaIds.split(",".toRegex()).toTypedArray())
            }
        }

        fun updateCheckedEntities(list: List<MyEntity>, checkedIds: Array<String>) {
            for (s in checkedIds) {
                val id = s.toLong()
                for (e in list) {
                    if (e.id == id) {
                        e.checked = true
                        break
                    }
                }
            }
        }
    }
}