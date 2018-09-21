package com.test.arcgis;
/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private Spinner mSpinner;

    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    private boolean isFirstLatLng;
    private Point oldTrailPoint;
    private PointCollection mPoints;
    private Polyline mPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud4530363064,none,E9PJD4SZ8Y80C2EN0097");//设置arcgis通行证key值
        // Get the Spinner from layout
        mSpinner = (Spinner) findViewById(R.id.spinner);
        isFirstLatLng = true;
        mPoints = new PointCollection(SpatialReferences.getWgs84());//84坐标系
        // Get the MapView from layout and set a map with the BasemapType Imagery
        mMapView = (MapView) findViewById(R.id.mapView);
        ArcGISMap mMap = new ArcGISMap(Basemap.createImagery());
        mMapView.setMap(mMap);

        // get the MapView's LocationDisplay
        mLocationDisplay = mMapView.getLocationDisplay();
        /*************************************************/
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 3);
        GraphicsOverlay overlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(overlay);
        overlay.getGraphics().add(new Graphic(createPolyline(), lineSymbol));
        mMapView.setViewpointGeometryAsync(createEnvelope(), 10);
        Toast.makeText(this, createDistance() + "m", Toast.LENGTH_LONG).show();


        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                // If LocationDisplay started OK, then continue.
                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                // No error is reported, then continue.
                if (dataSourceStatusChangedEvent.getError() == null)
                    return;

                // If an error is found, handle the failure to start.
                // Check permissions to see if failure may be due to lack of permissions.
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                    // Update UI to reflect that the location display did not actually start
                    mSpinner.setSelection(0, true);
//                        startListeners();

                }
            }
        });


        // Populate the list for the Location display options for the spinner's Adapter
        ArrayList<ItemData> list = new ArrayList<>();
        list.add(new ItemData("停止", R.drawable.arcgisruntime_location_display_acquiring_symbol));
        list.add(new ItemData("开", R.drawable.arcgisruntime_location_display_default_symbol));
        list.add(new ItemData("重新中心", R.drawable.arcgisruntime_location_display_acquiring_symbol));
        list.add(new ItemData("导航", R.drawable.arcgisruntime_location_display_course_symbol));
        list.add(new ItemData("指南针", R.drawable.arcgisruntime_location_display_compass_symbol));

        SpinnerAdapter adapter = new SpinnerAdapter(this, R.layout.spinner_layout, R.id.txt, list);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        // Stop Location Display
                        if (mLocationDisplay.isStarted())
                            mLocationDisplay.stop();
                        break;
                    case 1:
                        // Start Location Display
                        if (!mLocationDisplay.isStarted())
                            mLocationDisplay.startAsync();
                        break;
                    case 2:
                        // Re-Center MapView on Location
                        // AutoPanMode - Default: In this mode, the MapView attempts to keep the location symbol on-screen by
                        // re-centering the location symbol when the symbol moves outside a "wander extent". The location symbol
                        // may move freely within the wander extent, but as soon as the symbol exits the wander extent, the MapView
                        // re-centers the map on the symbol.
                        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                        if (!mLocationDisplay.isStarted())
                            mLocationDisplay.startAsync();
                        break;
                    case 3:
                        // Start Navigation Mode
                        // This mode is best suited for in-vehicle navigation.
                        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
                        if (!mLocationDisplay.isStarted())
                            mLocationDisplay.startAsync();
                        break;
                    case 4:
                        // Start Compass Mode
                        // This mode is better suited for waypoint navigation when the user is walking.
                        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                        if (!mLocationDisplay.isStarted())
                            mLocationDisplay.startAsync();
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
    }

    private void startListeners() {
        mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                LocationDataSource.Location location = locationChangedEvent.getLocation();
                Point newTrailPoint = location.getPosition();//GPS坐标
                if (isFirstLatLng) {
                    //记录第一次的定位信息
                    oldTrailPoint = newTrailPoint;
                    mPoints.add(newTrailPoint);
                    Log.i("sss", "points" + mPoints.toString());
                    isFirstLatLng = false;
                }

                //位置有变化
                if (!oldTrailPoint.equals(newTrailPoint)) {
                    mPoints.add(newTrailPoint);
                    Log.i("sss", "points" + mPoints.toString());

                    mPolyline = new Polyline(mPoints);//点画线
                    mMapView.setViewpointCenterAsync(newTrailPoint);

                    GraphicsOverlay overlay = new GraphicsOverlay();
                    mMapView.getGraphicsOverlays().add(overlay);

                    overlay.getGraphics().clear();//重绘线
                    SimpleLineSymbol lineSym = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 5);
                    Graphic graphicTrail = new Graphic(mPolyline, lineSym);
                    overlay.getGraphics().add(graphicTrail);

                    oldTrailPoint = newTrailPoint;
                }
            }
        });
    }

    /**
     * 开始轨迹记录
     */
