package umbc.cmsc628.assignment2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, PlaceSelectionListener {

    private static final String TAG = MapsActivity.class.getName();
    private static final int MY_PERMISSIONS_GET_FINE_LOCATION = 0;
    private static final int ZOOM_LEVEL = 15;
    private static final int GEOFENCE_RADIUS = 200;


    private boolean insideRadius;
    private LatLng placeLatLng;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    //.addApi(Places.GEO_DATA_API)
                    //.addApi(Places.PLACE_DETECTION_API)
                    //.enableAutoManage(this, this)
                    .build();
        }

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
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
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
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

        // Last known location may not be known yet so just move to approximately where the user is
        // likely to be in the meantime
        LatLng maryland = new LatLng(40, 40);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(maryland));
    }

    @Override
    public void onLocationChanged(Location location) {
        //Clear the map
        mMap.clear();
        Log.d(TAG, "Location changed: lat=" +
                Double.toString(location.getLatitude()) +
                " long=" + Double.toString(location.getLatitude()));
        //Set the last known location
        lastKnownLocation = location;
        LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
        //Add the markers
        setMarkerOnLocation(newLocation);
        //Only add the place marker and circle if there is a place set
        if(placeLatLng != null) {
            setMarkerOnLocation(placeLatLng);
            setCircle(placeLatLng);
        }
        //Move the camera to the new location
        zoomCamera();

        //Check the distance of the location from the specified place
        checkDistance();
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

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "On connected called.");
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
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (lastKnownLocation != null) {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            Log.d(TAG, "Last known location lat = " + Double.toString(latitude) + " long = " + Double.toString(longitude));
            LatLng lastLatLng = new LatLng(latitude, longitude);

            setMarkerOnLocation(lastLatLng);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLatLng, ZOOM_LEVEL);
            mMap.animateCamera(cameraUpdate);
        } else {
            Log.e(TAG, "Could not get last known location");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    //Set the marker location
    private void setMarkerOnLocation(LatLng loc) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(loc);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        mMap.addMarker(markerOptions);
    }

    //Method to draw a circle for the place
    private void setCircle(LatLng loc) {
        CircleOptions circleOptions = new CircleOptions().center(loc).radius(GEOFENCE_RADIUS).fillColor(Color.GREEN).strokeColor(Color.GREEN).strokeWidth(2);
        mMap.addCircle(circleOptions);
    }

    @Override
    public void onPlaceSelected(Place place) {
        Log.i(TAG, "Place: " + place.getName());
        //Get the place data
        placeLatLng = place.getLatLng();
        //Clear the map and then set the markers and draw the circle
        mMap.clear();
        setMarkerOnLocation(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        setMarkerOnLocation(placeLatLng);
        setCircle(placeLatLng);
        //Check the distance of the place from the location
        checkDistance();

        //Zoom the camera
        zoomCamera();

    }

    public void zoomCamera() {
        //Zoom camera to show both the place point and the last known location
        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        if(lastKnownLocation != null)
            bc.include(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        if(placeLatLng != null)
            bc.include(placeLatLng);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bc.build(), 200);
        mMap.animateCamera(cameraUpdate);
    }


    public void checkDistance() {
        float[] distance = new float[2];
        //Find the distance between the last known location and the place
        Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                placeLatLng.latitude, placeLatLng.longitude, distance);
        //If this distance greater than the radius, the location is not within the radius
        if(distance[0] > GEOFENCE_RADIUS) {
            if(insideRadius)
                Toast.makeText(getApplicationContext(), "Leaving the 200 meter radius.", Toast.LENGTH_LONG).show();
            insideRadius = false;
        }
        //Otherwise the location is within the radius from the place
        else {
            if(!insideRadius)
                Toast.makeText(getApplicationContext(), "Within the 200 meter radius.", Toast.LENGTH_LONG).show();
            insideRadius = true;
        }
    }

    @Override
    public void onError(Status status) {
        Log.e(TAG, "An error occurred " + status);
    }
}
