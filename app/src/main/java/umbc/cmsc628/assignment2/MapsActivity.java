package umbc.cmsc628.assignment2;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG = MapsActivity.class.getName();
    private static final int MY_PERMISSIONS_GET_FINE_LOCATION = 0;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private UserLocationDbHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper = new UserLocationDbHelper(this);
        db = dbHelper.getWritableDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.d(TAG, "Checking permissions");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_GET_FINE_LOCATION);
            return;
        }
        Log.d(TAG, "Requesting location updates");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case MY_PERMISSIONS_GET_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    onResume();
                }
                // else permission denied
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d(TAG, "Requesting location updates");
        locationManager.removeUpdates(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng sydney = new LatLng(40, 40);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: lat=" +
                Double.toString(location.getLatitude()) +
                " long=" + Double.toString(location.getLatitude()));
        new UpdateLocationTask().execute(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class UpdateLocationTask extends AsyncTask<Location, Integer, Long> {

        @Override
        protected Long doInBackground(Location... locs) {
            long lastRowId = -1;

            for (Location loc : locs) {
                lastRowId = writeLocationToDatabase(loc);
            }

            return lastRowId;
        }

        @Override
        protected void onPostExecute(Long lastRowInserted) {
            super.onPostExecute(lastRowInserted);

            LatLng lastLatLng = getLastLatLng();
            if (lastLatLng != null) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLatLng, 10);
                mMap.animateCamera(cameraUpdate);
            } else {
                Log.e(TAG, "No last lat/long found");
            }
        }

        private long writeLocationToDatabase(Location loc) {
            ContentValues values = createLocationContentValues(loc);

            long newRowId;
            newRowId = db.insert(UserLocationContract.UserLocationEntry.TABLE_NAME,
                    null,
                    values);

            return newRowId;
        }

        private ContentValues createLocationContentValues(Location loc) {
            ContentValues values = new ContentValues();

            String timestamp = new Date().toString();
            String latitudeString = Double.toString(loc.getLatitude());
            String longitudeString = Double.toString(loc.getLongitude());

            values.put(UserLocationContract.UserLocationEntry.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(UserLocationContract.UserLocationEntry.COLUMN_NAME_LATITUDE, latitudeString);
            values.put(UserLocationContract.UserLocationEntry.COLUMN_NAME_LONGITUDE, longitudeString);
            return values;
        }

        private LatLng getLastLatLng() {
            Cursor results = dbHelper.getLastLocation(db);
            if (results.moveToFirst()) {
                double latitude = results.getDouble(1);
                double longitude = results.getDouble(2);
                results.close();
                return new LatLng(latitude, longitude);
            }
            return null;
        }
    }
}
