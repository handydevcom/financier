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
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper.CategoryViewColumns
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Transient

@Entity
@Table(name = "category")
class Category : CategoryEntity<Category?> {
    @JvmField
    @Column(name = "last_location_id")
    var lastLocationId: Long = 0

    @JvmField
    @Column(name = "last_project_id")
    var lastProjectId: Long = 0

    @JvmField
    @Transient
    var level = 0

    @JvmField
    @Transient
    var attributes: List<Attribute>? = null

    @JvmField
    @Transient
    var tag: String? = null

    constructor() {}
    constructor(id: Long) {
        this.id = id
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        sb.append("id=").append(id)
        sb.append(",parentId=").append(parentId)
        sb.append(",title=").append(title)
        sb.append(",level=").append(level)
        sb.append(",left=").append(left)
        sb.append(",right=").append(right)
        sb.append(",type=").append(type)
        sb.append("]")
        return sb.toString()
    }

    override fun getTitle(): String {
        return getTitle(title, level)
    }

    fun copyTypeFromParent() {
        if (parent != null) {
            type = parent!!.type
        }
    }

    fun getTitle(db: DatabaseAdapter): String {
        var fullTitle = title
        var currentParentId = parentId
        while(currentParentId != 0L) {
            val currentCat = db.getCategoryWithParent(currentParentId)
            fullTitle = currentCat.title + " / " + fullTitle
            currentParentId = currentCat.parentId
        }
        return fullTitle
    }

    val isSplit: Boolean
        get() = id == SPLIT_CATEGORY_ID

    companion object {
        @JvmStatic
        fun noCategory(): Category {
            val category = Category()
            category.id = NO_CATEGORY_ID
            category.left = 1
            category.right = 2
            category.title = "<NO_CATEGORY>"
            return category
        }

        @JvmStatic
        fun splitCategory(): Category {
            val category = Category()
            category.id = SPLIT_CATEGORY_ID
            category.right = 0
            category.left = category.right
            category.title = "<SPLIT_CATEGORY>"
            return category
        }

        const val NO_CATEGORY_ID: Long = 0
        const val SPLIT_CATEGORY_ID: Long = -1
        fun isSplit(categoryId: Long): Boolean {
            return SPLIT_CATEGORY_ID == categoryId
        }

        @JvmStatic
        fun getTitle(title: String, level: Int): String {
            val span = getTitleSpan(level)
            return span + title
        }

        @JvmStatic
        fun getTitleSpan(level: Int): String {
            var level = level
            level -= 1
            return when {
                level <= 0 -> {
                    ""
                }
                level == 1 -> {
                    "-- "
                }
                level == 2 -> {
                    "---- "
                }
                level == 3 -> {
                    "------ "
                }
                else -> {
                    val sb = StringBuilder()
                    for (i in 1 until level) {
                    }
                    sb.toString()
                }
            }
        }

        @JvmStatic
        fun formCursor(c: Cursor): Category {
            val id = c.getLong(CategoryViewColumns._id.ordinal)
            val cat = Category()
            cat.id = id
            cat.title = c.getString(CategoryViewColumns.title.ordinal)
            cat.level = c.getInt(CategoryViewColumns.level.ordinal)
            cat.left = c.getInt(CategoryViewColumns.left.ordinal)
            cat.right = c.getInt(CategoryViewColumns.right.ordinal)
            cat.type = c.getInt(CategoryViewColumns.type.ordinal)
            cat.lastLocationId = c.getInt(CategoryViewColumns.last_location_id.ordinal).toLong()
            cat.lastProjectId = c.getInt(CategoryViewColumns.last_project_id.ordinal).toLong()
            return cat
        }
    }
}