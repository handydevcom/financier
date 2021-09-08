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

import javax.persistence.Column
import javax.persistence.Transient

open class CategoryEntity<T : CategoryEntity<T>> : MyEntity() {
    @JvmField
    @Transient
    var parent: T? = null

    @JvmField
    @Column(name = "left")
    var left = 1

    @JvmField
    @Column(name = "right")
    var right = 2

    @JvmField
    @Column(name = "type")
    var type = TYPE_EXPENSE

    @JvmField
    @Transient
    var children: CategoryTree<T>? = null
    val parentId: Long
        get() = if (parent != null) parent!!.id else 0

    fun addChild(category: T, uniqueOnly: Boolean = true) {
        if (children == null) {
            children = CategoryTree()
        }
        if(uniqueOnly && children?.any { it.id == category.id } == true) {
            return
        }
        category.parent = this as T
        category.type = type
        children!!.add(category)
    }

    fun removeChild(category: T) {
        if (children != null) {
            children?.remove(category)
        }
    }

    fun hasChildren(): Boolean {
        return children != null && !children!!.isEmpty
    }

    val isExpense: Boolean
        get() = type == TYPE_EXPENSE
    val isIncome: Boolean
        get() = type == TYPE_INCOME

    fun makeThisCategoryIncome() {
        type = TYPE_INCOME
    }

    fun makeThisCategoryExpense() {
        type = TYPE_EXPENSE
    }

    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1
    }
}