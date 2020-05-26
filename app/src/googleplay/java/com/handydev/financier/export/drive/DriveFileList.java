package com.handydev.financier.export.drive;

import com.google.api.services.drive.model.FileList;

public class DriveFileList {

    public final FileList files;

    public DriveFileList(FileList files) {
        this.files = files;
    }

}