//    private void startTrail() {
//        if (!mLocationDisplay.isStarted()) {
//            Toast.makeText(this, "请先打开定位", Toast.LENGTH_SHORT).show();
//        } else {
//            //执行方法1（对应功能1)
//            fab.setImageResource(R.drawable.ic_close);
//            Toast.makeText(MainActivity.this, "开始轨迹记录...", Toast.LENGTH_SHORT).show();
//            //创建数据表
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd+HH:mm:ss");
//            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//            oldTableName = formatter.format(curDate);
//            mTrailPoint = new TrailPointDAO(db, oldTableName);//创建TrailPointDAO实例，进行CRUD操作,传入当前时间作为表名
//
//            mLocationDisplay.addLocationChangedListener(locationListener);
//            flag = (flag + 1) % 2;//其余得到循环执行上面3个不同的功能,1%3=1  2%3=2  3%3=0
//        }
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission was granted. This would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again.
            mLocationDisplay.startAsync();
        } else {
            // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
            // request permission UX will be shown again, option should be shown to allow never showing the UX again.
            // Alternative would be to disable functionality so request is not shown again.
            Toast.makeText(MainActivity.this, "", Toast
                    .LENGTH_SHORT).show();

            // Update UI to reflect that the location display did not actually start
            mSpinner.setSelection(0, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        mLocationDisplay.startAsync();
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }

    private Polyline createPolyline() {

        // create a Polyline from a PointCollection
        PointCollection borderCAtoNV = new PointCollection(SpatialReferences.getWgs84());
        borderCAtoNV.add(-119.992, 41.989);
        borderCAtoNV.add(-119.994, 38.994);
        borderCAtoNV.add(-114.620, 35.0);
        Polyline polyline = new Polyline(borderCAtoNV);

        return polyline;
    }

    private double createDistance() {

        // create a Polyline from a PointCollection
        PointCollection borderCAtoNV = new PointCollection(SpatialReferences.getWgs84());
        Point p1 = new Point(-3.1883, 55.9533, 0.0, SpatialReferences.getWgs84());
        Point p2 = new Point(39.2083, -6.7924, 0.0, SpatialReferences.getWgs84());
        Point p3 = new Point(-114.620, 35.0, 0.0, SpatialReferences.getWgs84());
        // Create a world equidistant cylindrical spatial reference for measuring planar distance.
        SpatialReference equidistantSpatialRef = SpatialReference.create(54002);

// Project the points from geographic to the projected coordinate system.
        Point edinburghProjected = (Point) GeometryEngine.project(p1, equidistantSpatialRef);
        Point darEsSalaamProjected = (Point) GeometryEngine.project(p2, equidistantSpatialRef);


        double d1 = GeometryEngine.distanceBetween(edinburghProjected, darEsSalaamProjected);
        double d2 = GeometryEngine.distanceBetween(p2, p3);
        return d1;
    }

    private Envelope createEnvelope() {


        // create an Envelope using minimum and maximum x,y coordinates and a SpatialReference
        Envelope envelope = new Envelope(-123.0, 33.5, -101.0, 48.0, SpatialReferences.getWgs84());


        return envelope;
    }

    public void jump(View view) {
        startActivity(new Intent(this, Main2Activity.class));
    }
}
