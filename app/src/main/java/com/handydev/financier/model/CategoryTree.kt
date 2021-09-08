/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 * Denis Solonenko - initial API and implementation
 */
package com.handydev.financier.model

import android.database.Cursor
import android.util.Log
import com.handydev.financier.db.CategoriesCache.categoriesList
import java.util.*
import kotlin.collections.ArrayList

class CategoryTree<T : CategoryEntity<T>> : Iterable<T> {
    private val roots: ArrayList<T>

    constructor(roots: ArrayList<T>) {
        this.roots = roots
    }

    constructor() {
        roots = ArrayList()
    }

    fun insertAtTop(category: T) {
        roots.add(0, category)
    }

    interface NodeCreator<T> {
        fun createNode(c: Cursor?): T
    }

    fun asMap(): Map<Long, T> {
        val map: MutableMap<Long, T> = HashMap()
        initializeMap(map, this)
        return map
    }

    private fun initializeMap(map: MutableMap<Long, T>, tree: CategoryTree<T>) {
        for (c in tree) {
            map[c.id] = c
            if (c.children != null) {
                initializeMap(map, c.children!!)
            }
        }
    }

    override fun iterator(): MutableIterator<T> {
        return roots.iterator()
    }

    val isEmpty: Boolean
        get() = roots.isEmpty()

    fun add(child: T) {
        roots.add(child)
    }

    fun remove(category: T) {
        roots.remove(category)
    }

    fun indexOf(child: T): Int {
        return roots.indexOf(child)
    }

    fun size(): Int {
        return roots.size
    }

    fun getAt(pos: Int): T {
        return roots[pos]
    }

    fun getRoots(): List<T> {
        return roots
    }

    fun moveCategoryUp(pos: Int): Boolean {
        if (pos > 0 && pos < size()) {
            swap(pos, pos - 1)
            return true
        }
        return false
    }

    fun moveCategoryDown(pos: Int): Boolean {
        if (pos >= 0 && pos < size() - 1) {
            swap(pos, pos + 1)
            return true
        }
        return false
    }

    fun moveCategoryToTheTop(pos: Int): Boolean {
        if (pos > 0 && pos < size()) {
            val node = roots.removeAt(pos)
            roots.add(0, node)
            reIndex()
            return true
        }
        return false
    }

    fun moveCategoryToTheBottom(pos: Int): Boolean {
        if (pos >= 0 && pos < size() - 1) {
            val node = roots.removeAt(pos)
            roots.add(size(), node)
            reIndex()
            return true
        }
        return false
    }

    fun sortByTitle(): Boolean {
        sortByTitle(this)
        reIndex()
        return true
    }

    private val byTitleComparator = Comparator<T> { c1, c2 ->
        var t1 = c1!!.title
        var t2 = c2!!.title
        if (t1 == null) {
            t1 = ""
        }
        if (t2 == null) {
            t2 = ""
        }
        t1.compareTo(t2)
    }

    private fun sortByTitle(tree: CategoryTree<T>) {
        Collections.sort(tree.roots, byTitleComparator)
        for (node in tree) {
            if (node.children != null) {
                sortByTitle(node.children!!)
            }
        }
    }

    private fun swap(from: Int, to: Int) {
        val fromNode = roots[from]
        val toNode = roots.set(to, fromNode)
        roots[from] = toNode
        reIndex()
    }

    fun reIndex() {
        var left = Int.MAX_VALUE
        for (node in roots) {
            if (node.left < left) {
                left = node.left
            }
        }
        reIndex(this, left)
    }

    private fun reIndex(tree: CategoryTree<T>, left: Int): Int {
        var left = left
        for (node in tree.roots) {
            node.left = left
            if (node.children != null) {
                node.right = reIndex(node.children!!, left + 1)
            } else {
                node.right = left + 1
            }
            left = node.right + 1
        }
        return left
    }

    companion object {
        @JvmStatic
		fun <T : CategoryEntity<T>> createFromCursor(c: Cursor, creator: NodeCreator<T>): CategoryTree<T> {
            val roots = ArrayList<T>()
            var parent: T? = null
            while (c.moveToNext()) {
                val category = creator.createNode(c)
                while (parent != null) {
                    parent = if (category.left > parent.left && category.right < parent.right) {
                        parent.addChild(category, false)
                        break
                    } else {
                        parent.parent
                    }
                }
                if (parent == null) {
                    roots.add(category)
                }
                if (category.id > 0 && category.right - category.left > 1) {
                    parent = category
                }
            }
            return CategoryTree(roots)
        }

        fun createFromCache(categories: List<Category>): CategoryTree<Category> {
            val roots = ArrayList<Category>()
            val addedIds = ArrayList<Long>()
            fun addCategoryIfNeeded(category: Category, parent: Category?) {
                if(!addedIds.contains(category.id)) {
                    addedIds.add(category.id)
                    if(parent == null) {
                        roots.add(category)
                    } else {
                        parent.addChild(category)
                    }
                }
            }
            fun parseCategoryRecursive(category: Category?, parent: Category?, level: Int) {
                if(category == null) {
                    return
                }
                if(category.title == Category.noCategory().title) {
                    return
                }
                addCategoryIfNeeded(category, parent)
                var children = categoriesList.filter { it.left >= category.left && it.right <= category.right && it.id != category.id }
                for(child in children) {
                    parseCategoryRecursive(child, category, level + 1)
                }
            }
            for (category in categories) {
                parseCategoryRecursive(category, null, 0)
            }
            return CategoryTree(roots)
        }
    }
}