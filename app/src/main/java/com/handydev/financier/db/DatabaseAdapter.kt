/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Abdsandryk - implement getAllExpenses method for bill filtering
 */
package com.handydev.financier.db

import com.handydev.financier.model.Category.Companion.formCursor
import com.handydev.financier.model.Category.Companion.noCategory
import com.handydev.financier.model.Category.Companion.splitCategory
import org.androidannotations.annotations.EBean
import com.handydev.financier.db.MyEntityManager
import android.database.sqlite.SQLiteDatabase
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper.TransactionAttributeColumns
import com.handydev.financier.db.DatabaseHelper.TransactionColumns
import com.handydev.financier.filter.WhereFilter
import com.handydev.financier.db.DatabaseHelper.BlotterColumns
import com.handydev.financier.blotter.BlotterFilter
import kotlin.jvm.JvmOverloads
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.util.Log
import com.handydev.financier.db.DatabaseHelper.CategoryAttributeColumns
import com.handydev.financier.db.DatabaseHelper.CategoryViewColumns
import com.handydev.financier.db.DatabaseHelper.CategoryColumns
import com.handydev.financier.utils.ArrUtils
import com.handydev.financier.model.CategoryTree.NodeCreator
import com.handydev.financier.db.DatabaseHelper.SmsTemplateColumns
import com.handydev.financier.db.DatabaseHelper.SmsTemplateListColumns
import com.handydev.financier.db.DatabaseHelper.AttributeColumns
import com.handydev.financier.db.DatabaseHelper.AttributeViewColumns
import com.handydev.financier.db.DatabaseHelper.CreditCardClosingDateColumns
import com.handydev.financier.db.TransactionsTotalCalculator
import com.handydev.financier.db.DatabaseHelper.AccountColumns
import com.handydev.financier.rates.ExchangeRate
import com.handydev.financier.db.DatabaseHelper.ExchangeRateColumns
import com.handydev.financier.rates.ExchangeRateProvider
import com.handydev.financier.rates.LatestExchangeRates
import com.handydev.financier.rates.HistoryExchangeRates
import com.handydev.financier.rates.ExchangeRatesCollection
import com.handydev.financier.R
import com.handydev.financier.datetime.DateUtils
import com.handydev.financier.filter.Criteria
import com.handydev.financier.model.*
import com.handydev.financier.model.Currency
import com.handydev.financier.utils.StringUtil
import java.lang.Exception
import java.lang.StringBuilder
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

