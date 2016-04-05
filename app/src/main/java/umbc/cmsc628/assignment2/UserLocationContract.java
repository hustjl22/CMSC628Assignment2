package umbc.cmsc628.assignment2;

import android.provider.BaseColumns;

public final class UserLocationContract {

    public UserLocationContract() {
    }

    public static abstract class UserLocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "userLoc";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
