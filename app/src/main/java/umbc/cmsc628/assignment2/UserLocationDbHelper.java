package umbc.cmsc628.assignment2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserLocationDbHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UserLocationContract.UserLocationEntry.TABLE_NAME + " (" +
                    UserLocationContract.UserLocationEntry.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + " PRIMARY KEY," +
                    UserLocationContract.UserLocationEntry.COLUMN_NAME_LATITUDE + TEXT_TYPE + COMMA_SEP +
                    UserLocationContract.UserLocationEntry.COLUMN_NAME_LONGITUDE + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UserLocationContract.UserLocationEntry.TABLE_NAME;

    private static final String SQL_GET_LAST_ENTRY_BY_TIMESTAMP =
            "SELECT * FROM " + UserLocationContract.UserLocationEntry.TABLE_NAME +
            "ORDER BY " + UserLocationContract.UserLocationEntry.COLUMN_NAME_TIMESTAMP +
            "DESC LIMIT 1";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "UserLocation.db";

    public UserLocationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public Cursor getLastLocation(SQLiteDatabase db) {
        String[] tableColumns = new String[] {
                UserLocationContract.UserLocationEntry.COLUMN_NAME_TIMESTAMP,
                UserLocationContract.UserLocationEntry.COLUMN_NAME_LATITUDE,
                UserLocationContract.UserLocationEntry.COLUMN_NAME_LONGITUDE
        };
        String orderBy = UserLocationContract.UserLocationEntry.COLUMN_NAME_TIMESTAMP + " DESC";
        String limit = "1";

        return db.query(UserLocationContract.UserLocationEntry.TABLE_NAME,
                tableColumns,
                null, // where
                null, // where args
                null, //groupBy
                null, //having
                orderBy,
                limit);
    }
}