@EBean(scope = EBean.Scope.Singleton)
open class DatabaseAdapter(context: Context?) : MyEntityManager(context) {
    private var updateAccountBalance = true
    fun open() {}
    fun close() {}
    fun deleteAccount(id: Long): Int {
        val db = db()
        db.beginTransaction()
        return try {
            val sid = arrayOf(id.toString())
            db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_1, sid)
            db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_2, sid)
            db.delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
                    + " in (SELECT _id from " + DatabaseHelper.TRANSACTION_TABLE + " where " + TransactionColumns.from_account_id + "=?)", sid)
            db.delete(DatabaseHelper.TRANSACTION_TABLE, TransactionColumns.from_account_id.toString() + "=?", sid)
            val count = db.delete(DatabaseHelper.ACCOUNT_TABLE, "_id=?", sid)
            db.setTransactionSuccessful()
            count
        } finally {
            db.endTransaction()
        }
    }

    // ===================================================================
    // TRANSACTION
    // ===================================================================
    fun getTransaction(id: Long): Transaction {
        val t = get(Transaction::class.java, id)
        if (t != null) {
            t.systemAttributes = getSystemAttributesForTransaction(id)
            if (t.isSplitParent) {
                t.splits = getSplitsForTransaction(t.id)
            }
            return t
        }
        return Transaction()
    }

    fun getBlotter(filter: WhereFilter): Cursor {
        val view = if (filter.isEmpty) DatabaseHelper.V_BLOTTER else DatabaseHelper.V_BLOTTER_FLAT_SPLITS
        return getBlotter(view, filter)
    }

    fun getBlotterForAccount(filter: WhereFilter?): Cursor {
        val accountFilter = enhanceFilterForAccountBlotter(filter)
        return getBlotter(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, accountFilter)
    }

    fun getBlotterForAccountWithSplits(filter: WhereFilter): Cursor {
        return getBlotter(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, filter)
    }

    private fun getBlotter(view: String, filter: WhereFilter): Cursor {
        val t0 = System.currentTimeMillis()
        return try {
            val sortOrder = getBlotterSortOrder(filter)
            db().query(view, BlotterColumns.NORMAL_PROJECTION,
                    filter.selection, filter.selectionArgs, null, null,
                    sortOrder)
        } finally {
            val t1 = System.currentTimeMillis()
            Log.i("DB", "getBlotter " + (t1 - t0) + "ms")
        }
    }

    private fun getBlotterSortOrder(filter: WhereFilter): String {
        var sortOrder = filter.sortOrder
        if (sortOrder == null || sortOrder.length == 0) {
            sortOrder = BlotterFilter.SORT_NEWER_TO_OLDER + "," + BlotterFilter.SORT_NEWER_TO_OLDER_BY_ID
        } else {
            sortOrder += if (sortOrder.contains(BlotterFilter.SORT_NEWER_TO_OLDER)) {
                "," + BlotterFilter.SORT_NEWER_TO_OLDER_BY_ID
            } else {
                "," + BlotterFilter.SORT_OLDER_TO_NEWER_BY_ID
            }
        }
        return sortOrder
    }

    fun getAllTemplates(filter: WhereFilter, sortBy: String?): Cursor {
        val t0 = System.currentTimeMillis()
        return try {
            db().query(DatabaseHelper.V_ALL_TRANSACTIONS, BlotterColumns.NORMAL_PROJECTION,
                    filter.selection, filter.selectionArgs, null, null,
                    sortBy)
        } finally {
            val t1 = System.currentTimeMillis()
            Log.i("DB", "getBlotter " + (t1 - t0) + "ms")
        }
    }

    fun getBlotterWithSplits(where: String?): Cursor {
        return db().query(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BlotterColumns.NORMAL_PROJECTION, where, null, null, null, BlotterColumns.datetime.toString() + " DESC")
    }

    private fun updateLocationCount(locationId: Long, count: Int) {
        db().execSQL(LOCATION_COUNT_UPDATE, arrayOf(count, locationId))
    }

    private fun updateLastUsed(t: Transaction?) {
        val db = db()
        if (t!!.isTransfer) {
            db.execSQL(ACCOUNT_LAST_ACCOUNT_UPDATE, arrayOf<Any>(t.toAccountId, t.fromAccountId))
        }
        db.execSQL(ACCOUNT_LAST_CATEGORY_UPDATE, arrayOf<Any>(t.categoryId, t.fromAccountId))
        db.execSQL(PAYEE_LAST_CATEGORY_UPDATE, arrayOf<Any>(t.categoryId, t.payeeId))
        db.execSQL(CATEGORY_LAST_LOCATION_UPDATE, arrayOf<Any>(t.locationId, t.categoryId))
        db.execSQL(CATEGORY_LAST_PROJECT_UPDATE, arrayOf<Any>(t.projectId, t.categoryId))
    }

    fun duplicateTransaction(id: Long): Long {
        return duplicateTransaction(id, 0, 1)
    }

    fun duplicateTransactionWithMultiplier(id: Long, multiplier: Int): Long {
        return duplicateTransaction(id, 0, multiplier)
    }

    fun duplicateTransactionAsTemplate(id: Long): Long {
        return duplicateTransaction(id, 1, 1)
    }

    private fun duplicateTransaction(id: Long, isTemplate: Int, multiplier: Int): Long {
        var id = id
        val db = db()
        db.beginTransaction()
        return try {
            val now = System.currentTimeMillis()
            var transaction = getTransaction(id)
            if (transaction.isSplitChild) {
                id = transaction.parentId
                transaction = getTransaction(id)
            }
            transaction.lastRecurrence = now
            updateTransaction(transaction)
            transaction.id = -1
            transaction.isTemplate = isTemplate
            transaction.dateTime = now
            transaction.remoteKey = null
            if (isTemplate == 0) {
                transaction.recurrence = null
                transaction.notificationOptions = null
            }
            if (multiplier > 1) {
                transaction.fromAmount *= multiplier.toLong()
                transaction.toAmount *= multiplier.toLong()
            }
            val transactionId = insertTransaction(transaction)
            val attributesMap = getAllAttributesForTransaction(id)
            val attributes = LinkedList<TransactionAttribute>()
            for (attributeId in attributesMap.keys) {
                val ta = TransactionAttribute()
                ta.attributeId = attributeId
                ta.value = attributesMap[attributeId]
                attributes.add(ta)
            }
            if (attributes.size > 0) {
                insertAttributes(transactionId, attributes)
            }
            val splits = getSplitsForTransaction(id)
            if (multiplier > 1) {
                for (split in splits) {
                    split.fromAmount *= multiplier.toLong()
                    split.remoteKey = null
                }
            }
            transaction.id = transactionId
            transaction.splits = splits
            insertSplits(transaction)
            db.setTransactionSuccessful()
            transactionId
        } finally {
            db.endTransaction()
        }
    }

    @JvmOverloads
    fun insertOrUpdate(transaction: Transaction?, attributes: List<TransactionAttribute>? = emptyList()): Long {
        val db = db()
        db.beginTransaction()
        return try {
            val id = insertOrUpdateInTransaction(transaction, attributes)
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    fun insertOrUpdateInTransaction(transaction: Transaction?, attributes: List<TransactionAttribute>?): Long {
        val transactionId: Long
        transaction!!.lastRecurrence = System.currentTimeMillis()
        if (transaction.id == -1L) {
            transactionId = insertTransaction(transaction)
        } else {
            updateTransaction(transaction)
            transactionId = transaction.id
            db().delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?", arrayOf(transactionId.toString()))
            deleteSplitsForParentTransaction(transactionId)
        }
        attributes?.let { insertAttributes(transactionId, it) }
        transaction.id = transactionId
        insertSplits(transaction)
        updateAccountLastTransactionDate(transaction.fromAccountId)
        updateAccountLastTransactionDate(transaction.toAccountId)
        return transactionId
    }

    fun insertWithoutUpdatingBalance(transaction: Transaction) {
        updateAccountBalance = false
        try {
            transaction.id = insertTransaction(transaction)
            insertSplits(transaction)
        } finally {
            updateAccountBalance = true
        }
    }

    private fun insertAttributes(transactionId: Long, attributes: List<TransactionAttribute>) {
        for (a in attributes) {
            a.transactionId = transactionId
            val values = a.toValues()
            db().insert(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, null, values)
        }
    }

    private fun insertAttributes(transactionId: Long, categoryAttributes: Map<Long, String>?) {
        if (categoryAttributes != null && categoryAttributes.size > 0) {
            val attributes: MutableList<TransactionAttribute> = LinkedList()
            for ((key, value) in categoryAttributes) {
                val a = TransactionAttribute()
                a.attributeId = key
                a.value = value
                attributes.add(a)
            }
            insertAttributes(transactionId, attributes)
        }
    }

    private fun insertSplits(parent: Transaction?) {
        val splits = parent!!.splits
        if (splits != null) {
            for (split in splits) {
                split.id = -1
                split.parentId = parent.id
                split.dateTime = parent.dateTime
                split.fromAccountId = parent.fromAccountId
                split.payeeId = parent.payeeId
                split.isTemplate = parent.isTemplate
                split.status = parent.status
                updateSplitOriginalAmount(parent, split)
                val splitId = insertTransaction(split)
                insertAttributes(splitId, split.categoryAttributes)
            }
        }
    }

    private fun updateSplitOriginalAmount(parent: Transaction?, split: Transaction) {
        if (parent!!.originalCurrencyId > 0) {
            split.originalCurrencyId = parent.originalCurrencyId
            split.originalFromAmount = split.fromAmount
            split.fromAmount = calculateAmountInAccountCurrency(parent, split.fromAmount)
        }
    }

    private fun calculateAmountInAccountCurrency(parent: Transaction?, amount: Long): Long {
        val rate = getRateFromParent(parent)
        return (rate * amount).toLong()
    }

    private fun getRateFromParent(parent: Transaction?): Double {
        return if (parent!!.originalFromAmount != 0L) {
            Math.abs(1.0 * parent.fromAmount / parent.originalFromAmount)
        } else 0.0
    }

    private fun insertTransaction(t: Transaction?): Long {
        t!!.updatedOn = System.currentTimeMillis()
        val id = db().insert(DatabaseHelper.TRANSACTION_TABLE, null, t.toValues())
        if (updateAccountBalance) {
            if (!t.isTemplateLike) {
                if (t.isSplitChild) {
                    if (t.isTransfer) {
                        updateToAccountBalance(t, id)
                    }
                } else {
                    updateFromAccountBalance(t, id)
                    updateToAccountBalance(t, id)
                    updateLocationCount(t.locationId, 1)
                    updateLastUsed(t)
                }
            }
        }
        return id
    }

    private fun updateFromAccountBalance(t: Transaction?, id: Long) {
        updateAccountBalance(t!!.fromAccountId, t.fromAmount)
        insertRunningBalance(t.fromAccountId, id, t.dateTime, t.fromAmount, t.fromAmount)
    }

    private fun updateToAccountBalance(t: Transaction?, id: Long) {
        updateAccountBalance(t!!.toAccountId, t.toAmount)
        insertRunningBalance(t.toAccountId, id, t.dateTime, t.toAmount, t.toAmount)
    }

    private fun updateTransaction(t: Transaction?) {
        var oldT: Transaction? = null
        if (t!!.isNotTemplateLike) {
            oldT = getTransaction(t.id)
            updateAccountBalance(oldT.fromAccountId, oldT.fromAmount, t.fromAccountId, t.fromAmount)
            updateAccountBalance(oldT.toAccountId, oldT.toAmount, t.toAccountId, t.toAmount)
            updateRunningBalance(oldT, t)
            if (oldT.locationId != t.locationId) {
                updateLocationCount(oldT.locationId, -1)
                updateLocationCount(t.locationId, 1)
            }
        }
        t.updatedOn = System.currentTimeMillis()
        db().update(DatabaseHelper.TRANSACTION_TABLE, t.toValues(), TransactionColumns._id.toString() + "=?", arrayOf(t.id.toString()))
        if (oldT != null) {
            updateAccountLastTransactionDate(oldT.fromAccountId)
            updateAccountLastTransactionDate(oldT.toAccountId)
        }
    }

    fun updateTransactionStatus(id: Long, status: TransactionStatus?) {
        val t = getTransaction(id)
        t.status = status
        updateTransaction(t)
    }

    fun deleteTransaction(id: Long) {
        val db = db()
        db.beginTransaction()
        try {
            deleteTransactionNoDbTransaction(id)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTransactionNoDbTransaction(id: Long) {
        val t = getTransaction(id)
        if (t.isNotTemplateLike) {
            revertFromAccountBalance(t)
            revertToAccountBalance(t)
            updateAccountLastTransactionDate(t.fromAccountId)
            updateAccountLastTransactionDate(t.toAccountId)
            updateLocationCount(t.locationId, -1)
        }
        val sid = arrayOf(id.toString())
        val db = db()
        db.delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?", sid)
        db.delete(DatabaseHelper.TRANSACTION_TABLE, TransactionColumns._id.toString() + "=?", sid)
        deleteSplitsForParentTransaction(id)
    }

    private fun deleteSplitsForParentTransaction(parentId: Long) {
        val splits = getSplitsForTransaction(parentId)
        val db = db()
        for (split in splits) {
            if (split.isTransfer) {
                revertToAccountBalance(split)
            }
            db.delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?", arrayOf(split.id.toString()))
        }
        db.delete(DatabaseHelper.TRANSACTION_TABLE, TransactionColumns.parent_id.toString() + "=?", arrayOf(parentId.toString()))
    }

    private fun revertFromAccountBalance(t: Transaction) {
        updateAccountBalance(t.fromAccountId, -t.fromAmount)
        deleteRunningBalance(t.fromAccountId, t.id, t.fromAmount, t.dateTime)
    }

    private fun revertToAccountBalance(t: Transaction) {
        updateAccountBalance(t.toAccountId, -t.toAmount)
        deleteRunningBalance(t.toAccountId, t.id, t.toAmount, t.dateTime)
    }

    private fun updateAccountBalance(oldAccountId: Long, oldAmount: Long, newAccountId: Long, newAmount: Long) {
        if (oldAccountId == newAccountId) {
            updateAccountBalance(newAccountId, newAmount - oldAmount)
        } else {
            updateAccountBalance(oldAccountId, -oldAmount)
            updateAccountBalance(newAccountId, newAmount)
        }
    }

    private fun updateAccountBalance(accountId: Long, deltaAmount: Long) {
        if (accountId <= 0) {
            return
        }
        db().execSQL(ACCOUNT_TOTAL_AMOUNT_UPDATE, arrayOf<Any>(deltaAmount, accountId))
    }

    private fun insertRunningBalance(accountId: Long, transactionId: Long, datetime: Long, amount: Long, deltaAmount: Long) {
        if (accountId <= 0) {
            return
        }
        val previousTransactionBalance = fetchAccountBalanceAtTheTime(accountId, datetime)
        val db = db()
        db.execSQL(INSERT_RUNNING_BALANCE, arrayOf<Any>(accountId, transactionId, datetime, previousTransactionBalance + amount))
        db.execSQL(UPDATE_RUNNING_BALANCE, arrayOf<Any>(deltaAmount, accountId, datetime))
    }

    private fun updateRunningBalance(oldTransaction: Transaction?, newTransaction: Transaction?) {
        deleteRunningBalance(oldTransaction!!.fromAccountId, oldTransaction.id, oldTransaction.fromAmount, oldTransaction.dateTime)
        insertRunningBalance(newTransaction!!.fromAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.fromAmount, newTransaction.fromAmount)
        deleteRunningBalance(oldTransaction.toAccountId, oldTransaction.id, oldTransaction.toAmount, oldTransaction.dateTime)
        insertRunningBalance(newTransaction.toAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.toAmount, newTransaction.toAmount)
    }

    private fun deleteRunningBalance(accountId: Long, transactionId: Long, amount: Long, dateTime: Long) {
        if (accountId <= 0) {
            return
        }
        val db = db()
        db.execSQL(DELETE_RUNNING_BALANCE, arrayOf<Any>(accountId, transactionId))
        db.execSQL(UPDATE_RUNNING_BALANCE, arrayOf<Any>(-amount, accountId, dateTime))
    }

    private fun fetchAccountBalanceAtTheTime(accountId: Long, datetime: Long): Long {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id = ? and datetime <= ? order by datetime desc, transaction_id desc limit 1", arrayOf(accountId.toString(), datetime.toString()))
    }

    // ===================================================================
    // CATEGORY
    // ===================================================================
    fun insertOrUpdate(category: Category, attributes: List<Attribute>?): Long {
        val db = db()
        db.beginTransaction()
        return try {
            val id: Long
            id = if (category.id == -1L) {
                insertCategory(category)
            } else {
                updateCategory(category)
                category.id
            }
            addAttributes(id, attributes)
            category.id = id
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    private fun addAttributes(categoryId: Long, attributes: List<Attribute>?) {
        val db = db()
        db.delete(DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.CATEGORY_ID + "=?", arrayOf(categoryId.toString()))
        if (attributes != null) {
            val values = ContentValues()
            values.put(CategoryAttributeColumns.CATEGORY_ID, categoryId)
            for (a in attributes) {
                values.put(CategoryAttributeColumns.ATTRIBUTE_ID, a.id)
                db.insert(DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE, null, values)
            }
        }
    }

    private fun insertCategory(category: Category): Long {
        val tree = getCategoriesTree(false)
        val parentId = category.parentId
        if (parentId == Category.NO_CATEGORY_ID) {
            if (!tree.isEmpty) {
                return insertAsLast(category, tree)
            }
        } else {
            val map = tree.asMap()
            val parent = map[parentId]
            if (parent != null && parent.hasChildren()) {
                val children = parent.children
                if(children != null) {
                    return insertAsLast(category, children)
                }
            }
        }
        return insertChildCategory(parentId, category)
    }

    private fun insertAsLast(category: Category, tree: CategoryTree<Category>): Long {
        val mateId = tree.getAt(tree.size() - 1)!!.id
        return insertMateCategory(mateId, category)
    }

    private fun updateCategory(category: Category): Long {
        val oldCategory = getCategoryWithParent(category.id)
        if (oldCategory.parentId == category.parentId) {
            updateCategory(category.id, category.title, category.type)
            updateChildCategoriesType(category.type, category.left, category.right)
        } else {
            moveCategory(category)
        }
        return category.id
    }

    private fun moveCategory(category: Category) {
        val tree = getCategoriesTree(false)
        val map = tree.asMap()
        val oldCategory = map[category.id]
        if (oldCategory != null) {
            val oldParent = map[oldCategory.parentId]
            if (oldParent != null) {
                oldParent.removeChild(oldCategory)
            } else {
                tree.remove(oldCategory)
            }
            val newParent = map[category.parentId]
            var newCategoryType = category.type
            if (newParent != null) {
                newParent.addChild(oldCategory)
                newCategoryType = newParent.type
            } else {
                tree.add(oldCategory)
            }
            tree.reIndex()
            updateCategoryTreeInTransaction(tree)
            updateCategory(category.id, category.title, newCategoryType)
            updateChildCategoriesType(newCategoryType, oldCategory.left, oldCategory.right)
        }
    }

    fun getCategoryWithParent(id: Long): Category {
        val db = db()
        db.query(DatabaseHelper.V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, CategoryViewColumns._id.toString() + "=?", arrayOf(id.toString()), null, null, null).use { c ->
            return if (c.moveToNext()) {
                val cat = formCursor(c)
                val s = id.toString()
                db.query(GET_PARENT_SQL, arrayOf(CategoryColumns._id.name), null, arrayOf(s, s), null, null, null, "1").use { c2 ->
                    if (c2.moveToFirst()) {
                        cat.parent = Category(c2.getLong(0))
                    }
                }
                cat
            } else {
                Category(-1)
            }
        }
    }

    fun getCategoryIdsByLeftIds(leftIds: List<String?>): List<Long> {
        val db = db()
        val res: MutableList<Long> = LinkedList()
        db.query(DatabaseHelper.V_CATEGORY, arrayOf(CategoryViewColumns._id.name),
                CategoryViewColumns.left.toString() + " IN (" + StringUtil.generateQueryPlaceholders(leftIds.size) + ")",
                ArrUtils.strListToArr(leftIds), null, null, null).use { c ->
            while (c.moveToNext()) {
                res.add(c.getLong(0))
            }
        }
        return res
    }

    fun getCategoriesTreeWithoutSubTree(excludingTreeId: Long, includeNoCategory: Boolean): CategoryTree<Category> {
        var categories = getAllCategoriesList()
        if(!includeNoCategory) {
            categories = categories.filter { it.id > 0 }
        }
        if(excludingTreeId > 0)
        {
            val category = categories.firstOrNull { it.id == excludingTreeId }
            if(category != null) {
                categories = categories.filter { it.id < category.left || it.id > category.right }
            }
        }
        return CategoryTree.createFromCache(categories)
        /*val c: Cursor = when {
            excludingTreeId > 0 -> {
                getCategoriesWithoutSubtree(excludingTreeId, includeNoCategory)
            }
            else -> {
                getCategories(includeNoCategory)
            }
        }
        return CategoryTree.createFromCursor<Category>(c) { obj: Cursor? -> formCursor(c) }*/
    }

    fun getCategoriesTree(includeNoCategory: Boolean): CategoryTree<Category> {
        return getCategoriesTreeWithoutSubTree(-1, includeNoCategory)
    }

    private val allCategoriesTree: CategoryTree<Category>
        get() {
            return CategoryTree.createFromCache(CategoriesCache.categoriesList)
        }

    val allCategoriesMap: Map<Long, Category>
        get() = allCategoriesTree.asMap()

    fun getCategoriesList(includeNoCategory: Boolean): List<Category> {
        getCategories(includeNoCategory).use { c -> return categoriesAsList(c) }
    }

    val allCategories: Cursor
        get() = db().query(DatabaseHelper.V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, null, null, null, null, null)

    fun getAllCategoriesList(): List<Category> {
        updateCategoriesCache(false)
        return CategoriesCache.categoriesList
    }

    fun updateCategoriesCache(force: Boolean) {
        if (force || CategoriesCache.categoriesList.isEmpty()) {
            allCategories.use { c ->
                CategoriesCache.updateCache(categoriesAsList(c))
            }
        }
    }

    private fun categoriesAsList(c: Cursor): ArrayList<Category> {
        val list: ArrayList<Category> = ArrayList()
        while (c.moveToNext()) {
            val category = formCursor(c)
            list.add(category)
        }
        return list
    }

    fun getCategories(includeNoCategory: Boolean): Cursor {
        return getCategories(includeNoCategory, null)
    }

    fun filterCategories(titleFilter: CharSequence?): Cursor {
        return getCategories(false, titleFilter)
    }

    fun getCategories(includeNoCategory: Boolean, titleFilter: CharSequence?): Cursor {
        var query = CategoryViewColumns._id.toString() + if (includeNoCategory) ">=0" else ">0"
        var args: Array<String>? = null
        if (titleFilter != null) {
            query += " and (" + CategoryViewColumns.title + " like ? or " + CategoryViewColumns.title + " like ? )"
            args = arrayOf(
                    "%$titleFilter%",
                    "%" + StringUtil.capitalize(titleFilter.toString()) + "%")
        }
        return db().query(DatabaseHelper.V_CATEGORY,
                CategoryViewColumns.NORMAL_PROJECTION,
                query,
                args, null, null, null)
    }

    fun getCategoriesWithoutSubtree(id: Long, includeNoCategory: Boolean): Cursor {
        val db = db()
        var left: Long = 0
        var right: Long = 0
        db.query(DatabaseHelper.CATEGORY_TABLE, arrayOf(CategoryColumns.left.name, CategoryColumns.right.name), CategoryColumns._id.toString() + "=?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) {
                left = c.getLong(0)
                right = c.getLong(1)
            }
        }
        return db.query(DatabaseHelper.V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
                "(NOT (" + CategoryViewColumns.left + ">=? AND " + CategoryColumns.right + "<=?)) AND "
                        + CategoryViewColumns._id + if (includeNoCategory) ">=0" else ">0", arrayOf(left.toString(), right.toString()), null, null, null)
    }

    fun getCategoriesWithoutSubtreeAsList(categoryId: Long): List<Category> {
        val list: MutableList<Category> = ArrayList()
        getCategoriesWithoutSubtree(categoryId, true).use { c ->
            while (c.moveToNext()) {
                val category = formCursor(c)
                list.add(category)
            }
            return list
        }
    }

    fun insertChildCategory(parentId: Long, category: Category): Long {
        //DECLARE v_leftkey INT UNSIGNED DEFAULT 0;
        //SELECT l INTO v_leftkey FROM `nset` WHERE `id` = ParentID;
        //UPDATE `nset` SET `r` = `r` + 2 WHERE `r` > v_leftkey;
        //UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_leftkey;
        //INSERT INTO `nset` (`name`, `l`, `r`) VALUES (NodeName, v_leftkey + 1, v_leftkey + 2);
        val type = getActualCategoryType(parentId, category)
        return insertCategory(CategoryColumns.left.name, parentId, category.title, type)
    }

    fun insertMateCategory(categoryId: Long, category: Category): Long {
        //DECLARE v_rightkey INT UNSIGNED DEFAULT 0;
        //SELECT `r` INTO v_rightkey FROM `nset` WHERE `id` = MateID;
        //UPDATE `	nset` SET `r` = `r` + 2 WHERE `r` > v_rightkey;
        //UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_rightkey;
        //INSERT `nset` (`name`, `l`, `r`) VALUES (NodeName, v_rightkey + 1, v_rightkey + 2);
        val mate = getCategoryWithParent(categoryId)
        val parentId = mate.parentId
        val type = getActualCategoryType(parentId, category)
        return insertCategory(CategoryColumns.right.name, categoryId, category.title, type)
    }

    private fun getActualCategoryType(parentId: Long, category: Category): Int {
        var type = category.type
        if (parentId > 0) {
            val parent = getCategoryWithParent(parentId)
            type = parent.type
        }
        return type
    }

    private fun insertCategory(field: String, categoryId: Long, title: String, type: Int): Long {
        var num = 0
        val db = db()
        val c = db.query(DatabaseHelper.CATEGORY_TABLE, arrayOf(field), CategoryColumns._id.toString() + "=?", arrayOf(categoryId.toString()), null, null, null)
        try {
            if (c.moveToFirst()) {
                num = c.getInt(0)
            }
        } finally {
            c.close()
        }
        val args = arrayOf(num.toString())
        db.execSQL(INSERT_CATEGORY_UPDATE_RIGHT, args)
        db.execSQL(INSERT_CATEGORY_UPDATE_LEFT, args)
        val values = ContentValues()
        values.put(CategoryColumns.title.name, title)
        val left = num + 1
        val right = num + 2
        values.put(CategoryColumns.left.name, left)
        values.put(CategoryColumns.right.name, right)
        values.put(CategoryColumns.type.name, type)
        val id = db.insert(DatabaseHelper.CATEGORY_TABLE, null, values)
        updateChildCategoriesType(type, left, right)
        updateCategoriesCache(true)
        return id
    }

    private fun updateChildCategoriesType(type: Int, left: Int, right: Int) {
        db().execSQL(CATEGORY_UPDATE_CHILDREN_TYPES, arrayOf<Any>(type, left, right))
    }

    fun deleteCategory(categoryId: Long) {
        //DECLARE v_leftkey, v_rightkey, v_width INT DEFAULT 0;
        //
        //SELECT
        //	`l`, `r`, `r` - `l` + 1 INTO v_leftkey, v_rightkey, v_width
        //FROM `nset`
        //WHERE
        //	`id` = NodeID;
        //
        //DELETE FROM `nset` WHERE `l` BETWEEN v_leftkey AND v_rightkey;
        //
        //UPDATE `nset`
        //SET
        //	`l` = IF(`l` > v_leftkey, `l` - v_width, `l`),
        //	`r` = `r` - v_width
        //WHERE
        //	`r` > v_rightkey;
        val db = db()
        var left = 0
        var right = 0
        val c = db.query(DatabaseHelper.CATEGORY_TABLE, arrayOf(CategoryColumns.left.name, CategoryColumns.right.name), CategoryColumns._id.toString() + "=?", arrayOf(categoryId.toString()), null, null, null)
        try {
            if (c.moveToFirst()) {
                left = c.getInt(0)
                right = c.getInt(1)
            }
        } finally {
            c.close()
        }
        db.beginTransaction()
        try {
            val width = right - left + 1
            val args = arrayOf(left.toString(), right.toString())
            db.execSQL(DELETE_CATEGORY_UPDATE1, args)
            db.delete(DatabaseHelper.CATEGORY_TABLE, CategoryColumns.left.toString() + " BETWEEN ? AND ?", args)
            db.execSQL(String.format(DELETE_CATEGORY_UPDATE2, left, width, width, right))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            updateCategoriesCache(true)
        }
    }

    private fun updateCategory(id: Long, title: String, type: Int) {
        val values = ContentValues()
        values.put(CategoryColumns.title.name, title)
        values.put(CategoryColumns.type.name, type)
        db().update(DatabaseHelper.CATEGORY_TABLE, values, CategoryColumns._id.toString() + "=?", arrayOf(id.toString()))
        updateCategoriesCache(true)
    }

    fun insertCategoryTreeInTransaction(tree: CategoryTree<Category>) {
        db().delete("category", "_id > 0", null)
        insertCategoryInTransaction(tree)
        updateCategoryTreeInTransaction(tree)
        updateCategoriesCache(true)
    }

    private fun insertCategoryInTransaction(tree: CategoryTree<Category>) {
        for (category in tree) {
            reInsertEntity(category)
            if (category.children != null) {
                insertCategoryInTransaction(category.children!!)
            }
        }
        updateCategoriesCache(true)
    }

    fun updateCategoryTree(tree: CategoryTree<Category>) {
        val db = db()
        db.beginTransaction()
        try {
            updateCategoryTreeInTransaction(tree)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            updateCategoriesCache(true)
        }
    }

    private fun updateCategoryTreeInTransaction(tree: CategoryTree<Category>) {
        var left = 1
        var right = 2
        val values = ContentValues()
        val sid = arrayOfNulls<String>(1)
        for (c in tree) {
            values.put(CategoryColumns.left.name, c!!.left)
            values.put(CategoryColumns.right.name, c.right)
            sid[0] = c.id.toString()
            db().update(DatabaseHelper.CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid)
            if (c.children != null) {
                updateCategoryTreeInTransaction(c.children!!)
            }
            if (c.left < left) {
                left = c.left
            }
            if (c.right > right) {
                right = c.right
            }
        }
        values.put(CategoryColumns.left.name, left - 1)
        values.put(CategoryColumns.right.name, right + 1)
        sid[0] = Category.NO_CATEGORY_ID.toString()
        db().update(DatabaseHelper.CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid)
        updateCategoriesCache(true)
    }

    // ===================================================================
    // SMS TEMPLATES >>
    // ===================================================================
    fun getSmsTemplatesForCategory(categoryId: Long): List<SmsTemplate> {
        db().query(DatabaseHelper.SMS_TEMPLATES_TABLE, SmsTemplateColumns.NORMAL_PROJECTION, SmsTemplateColumns.category_id.toString() + "=?", arrayOf(categoryId.toString()), null, null, SmsTemplateColumns.title.name).use { c ->
            val res: MutableList<SmsTemplate> = ArrayList(c.count)
            while (c.moveToNext()) {
                val a = SmsTemplate.fromCursor(c)
                res.add(a)
            }
            return res
        }
    }

    fun getSmsTemplatesByNumber(smsNumber: String): List<SmsTemplate> {
        db().rawQuery(String.format("select %s from %s where %s=? order by %s, length(%s) desc",
                DatabaseUtils.generateSelectClause(SmsTemplateColumns.NORMAL_PROJECTION, null),
                DatabaseHelper.SMS_TEMPLATES_TABLE, SmsTemplateColumns.title, SmsTemplateColumns.sort_order, SmsTemplateColumns.template), arrayOf(smsNumber)).use { c ->
            val res: MutableList<SmsTemplate> = ArrayList(c.count)
            while (c.moveToNext()) {
                val a = SmsTemplate.fromCursor(c)
                res.add(a)
            }
            return res
        }
    }

    fun findAllSmsTemplateNumbers(): Set<String> {
        db().rawQuery("select distinct " + SmsTemplateColumns.title + " from " + DatabaseHelper.SMS_TEMPLATES_TABLE +
                " where " + SmsTemplateColumns.template + " is not null", null).use { c ->
            val res: MutableSet<String> = HashSet(c.count)
            while (c.moveToNext()) {
                res.add(c.getString(0))
            }
            return res
        }
    }

    val allSmsTemplates: Cursor
        get() = db().query(DatabaseHelper.SMS_TEMPLATES_TABLE, SmsTemplateColumns.NORMAL_PROJECTION, SmsTemplateColumns.template.toString() + " is not null", null, null, null, SmsTemplateColumns.title.name)
    val smsTemplatesWithFullInfo: Cursor
        get() = getSmsTemplatesWithFullInfo(null)

    fun getSmsTemplatesWithFullInfo(filter: String?): Cursor {
        var nativeQuery = String.format(
                "select %s, c.%s as %s, c.%s as %s " +
                        "from %s t left outer join %s c on t.%s = c.%s ",
                DatabaseUtils.generateSelectClause(SmsTemplateColumns.NORMAL_PROJECTION, "t"),
                CategoryViewColumns.title, SmsTemplateListColumns.cat_name, CategoryViewColumns.level, SmsTemplateListColumns.cat_level,
                DatabaseHelper.SMS_TEMPLATES_TABLE,
                DatabaseHelper.V_CATEGORY, SmsTemplateColumns.category_id, CategoryViewColumns._id)
        if (!StringUtil.isEmpty(filter)) {
            nativeQuery += String.format("where t.%s like '%%%s%%' or t.%s like '%%%2\$s%%' ",
                    CategoryViewColumns.title, filter, SmsTemplateColumns.template)
        }
        nativeQuery += "order by t." + SmsTemplateColumns.sort_order
        return db().rawQuery(nativeQuery, arrayOf())
    }

    fun duplicateSmsTemplateBelowOriginal(id: Long): Long {
        val newId = duplicate(SmsTemplate::class.java, id)
        val nextOrderItem = getNextByOrder(SmsTemplate::class.java, id)
        if (nextOrderItem > 0) {
            moveItemByChangingOrder(SmsTemplate::class.java, newId, nextOrderItem)
        }
        return newId
    }

    // ===================================================================
    // ATTRIBUTES
    // ===================================================================
    fun getAttributesForCategory(categoryId: Long): ArrayList<Attribute> {
        db().query(DatabaseHelper.V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
                CategoryAttributeColumns.CATEGORY_ID + "=?", arrayOf(categoryId.toString()),
                null, null, AttributeColumns.TITLE).use { c ->
            val list = ArrayList<Attribute>(c.count)
            while (c.moveToNext()) {
                val a = Attribute.fromCursor(c)
                list.add(a)
            }
            return list
        }
    }

    fun getAllAttributesForCategory(categoryId: Long): ArrayList<Attribute> {
        val category = getCategoryWithParent(categoryId)
        db().query(DatabaseHelper.V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
                AttributeViewColumns.CATEGORY_LEFT + "<= ? AND " + AttributeViewColumns.CATEGORY_RIGHT + " >= ?", arrayOf(category.left.toString(), category.right.toString()),
                null, null, AttributeColumns.TITLE).use { c ->
            val list = ArrayList<Attribute>(c.count)
            while (c.moveToNext()) {
                val a = Attribute.fromCursor(c)
                list.add(a)
            }
            return list
        }
    }

    fun getSystemAttribute(a: SystemAttribute): Attribute {
        val sa = getAttribute(a.id)
        sa.title = context.getString(a.titleId)
        return sa
    }

    fun getAttribute(id: Long): Attribute {
        db().query(DatabaseHelper.ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
                AttributeColumns.ID + "=?", arrayOf(id.toString()),
                null, null, null).use { c ->
            if (c.moveToFirst()) {
                return Attribute.fromCursor(c)
            }
        }
        return Attribute()
    }

    fun insertOrUpdate(attribute: Attribute): Long {
        return if (attribute.id == -1L) {
            insertAttribute(attribute)
        } else {
            updateAttribute(attribute)
            attribute.id
        }
    }

    fun deleteAttribute(id: Long) {
        val db = db()
        db.beginTransaction()
        try {
            val attr = getAttribute(id)
            val p = arrayOf(id.toString())
            db.delete(DatabaseHelper.ATTRIBUTES_TABLE, AttributeColumns.ID + "=?", p)
            db.delete(DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.ATTRIBUTE_ID + "=?", p)
            db.delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.ATTRIBUTE_ID + "=?", p)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertAttribute(attribute: Attribute): Long {
        return db().insert(DatabaseHelper.ATTRIBUTES_TABLE, null, attribute.toValues())
    }

    private fun updateAttribute(attribute: Attribute) {
        db().update(DatabaseHelper.ATTRIBUTES_TABLE, attribute.toValues(), AttributeColumns.ID + "=?", arrayOf(attribute.id.toString()))
    }

    val allAttributes: Cursor
        get() = db().query(DatabaseHelper.ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
                AttributeColumns.ID + ">0", null, null, null, AttributeColumns.TITLE)
    val allAttributesMap: Map<Long, String>
        get() {
            db().query(DatabaseHelper.V_ATTRIBUTES, AttributeViewColumns.NORMAL_PROJECTION, null, null, null, null,
                    AttributeViewColumns.CATEGORY_ID + ", " + AttributeViewColumns.TITLE).use { c ->
                val attributes = HashMap<Long, String>()
                var sb: StringBuilder? = null
                var prevCategoryId: Long = -1
                while (c.moveToNext()) {
                    val categoryId = c.getLong(AttributeViewColumns.Indicies.CATEGORY_ID)
                    val name = c.getString(AttributeViewColumns.Indicies.NAME)
                    if (prevCategoryId != categoryId) {
                        if (sb != null) {
                            attributes[prevCategoryId] = sb.append("]").toString()
                            sb.setLength(1)
                        } else {
                            sb = StringBuilder()
                            sb.append("[")
                        }
                        prevCategoryId = categoryId
                    }
                    if (sb!!.length > 1) {
                        sb.append(", ")
                    }
                    sb.append(name)
                }
                if (sb != null) {
                    attributes[prevCategoryId] = sb.append("]").toString()
                }
                return attributes
            }
        }

    fun getAllAttributesForTransaction(transactionId: Long): Map<Long, String> {
        val c = db().query(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
                TransactionAttributeColumns.TRANSACTION_ID + "=? AND " + TransactionAttributeColumns.ATTRIBUTE_ID + ">=0", arrayOf(transactionId.toString()),
                null, null, null)
        return try {
            val attributes = HashMap<Long, String>()
            while (c.moveToNext()) {
                val attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID)
                val value = c.getString(TransactionAttributeColumns.Indicies.VALUE)
                attributes[attributeId] = value
            }
            attributes
        } finally {
            c.close()
        }
    }

    fun getSystemAttributesForTransaction(transactionId: Long): EnumMap<SystemAttribute, String> {
        val c = db().query(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
                TransactionAttributeColumns.TRANSACTION_ID + "=? AND " + TransactionAttributeColumns.ATTRIBUTE_ID + "<0", arrayOf(transactionId.toString()),
                null, null, null)
        return try {
            val attributes = EnumMap<SystemAttribute, String>(SystemAttribute::class.java)
            while (c.moveToNext()) {
                val attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID)
                val value = c.getString(TransactionAttributeColumns.Indicies.VALUE)
                attributes[SystemAttribute.forId(attributeId)] = value
            }
            attributes
        } finally {
            c.close()
        }
    }

    /**
     * Sets status=CL (Cleared) for the selected transactions
     *
     * @param ids selected transactions' ids
     */
    fun clearSelectedTransactions(ids: LongArray) {
        val sql = "UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " + TransactionColumns.status + "='" + TransactionStatus.CL + "'"
        runInTransaction(sql, ids)
    }

    /**
     * Sets status=RC (Reconciled) for the selected transactions
     *
     * @param ids selected transactions' ids
     */
    fun reconcileSelectedTransactions(ids: LongArray) {
        val sql = "UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " + TransactionColumns.status + "='" + TransactionStatus.RC + "'"
        runInTransaction(sql, ids)
    }

    /**
     * Deletes the selected transactions
     *
     * @param ids selected transactions' ids
     */
    fun deleteSelectedTransactions(ids: LongArray) {
        val db = db()
        db.beginTransaction()
        try {
            for (id in ids) {
                deleteTransactionNoDbTransaction(id)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun runInTransaction(sql: String, ids: LongArray) {
        val db = db()
        db.beginTransaction()
        try {
            val count = ids.size
            val bucket = 100
            val num = 1 + count / bucket
            for (i in 0 until num) {
                val x = bucket * i
                val y = Math.min(count, bucket * (i + 1))
                val script = createSql(sql, ids, x, y)
                db.execSQL(script)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun createSql(updateSql: String, ids: LongArray, x: Int, y: Int): String {
        val sb = StringBuilder(updateSql)
                .append(" WHERE ")
                .append(TransactionColumns.is_template)
                .append("=0 AND ")
                .append(TransactionColumns.parent_id)
                .append("=0 AND ")
                .append(TransactionColumns._id)
                .append(" IN (")
        for (i in x until y) {
            if (i > x) {
                sb.append(",")
            }
            sb.append(ids[i])
        }
        sb.append(")")
        return sb.toString()
    }

    fun storeMissedSchedules(restored: List<RestoredTransaction>, now: Long): LongArray {
        val db = db()
        db.beginTransaction()
        return try {
            val count = restored.size
            val restoredIds = LongArray(count)
            val transactions = HashMap<Long, Transaction?>()
            for (i in 0 until count) {
                val rt = restored[i]
                val transactionId = rt.transactionId
                var t = transactions[transactionId]
                if (t == null) {
                    t = getTransaction(transactionId)
                    transactions[transactionId] = t
                }
                t.id = -1
                t.dateTime = rt.dateTime.time
                t.status = TransactionStatus.RS
                t.isTemplate = 0
                restoredIds[i] = insertOrUpdate(t)
                t.id = transactionId
            }
            for (t in transactions.values) {
                db.execSQL(UPDATE_LAST_RECURRENCE, arrayOf<Any>(now, t!!.id))
            }
            db.setTransactionSuccessful()
            restoredIds
        } finally {
            db.endTransaction()
        }
    }

    /**
     * @param accountId
     * @param period
     * @return
     */
    fun getCustomClosingDay(accountId: Long, period: Int): Int {
        val where = CreditCardClosingDateColumns.ACCOUNT_ID + "=? AND " +
                CreditCardClosingDateColumns.PERIOD + "=?"
        val c = db().query(DatabaseHelper.CCARD_CLOSING_DATE_TABLE, arrayOf(CreditCardClosingDateColumns.CLOSING_DAY),
                where, arrayOf(java.lang.Long.toString(accountId), Integer.toString(period)), null, null, null)
        var res = 0
        res = try {
            if (c != null) {
                if (c.count > 0) {
                    c.moveToFirst()
                    c.getInt(0)
                } else {
                    0
                }
            } else {
                // there is no custom closing day in database for the given account id an period
                0
            }
        } catch (e: SQLiteException) {
            0
        } finally {
            c!!.close()
        }
        return res
    }

    fun setCustomClosingDay(accountId: Long, period: Int, closingDay: Int) {
        val values = ContentValues()
        values.put(CreditCardClosingDateColumns.ACCOUNT_ID, java.lang.Long.toString(accountId))
        values.put(CreditCardClosingDateColumns.PERIOD, Integer.toString(period))
        values.put(CreditCardClosingDateColumns.CLOSING_DAY, Integer.toString(closingDay))
        db().insert(DatabaseHelper.CCARD_CLOSING_DATE_TABLE, null, values)
    }

    fun deleteCustomClosingDay(accountId: Long, period: Int) {
        val where = CreditCardClosingDateColumns.ACCOUNT_ID + "=? AND " +
                CreditCardClosingDateColumns.PERIOD + "=?"
        val args = arrayOf(java.lang.Long.toString(accountId), Integer.toString(period))
        db().delete(DatabaseHelper.CCARD_CLOSING_DATE_TABLE, where, args)
    }

    fun updateCustomClosingDay(accountId: Long, period: Int, closingDay: Int) {
        // delete previous content
        deleteCustomClosingDay(accountId, period)

        // save new value
        setCustomClosingDay(accountId, period, closingDay)
    }

    /**
     * Re-populates running_balance table for all accounts
     */
    fun rebuildRunningBalances() {
        val accounts = allAccountsList
        for (account in accounts) {
            rebuildRunningBalanceForAccount(account)
        }
    }

    /**
     * Re-populates running_balance for specific account
     *
     * @param account selected account
     */
    fun rebuildRunningBalanceForAccount(account: Account) {
        val db = db()
        db.beginTransaction()
        try {
            val accountId = account.getId().toString()
            db.execSQL("delete from running_balance where account_id=?", arrayOf<Any>(accountId))
            val filter = WhereFilter("")
            filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, accountId))
            filter.asc("datetime")
            filter.asc("_id")
            val values = arrayOfNulls<Any>(4)
            values[0] = accountId
            getBlotterForAccountWithSplits(filter).use { c ->
                var balance: Long = 0
                while (c.moveToNext()) {
                    val parentId = c.getLong(BlotterColumns.parent_id.ordinal)
                    val isTransfer = c.getInt(BlotterColumns.is_transfer.ordinal)
                    if (parentId > 0) {
                        if (isTransfer >= 0) {
                            // we only interested in the second part of the transfer-split
                            // which is marked with is_transfer=-1 (see v_blotter_for_account_with_splits)
                            continue
                        }
                    }
                    val fromAccountId = c.getLong(BlotterColumns.from_account_id.ordinal)
                    val toAccountId = c.getLong(BlotterColumns.to_account_id.ordinal)
                    if (toAccountId > 0 && toAccountId == fromAccountId) {
                        // weird bug when a transfer is done from an account to the same account
                        continue
                    }
                    balance += c.getLong(BlotterColumns.from_amount.ordinal)
                    values[1] = c.getString(BlotterColumns._id.ordinal)
                    values[2] = c.getString(BlotterColumns.datetime.ordinal)
                    values[3] = balance
                    db.execSQL("insert into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)", values)
                }
            }
            updateAccountLastTransactionDate(account.id)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun fetchBudgetBalance(categories: Map<Long?, Category?>?, projects: Map<Long?, Project?>?, b: Budget?): Long {
        val where = Budget.createWhere(b, categories, projects)
        val c = db().query(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, SUM_FROM_AMOUNT, where, null, null, null, null)
        try {
            if (c.moveToNext()) {
                return c.getLong(0)
            }
        } finally {
            c.close()
        }
        return 0
    }

    fun recalculateAccountsBalances() {
        val db = db()
        db.beginTransaction()
        try {
            for (account in allAccountsList) {
                recalculateAccountBalances(account.id)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun recalculateAccountBalances(accountId: Long) {
        val calculator = TransactionsTotalCalculator(this,
                enhanceFilterForAccountBlotter(WhereFilter.empty()
                        .eq(BlotterFilter.FROM_ACCOUNT_ID, accountId.toString())))
        val total = calculator.accountTotal
        val values = ContentValues()
        values.put(AccountColumns.TOTAL_AMOUNT, total.balance)
        db().update(DatabaseHelper.ACCOUNT_TABLE, values, AccountColumns.ID + "=?", arrayOf(accountId.toString()))
        Log.i("DatabaseImport", "Recalculating amount for $accountId")
    }

    private fun fetchAccountBalance(accountId: Long): Long {
        db().query(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, arrayOf("SUM(" + BlotterColumns.from_amount + ")"),
                BlotterColumns.from_account_id.toString() + "=? and (" + BlotterColumns.parent_id + "=0 or " + BlotterColumns.is_transfer + "=-1)", arrayOf(accountId.toString()), null, null, null).use { c ->
            return if (c.moveToFirst()) {
                c.getLong(0)
            } else 0
        }
    }

    fun saveRate(r: ExchangeRate) {
        replaceRate(r, r.date)
    }

    fun replaceRate(rate: ExchangeRate, originalDate: Long) {
        val db = db()
        db.beginTransaction()
        try {
            replaceRateInTransaction(rate, originalDate, db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun replaceRateInTransaction(rate: ExchangeRate, originalDate: Long, db: SQLiteDatabase) {
        deleteRateInTransaction(rate.fromCurrencyId, rate.toCurrencyId, originalDate, db)
        saveBothRatesInTransaction(rate, db)
    }

    private fun saveBothRatesInTransaction(r: ExchangeRate, db: SQLiteDatabase) {
        r.date = DateUtils.atMidnight(r.date)
        saveRateInTransaction(db, r)
        saveRateInTransaction(db, r.flip())
    }

    private fun saveRateInTransaction(db: SQLiteDatabase, r: ExchangeRate) {
        db.insert(DatabaseHelper.EXCHANGE_RATES_TABLE, null, r.toValues())
    }

    fun saveDownloadedRates(downloadedRates: List<ExchangeRate>) {
        val db = db()
        db.beginTransaction()
        try {
            for (r in downloadedRates) {
                if (r.isOk) {
                    replaceRateInTransaction(r, r.date, db)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun findRate(fromCurrency: Currency, toCurrency: Currency, date: Long): ExchangeRate? {
        val day = DateUtils.atMidnight(date)
        val c = db().query(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.NORMAL_PROJECTION_WHERE, arrayOf(fromCurrency.id.toString(), toCurrency.id.toString(), day.toString()), null, null, null)
        try {
            if (c.moveToFirst()) {
                return ExchangeRate.fromCursor(c)
            }
        } finally {
            c.close()
        }
        return null
    }

    fun findRates(fromCurrency: Currency): List<ExchangeRate> {
        val rates: MutableList<ExchangeRate> = ArrayList()
        val c = db().query(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.from_currency_id.toString() + "=?", arrayOf(fromCurrency.id.toString()), null, null, ExchangeRateColumns.rate_date.toString() + " desc")
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c))
            }
        } finally {
            c.close()
        }
        return rates
    }

    fun findRates(fromCurrency: Currency, toCurrency: Currency): List<ExchangeRate> {
        val rates: MutableList<ExchangeRate> = ArrayList()
        val c = db().query(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION,
                ExchangeRateColumns.from_currency_id.toString() + "=? and " + ExchangeRateColumns.to_currency_id + "=?", arrayOf(fromCurrency.id.toString(), toCurrency.id.toString()),
                null, null, ExchangeRateColumns.rate_date.toString() + " desc")
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c))
            }
        } finally {
            c.close()
        }
        return rates
    }

    val latestRates: ExchangeRateProvider
        get() {
            val m = LatestExchangeRates()
            val c = db().query(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.LATEST_RATE_PROJECTION, null, null, ExchangeRateColumns.LATEST_RATE_GROUP_BY, null, null)
            fillRatesCollection(m, c)
            return m
        }
    val historyRates: ExchangeRateProvider
        get() {
            val m = HistoryExchangeRates()
            val c = db().query(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, null, null, null, null, null)
            fillRatesCollection(m, c)
            return m
        }

    private fun fillRatesCollection(m: ExchangeRatesCollection, c: Cursor) {
        try {
            while (c.moveToNext()) {
                val r = ExchangeRate.fromCursor(c)
                m.addRate(r)
            }
        } finally {
            c.close()
        }
    }

    fun deleteRate(rate: ExchangeRate) {
        deleteRate(rate.fromCurrencyId, rate.toCurrencyId, rate.date)
    }

    fun deleteRate(fromCurrencyId: Long, toCurrencyId: Long, date: Long) {
        val db = db()
        db.beginTransaction()
        try {
            deleteRateInTransaction(fromCurrencyId, toCurrencyId, date, db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun deleteRateInTransaction(fromCurrencyId: Long, toCurrencyId: Long, date: Long, db: SQLiteDatabase) {
        val d = DateUtils.atMidnight(date)
        db.delete(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE, arrayOf(fromCurrencyId.toString(), toCurrencyId.toString(), d.toString()))
        db.delete(DatabaseHelper.EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE, arrayOf(toCurrencyId.toString(), fromCurrencyId.toString(), d.toString()))
    }

    val accountsTotalInHomeCurrency: Total
        get() {
            val homeCurrency = homeCurrency
            return getAccountsTotal(homeCurrency)
        }

    /**
     * Calculates total in every currency for all accounts
     */
    val accountsTotal: Array<Total>
        get() {
            val accounts = allAccountsList
            val totalsMap: MutableMap<Currency, Total> = HashMap()
            for (account in accounts) {
                if (account.shouldIncludeIntoTotals()) {
                    val currency = account.currency
                    var total = totalsMap[currency]
                    if (total == null) {
                        total = Total(currency)
                        totalsMap[currency] = total
                    }
                    total.balance += account.totalAmount
                }
            }
            val values: Collection<Total> = totalsMap.values
            return values.toTypedArray()
        }

    /**
     * Calculates total in home currency for all accounts
     */
    fun getAccountsTotal(homeCurrency: Currency): Total {
        val rates = latestRates
        val accounts = allAccountsList
        var total = BigDecimal.ZERO
        for (account in accounts) {
            if (account.shouldIncludeIntoTotals()) {
                total = if (account.currency.id == homeCurrency.id) {
                    total.add(BigDecimal.valueOf(account.totalAmount))
                } else {
                    val rate = rates.getRate(account.currency, homeCurrency)
                    if (rate === ExchangeRate.NA) {
                        return Total(homeCurrency, TotalError.lastRateError(account.currency))
                    } else {
                        total.add(BigDecimal.valueOf(rate.rate * account.totalAmount))
                    }
                }
            }
        }
        val result = Total(homeCurrency)
        result.balance = total.toLong()
        return result
    }

    fun findAccountsByNumber(numberEnding: String): List<Long> {
        db().rawQuery(
                "select " + AccountColumns.ID + " from " + DatabaseHelper.ACCOUNT_TABLE +
                        " where " + AccountColumns.NUMBER + " like ?", arrayOf("%$numberEnding")).use { c ->
            val res: MutableList<Long> = ArrayList(c.count)
            while (c.moveToNext()) {
                res.add(c.getLong(0))
            }
            return res
        }
    }

    fun singleCurrencyOnly(): Boolean {
        val currencyId = singleCurrencyId
        return currencyId > 0
    }

    private val singleCurrencyId: Long
        private get() {
            db().rawQuery("select distinct " + AccountColumns.CURRENCY_ID + " from " + DatabaseHelper.ACCOUNT_TABLE +
                    " where " + AccountColumns.IS_INCLUDE_INTO_TOTALS + "=1 and " + AccountColumns.IS_ACTIVE + "=1", null).use { c ->
                if (c.count == 1) {
                    c.moveToFirst()
                    return c.getLong(0)
                }
                return -1
            }
        }

    fun setDefaultHomeCurrency() {
        val homeCurrency = homeCurrency
        val singleCurrencyId = singleCurrencyId
        if (homeCurrency === Currency.EMPTY && singleCurrencyId > 0) {
            val c = get(Currency::class.java, singleCurrencyId)
            c.isDefault = true
            saveOrUpdate(c)
        }
    }

    fun purgeAccountAtDate(account: Account, date: Long) {
        val nearestTransactionId = findNearestOlderTransactionId(account, date)
        if (nearestTransactionId > 0) {
            val db = db()
            db.beginTransaction()
            try {
                val newTransaction = createTransactionFromNearest(account, nearestTransactionId)
                breakSplitTransactions(account, date)
                deleteOldTransactions(account, date)
                insertWithoutUpdatingBalance(newTransaction)
                db.execSQL(INSERT_RUNNING_BALANCE, arrayOf<Any>(account.id, newTransaction.id, newTransaction.dateTime, newTransaction.fromAmount))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun createTransactionFromNearest(account: Account, nearestTransactionId: Long): Transaction {
        val nearestTransaction = get(Transaction::class.java, nearestTransactionId)
        val balance = getAccountBalanceForTransaction(account, nearestTransaction)
        val newTransaction = Transaction()
        newTransaction.fromAccountId = account.id
        newTransaction.dateTime = DateUtils.atDayEnd(nearestTransaction.dateTime)
        newTransaction.fromAmount = balance
        val payee = findOrInsertEntityByTitle(Payee::class.java, context.getString(R.string.purge_account_payee))
        newTransaction.payeeId = payee?.id ?: 0
        newTransaction.status = TransactionStatus.CL
        return newTransaction
    }

    private fun breakSplitTransactions(account: Account, date: Long) {
        val db = db()
        val dayEnd = DateUtils.atDayEnd(date)
        db.execSQL(BREAK_SPLIT_TRANSACTIONS_1, arrayOf<Any>(account.id, dayEnd))
        db.execSQL(BREAK_SPLIT_TRANSACTIONS_2, arrayOf<Any>(account.id, dayEnd))
        db.delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
                + " in (SELECT _id from " + DatabaseHelper.TRANSACTION_TABLE + " where " + TransactionColumns.datetime + "<=?)", arrayOf(dayEnd.toString()))
    }

    fun deleteOldTransactions(account: Account, date: Long) {
        val db = db()
        val dayEnd = DateUtils.atDayEnd(date)
        db.delete("transactions", "from_account_id=? and datetime<=? and is_template=0", arrayOf(account.id.toString(), dayEnd.toString()))
        db.delete("running_balance", "account_id=? and datetime<=?", arrayOf(account.id.toString(), dayEnd.toString()))
    }

    fun getAccountBalanceForTransaction(a: Account, t: Transaction): Long {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id=? and transaction_id=?", arrayOf(a.id.toString(), t.id.toString()))
    }

    fun findNearestOlderTransactionId(account: Account, date: Long): Long {
        return DatabaseUtils.rawFetchId(this,
                "select _id from v_blotter where from_account_id=? and datetime<=? order by datetime desc limit 1", arrayOf(account.id.toString(), DateUtils.atDayEnd(date).toString()))
    }

    fun findLatestTransactionDate(accountId: Long): Long {
        return DatabaseUtils.rawFetchLongValue(this,
                "select datetime from running_balance where account_id=? order by datetime desc limit 1", arrayOf(accountId.toString()))
    }

    private fun updateAccountLastTransactionDate(accountId: Long) {
        if (accountId <= 0) {
            return
        }
        val lastTransactionDate = findLatestTransactionDate(accountId)
        db().execSQL(ACCOUNT_LAST_TRANSACTION_DATE_UPDATE, arrayOf<Any>(lastTransactionDate, accountId))
    }

    fun updateAccountsLastTransactionDate() {
        val accounts = allAccountsList
        for (account in accounts) {
            updateAccountLastTransactionDate(account.id)
        }
    }

    fun restoreSystemEntities() {
        val db = db()
        db.beginTransaction()
        try {
            restoreCategories()
            restoreAttributes()
            restoreProjects()
            restoreLocations()
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("Financier", "Unable to restore system entities", e)
        } finally {
            db.endTransaction()
        }
    }

    private fun restoreCategories() {
        reInsertEntity(noCategory())
        reInsertEntity(splitCategory())
        val tree = getCategoriesTree(false)
        tree.reIndex()
        updateCategoryTree(tree)
    }

    private fun restoreAttributes() {
        reInsertEntity(Attribute.deleteAfterExpired())
    }

    private fun restoreProjects() {
        reInsertEntity(Project.noProject())
    }

    private fun restoreLocations() {
        reInsertEntity(MyLocation.currentLocation())
    }

    fun getLastRunningBalanceForAccount(account: Account): Long {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id=? order by datetime desc, transaction_id desc limit 1", arrayOf(account.id.toString()))
    }

    companion object {
        // ===================================================================
        // ACCOUNT
        // ===================================================================
        private val UPDATE_ORPHAN_TRANSACTIONS_1 = "UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " +
                TransactionColumns.to_account_id + "=0, " +
                TransactionColumns.to_amount + "=0 " +
                "WHERE " + TransactionColumns.to_account_id + "=?"
        private val UPDATE_ORPHAN_TRANSACTIONS_2 = "UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " +
                TransactionColumns.from_account_id + "=" + TransactionColumns.to_account_id + ", " +
                TransactionColumns.from_amount + "=" + TransactionColumns.to_amount + ", " +
                TransactionColumns.to_account_id + "=0, " +
                TransactionColumns.to_amount + "=0, " +
                TransactionColumns.parent_id + "=0 " +
                "WHERE " + TransactionColumns.from_account_id + "=? AND " +
                TransactionColumns.to_account_id + ">0"

        @JvmStatic
        fun enhanceFilterForAccountBlotter(filter: WhereFilter?): WhereFilter {
            val accountFilter = WhereFilter.copyOf(filter)
            accountFilter.put(Criteria.raw(BlotterColumns.parent_id.toString() + "=0 OR " + BlotterColumns.is_transfer + "=-1"))
            return accountFilter
        }

        private const val LOCATION_COUNT_UPDATE = ("UPDATE " + DatabaseHelper.LOCATIONS_TABLE
                + " SET count=count+(?) WHERE _id=?")
        private const val ACCOUNT_LAST_CATEGORY_UPDATE = ("UPDATE " + DatabaseHelper.ACCOUNT_TABLE
                + " SET " + AccountColumns.LAST_CATEGORY_ID + "=? "
                + " WHERE " + AccountColumns.ID + "=?")
        private const val ACCOUNT_LAST_ACCOUNT_UPDATE = ("UPDATE " + DatabaseHelper.ACCOUNT_TABLE
                + " SET " + AccountColumns.LAST_ACCOUNT_ID + "=? "
                + " WHERE " + AccountColumns.ID + "=?")
        private const val PAYEE_LAST_CATEGORY_UPDATE = ("UPDATE " + DatabaseHelper.PAYEE_TABLE
                + " SET last_category_id=(?) WHERE _id=?")
        private const val CATEGORY_LAST_LOCATION_UPDATE = ("UPDATE " + DatabaseHelper.CATEGORY_TABLE
                + " SET last_location_id=(?) WHERE _id=?")
        private const val CATEGORY_LAST_PROJECT_UPDATE = ("UPDATE " + DatabaseHelper.CATEGORY_TABLE
                + " SET last_project_id=(?) WHERE _id=?")
        private const val ACCOUNT_TOTAL_AMOUNT_UPDATE = ("UPDATE " + DatabaseHelper.ACCOUNT_TABLE
                + " SET " + AccountColumns.TOTAL_AMOUNT + "=" + AccountColumns.TOTAL_AMOUNT + "+(?) "
                + " WHERE " + AccountColumns.ID + "=?")
        private const val INSERT_RUNNING_BALANCE = "insert or replace into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)"
        private const val UPDATE_RUNNING_BALANCE = "update running_balance set balance = balance+(?) where account_id = ? and datetime > ?"
        private const val DELETE_RUNNING_BALANCE = "delete from running_balance where account_id = ? and transaction_id = ?"
        private val GET_PARENT_SQL = ("(SELECT "
                + "parent." + CategoryColumns._id + " AS " + CategoryColumns._id
                + " FROM "
                + DatabaseHelper.CATEGORY_TABLE + " AS node" + ","
                + DatabaseHelper.CATEGORY_TABLE + " AS parent "
                + " WHERE "
                + " node." + CategoryColumns.left + " BETWEEN parent." + CategoryColumns.left + " AND parent." + CategoryColumns.right
                + " AND node." + CategoryColumns._id + "=?"
                + " AND parent." + CategoryColumns._id + "!=?"
                + " ORDER BY parent." + CategoryColumns.left + " DESC)")
        private val INSERT_CATEGORY_UPDATE_RIGHT = "UPDATE " + DatabaseHelper.CATEGORY_TABLE + " SET " + CategoryColumns.right + "=" + CategoryColumns.right + "+2 WHERE " + CategoryColumns.right + ">?"
        private val INSERT_CATEGORY_UPDATE_LEFT = "UPDATE " + DatabaseHelper.CATEGORY_TABLE + " SET " + CategoryColumns.left + "=" + CategoryColumns.left + "+2 WHERE " + CategoryColumns.left + ">?"
        private val CATEGORY_UPDATE_CHILDREN_TYPES = "UPDATE " + DatabaseHelper.CATEGORY_TABLE + " SET " + CategoryColumns.type + "=? WHERE " + CategoryColumns.left + ">? AND " + CategoryColumns.right + "<?"
        private val DELETE_CATEGORY_UPDATE1 = ("UPDATE " + DatabaseHelper.TRANSACTION_TABLE
                + " SET " + TransactionColumns.category_id + "=0 WHERE "
                + TransactionColumns.category_id + " IN ("
                + "SELECT " + CategoryColumns._id + " FROM " + DatabaseHelper.CATEGORY_TABLE + " WHERE "
                + CategoryColumns.left + " BETWEEN ? AND ?)")
        private val DELETE_CATEGORY_UPDATE2 = ("UPDATE " + DatabaseHelper.CATEGORY_TABLE
                + " SET " + CategoryColumns.left + "=(CASE WHEN " + CategoryColumns.left + ">%s THEN "
                + CategoryColumns.left + "-%s ELSE " + CategoryColumns.left + " END),"
                + CategoryColumns.right + "=" + CategoryColumns.right + "-%s"
                + " WHERE " + CategoryColumns.right + ">%s")
        private val WHERE_CATEGORY_ID = CategoryColumns._id.toString() + "=?"
        private val UPDATE_LAST_RECURRENCE = "UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " + TransactionColumns.last_recurrence + "=? WHERE " + TransactionColumns._id + "=?"
        private val SUM_FROM_AMOUNT = arrayOf("sum(from_amount)")
        private val BREAK_SPLIT_TRANSACTIONS_1 = UPDATE_ORPHAN_TRANSACTIONS_1 + " " +
                "AND " + TransactionColumns.datetime + "<=?"
        private val BREAK_SPLIT_TRANSACTIONS_2 = UPDATE_ORPHAN_TRANSACTIONS_2 + " " +
                "AND " + TransactionColumns.datetime + "<=?"
        private const val ACCOUNT_LAST_TRANSACTION_DATE_UPDATE = ("UPDATE " + DatabaseHelper.ACCOUNT_TABLE
                + " SET " + AccountColumns.LAST_TRANSACTION_DATE + "=? WHERE " + AccountColumns.ID + "=?")
    }
}