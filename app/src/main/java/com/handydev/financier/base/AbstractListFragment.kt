package com.handydev.financier.base

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.fragment.app.ListFragment
import com.handydev.financier.R
import com.handydev.financier.activity.RefreshSupportedActivity
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.utils.MenuItemInfo
import com.handydev.financier.utils.PinProtection
import com.handydev.financier.protocol.IOnBackPressed
import kotlin.collections.ArrayList

abstract class AbstractListFragment protected constructor(private val contentId: Int) : ListFragment(), IOnBackPressed, RefreshSupportedActivity {
    protected open var inflater: LayoutInflater? = null
    protected var cursor: Cursor? = null
    protected var adapter: ListAdapter? = null
    protected var db: DatabaseAdapter? = null
    protected var bAdd: ImageButton? = null
    protected var enablePin = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(contentId, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        db = DatabaseAdapter(activity!!)
        db!!.open()
        inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        internalOnCreate(savedInstanceState)
        cursor = createCursor()
        recreateAdapter()
        listView.onItemLongClickListener = OnItemLongClickListener { _: AdapterView<*>?, view: View?, position: Int, id: Long ->
            val popupMenu = PopupMenu(activity!!, view)
            val menu = popupMenu.menu
            val menus = createContextMenus(id)
            var i = 0
            for (m in menus) {
                if (m.enabled) {
                    menu.add(0, m.menuId, i++, m.titleId)
                }
            }
            popupMenu.setOnMenuItemClickListener { item: MenuItem -> onPopupItemSelected(item.itemId, view, position, id) }
            popupMenu.show()
            true
        }
    }

     override fun onBackPressed(): Boolean {
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cursor?.close()
        cursor = null
    }

    private fun recreateAdapter() {
        adapter = createAdapter(cursor)
        listAdapter = adapter
    }

    protected abstract fun createCursor(): Cursor?
    protected abstract fun createAdapter(cursor: Cursor?): ListAdapter?
    open fun internalOnCreate(savedInstanceState: Bundle?) {
        bAdd = view?.findViewById(R.id.bAdd)
        bAdd?.setOnClickListener(View.OnClickListener { arg0: View? -> addItem() })
    }

    override fun onDestroy() {
        db!!.close()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if(activity != null) {
            if (enablePin) PinProtection.lock(activity!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            if (enablePin) PinProtection.unlock(activity!!)
        }
    }

    protected open fun createContextMenus(id: Long): ArrayList<MenuItemInfo> {
        val menus = ArrayList<MenuItemInfo>()
        menus.add(MenuItemInfo(MENU_VIEW, R.string.view))
        menus.add(MenuItemInfo(MENU_EDIT, R.string.edit))
        menus.add(MenuItemInfo(MENU_DELETE, R.string.delete))
        return menus
    }

    open fun onPopupItemSelected(itemId: Int, view: View?, position: Int, id: Long): Boolean {
        when (itemId) {
            MENU_VIEW -> {
                viewItem(view, position, id)
                return true
            }
            MENU_EDIT -> {
                editItem(view, position, id)
                return true
            }
            MENU_DELETE -> {
                deleteItem(view, position, id)
                return true
            }
        }
        return false
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        onItemClick(v, position, id)
    }

    protected open fun onItemClick(v: View?, position: Int, id: Long) {
        viewItem(v, position, id)
    }

    protected open fun addItem() {}
    protected abstract fun deleteItem(v: View?, position: Int, id: Long)
    protected abstract fun editItem(v: View?, position: Int, id: Long)
    protected abstract fun viewItem(v: View?, position: Int, id: Long)
    override fun recreateCursor() {
        Log.i("AbstractListActivity", "Recreating cursor")
        val state = listView.onSaveInstanceState()
        try {
            if (cursor != null) {
                cursor!!.close()
            }
            cursor = createCursor()
            if (cursor != null) {
                recreateAdapter()
            }
        } finally {
            listView.onRestoreInstanceState(state)
        }
    }

    override fun integrityCheck() {}

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            recreateCursor()
        }
    }

    */
    companion object {
        const val MENU_VIEW = Menu.FIRST + 1
        const val MENU_EDIT = Menu.FIRST + 2
        const val MENU_DELETE = Menu.FIRST + 3
        @JvmStatic val MENU_ADD = Menu.FIRST + 4
    }
}
