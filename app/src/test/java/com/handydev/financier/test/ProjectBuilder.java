package com.handydev.financier.test;

import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.model.Project;

public class ProjectBuilder {
    private final DatabaseAdapter db;
    private final Project p = new Project();

    public static ProjectBuilder withDb(DatabaseAdapter db) {
        return new ProjectBuilder(db);
    }

    private ProjectBuilder(DatabaseAdapter db) {
        this.db = db;
        this.p.isActive = false;
    }

    public ProjectBuilder id(long v) {
        p.id = v;
        return this;
    }

    public ProjectBuilder title(String v) {
        p.title = v;
        return this;
    }

    public ProjectBuilder setActive() {
        p.isActive = true;
        return this;
    }

    public Project create() {
        db.saveOrUpdate(p);
        return p;
    }


}
