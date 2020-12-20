package com.handydev.financier.db

import com.handydev.financier.model.Category

object CategoriesCache {
    var categoriesList = ArrayList<Category>()
    fun updateCache(categories: ArrayList<Category>) {
        categoriesList = categories
    }

    fun getCategory(id: Long): Category? {
        return categoriesList.firstOrNull { it.id == id }
    }
}