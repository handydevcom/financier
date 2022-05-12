package com.handydev.financier.test;

import java.util.*;

import android.database.sqlite.SQLiteDatabase;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.db.DatabaseHelper;
import com.handydev.financier.model.Attribute;
import com.handydev.financier.model.Category;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/28/11 11:29 PM
 */
public class CategoryBuilder {

    private final DatabaseAdapter db;
    private final Category category = new Category();

    /**
     * A
     * - A1
     * -- AA1
     * - A2
     * B
     */
    public static Map<String, Category> createDefaultHierarchy(DatabaseAdapter db) {
        Category a = new CategoryBuilder(db).withTitle("A").create();
        Category a1 = new CategoryBuilder(db).withParent(a).withTitle("A1").create();
        new CategoryBuilder(db).withParent(a1).withTitle("AA1")
                .withAttributes(
                        AttributeBuilder.withDb(db).createTextAttribute("attr1"),
                        AttributeBuilder.withDb(db).createNumberAttribute("attr2")
                ).create();
        new CategoryBuilder(db).withParent(a).withTitle("A2").create();
        new CategoryBuilder(db).withTitle("B").income().create();
        return allCategoriesAsMap(db);
    }

    /**
     * Set up a corrupted category tree, where walking into it leads to a StackOverflow
     *
     * RECURSIVE
     * - LEVEL_1
     * -- RECURSIVE
     * --- LEVEL_1
     * ---- ... StackOverflow
     */
    public static void createBrokenHierarchy(DatabaseAdapter db) {
        SQLiteDatabase sqlite = db.db();

        sqlite.beginTransaction();
        sqlite.execSQL("insert into category (_id, title, 'left', 'right', is_active) values (11, 'ONE', 11, 12, 1)");
        sqlite.execSQL("insert into category (_id, title, 'left', 'right', is_active) values (12, 'TWO', 11, 12, 1)");
        sqlite.execSQL("insert into category (_id, title, 'left', 'right', is_active) values (13, 'THREE', 12, 13, 1)");
        sqlite.setTransactionSuccessful();
        sqlite.endTransaction();

        //new CategoryBuilder(db).withTitle("IGNORE").create(); // inserting manually to the DB doesn't commit the categories for some reason
    }

    private CategoryBuilder withAttributes(Attribute...attributes) {
        category.attributes = Arrays.asList(attributes);
        return this;
    }

    public static Map<String, Category> allCategoriesAsMap(DatabaseAdapter db) {
        HashMap<String, Category> map = new HashMap<String, Category>();
        List<Category> categories = db.getAllCategoriesList();
        for (Category category : categories) {
            category.attributes = db.getAttributesForCategory(category.id);
            map.put(category.title, category);
        }
        return map;
    }

    public static Category split(DatabaseAdapter db) {
        return db.getCategoryWithParent(Category.SPLIT_CATEGORY_ID);
    }

    public static Category noCategory(DatabaseAdapter db) {
        return db.getCategoryWithParent(Category.NO_CATEGORY_ID);
    }

    private CategoryBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public CategoryBuilder withTitle(String title) {
        category.title = title;
        return this;
    }

    public CategoryBuilder withParent(Category parent) {
        category.parent = parent;
        return this;
    }

    private CategoryBuilder income() {
        category.makeThisCategoryIncome();
        return this;
    }

    public Category create() {
        category.id = db.insertOrUpdate(category, category.attributes);
        return category;
    }

}
