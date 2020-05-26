package com.handydev.financier.export.drive;

import com.google.android.gms.common.ConnectionResult;

public class DriveConnectionFailed {

    public final ConnectionResult connectionResult;

    public DriveConnectionFailed(ConnectionResult connectionResult) {
        this.connectionResult = connectionResult;
    }

}
