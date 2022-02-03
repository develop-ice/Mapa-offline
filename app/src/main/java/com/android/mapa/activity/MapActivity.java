package com.android.mapa.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.android.mapa.R;
import com.android.mapa.map.MyMarker;
import com.android.mapa.map.PlaceMarker;
import com.android.mapa.model.Place;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.android.mapa.map.Points.latLongCmg;
import static com.android.mapa.map.Points.latLongParqueAgramonte;

public class MapActivity extends AppCompatActivity {

    // Ruta donde se almacena el mapa
    public static String MAP_FILE;

    // Permissions
    private static final int REQUEST_LOCATION = 101;
    private static final int REQUEST_SD = 102;

    // Map y Markers
    private MyMarker marker; // Marcador de posicion del usuario
    private Double userLongitud, userLatitud; // lat y lon del usuario via GPS
    private LocationManager locManager;
    private MapView mapView;
    private String provider = LocationManager.GPS_PROVIDER;
    private ArrayList<PlaceMarker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Importante para que funcione el mapa justo antes del "setContentView()"
        AndroidGraphicFactory.createInstance(getApplication());
        setContentView(R.layout.activity_main);

        initViews();

        copyMapIntoDevice();

        setupMap();

        setupMarckers();

        setupGPS();

    }

    /**
     * Init el FloatingActionButton de la ubicacion
     */
    private void initViews() {
        // UI
        FloatingActionButton fab = findViewById(R.id.fab);
        mapView = findViewById(R.id.mapView);

        // FAB onCLick
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userLatitud != null && userLongitud != null) {
                    // si se ha encontrado la lat y long, se mueve el foco a la posicion obtenida del GPS
                    byte zoom = mapView.getModel().mapViewPosition.getZoomLevel();
                    mapView.getModel().mapViewPosition.animateTo(new LatLong(userLatitud, userLongitud));
                    mapView.getModel().mapViewPosition.setZoomLevel(zoom);
                } else {
                    // si lat y long no se han encontrado, se muestra "NO SE HA ENCONTRADO SU POSICION"
                    Toast.makeText(getApplicationContext(), getString(R.string.message2), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Copiar el mapa desde las assets al dispositivo
     */
    private void copyMapIntoDevice() {

        // Check si los permisos estan ok
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SD);

        }

        AssetManager am = getAssets();
        try

        {
            File dir = new File(Environment.getExternalStorageDirectory() + "/mapa/");
            if (!dir.exists()) {
                dir.mkdir();
            }
            // se remueve el .mp3 por .map
            String fileName = "cuba";
            MAP_FILE = Environment.getExternalStorageDirectory() + "/mapa/" + fileName + ".map";
            File destinationFile = new File(MAP_FILE);
            if (!destinationFile.exists()) {
                InputStream in = am.open(fileName + ".mp3");
                FileOutputStream f = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[1024];
                int len1;
                while ((len1 = in.read(buffer)) > 0) {
                    f.write(buffer, 0, len1);
                }
                f.close();
            }
        } catch (
                Exception e)

        {
            Toast.makeText(getApplicationContext(), getString(R.string.message_toast_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Configurar el mapView
     */

    private void setupMap() {
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.getModel().mapViewPosition.setZoomLevel((byte) 12);
        mapView.getMapZoomControls().setZoomLevelMin((byte) 8);
        mapView.getMapZoomControls().setZoomLevelMax((byte) 16);
        mapView.getModel().mapViewPosition.setCenter(latLongCmg);
        TileCache tileCache = AndroidUtil.createTileCache(getApplicationContext(), "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor());
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache,
                mapView.getModel().mapViewPosition, false, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setMapFile(new File(MAP_FILE));
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    /**
     * Añadir marcadores y poligonos
     */
    private void setupMarckers() {
        paintMarkers();
        addPolygon();
        addCircleFix();
        addCircle();
        addListMap();
    }

    /**
     * add circle
     */
    private void addCircle() {
        Circle circle = new Circle(latLongParqueAgramonte, 100, Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 5,
                Style.STROKE), null) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                return true;
            }
        };
        mapView.getLayerManager().getLayers().add(circle);
    }

    /**
     * add circle fix
     */
    private void addCircleFix() {
        FixedPixelCircle tappableCircle = new FixedPixelCircle(
                latLongCmg,
                15,
                Utils.createPaint(
                        AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN),
                        0, Style.FILL), null) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (this.contains(viewPosition, tapPoint)) {
                    Toast.makeText(MapActivity.this,
                            "Círculo -> fix " + geoPoint.toString(),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
        mapView.getLayerManager().getLayers().add(tappableCircle);
    }

    /**
     * add polygon
     */
    private void addPolygon() {
        Paint paintStroke = Utils.createPaint(AndroidGraphicFactory.INSTANCE
                        .createColor(Color.BLUE), 4,
                Style.STROKE);
        paintStroke.setDashPathEffect(new float[]{15, 5});
        paintStroke.setStrokeWidth(5);
        paintStroke.setStrokeWidth(3);
        Polyline line = new Polyline(paintStroke,
                AndroidGraphicFactory.INSTANCE);

        // Coordenadas Parque Casino
        List<LatLong> geoPoints = line.getLatLongs();
        double[] latitudes = {21.3741801, 21.3761986, 21.376875, 21.3781827, 21.3779202, 21.376052, 21.3741801};
        double[] longitudes = {-77.9103996, -77.9132277, -77.9136152, -77.9127224, -77.9117529, -77.9086728, -77.9103996};

        for (int i = 0; i < latitudes.length; i++) {
            geoPoints.add(new LatLong(latitudes[i], longitudes[i]));
        }

        mapView.getLayerManager().getLayers().add(line);
    }

    /**
     * Añadir los marcadores
     */
    private void paintMarkers() {
        Place place1 = new Place("Place1", "Descripción1...", 21.3919882, -77.919945);
        Place place2 = new Place("Place2", "Descripción2...", 21.3829782, -77.929845);
        Place place3 = new Place("Place3", "Descripción3...", 21.3739682, -77.939745);

        PlaceMarker placeMarker1 = new PlaceMarker(this, new LatLong(place1.getLatitud(), place1.getLongitud()), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.drawable.img_lugar)), 0, 0, place1);
        PlaceMarker placeMarker2 = new PlaceMarker(this, new LatLong(place2.getLatitud(), place2.getLongitud()), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.drawable.img_lugar)), 0, 0, place2);
        PlaceMarker placeMarker3 = new PlaceMarker(this, new LatLong(place3.getLatitud(), place3.getLongitud()), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.drawable.img_lugar)), 0, 0, place3);

        markers.add(placeMarker1);
        markers.add(placeMarker2);
        markers.add(placeMarker3);
    }

    /**
     * añadir los markers
     */
    private void addListMap() {
        for (int i = 0; i < markers.size(); i++) {
            mapView.getLayerManager().getLayers().add(markers.get(i));
        }
    }

    /**
     * GPS
     */
    private void setupGPS() {
        // Permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);

        locManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locManager.getBestProvider(criteria, true);

        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            getLocation();
        }

    }

    /**
     * Dialogo para abrir el GPS
     */
    private void OnGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.title_message_dialogo)).setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //click en si
                        Toast.makeText(MapActivity.this, getString(R.string.message_dialogo), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    //click en no
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Obtener la ubicacion
     */
    private void getLocation() {
        // Check si los permisos estan ok
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        } else {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 1, locationListener);
            Location locationGPS = locManager.getLastKnownLocation(provider);
            if (locationGPS != null) {
                paintMyMarker(locationGPS);
            }
        }

    }

    /**
     * metodo para añadir el marcador de mi posicion
     */
    private void paintMyMarker(Location locationGPS) {

        if (marker != null) {
            mapView.getLayerManager().getLayers().remove(marker);
        }

        if (locationGPS != null) {
            userLatitud = locationGPS.getLatitude();
            userLongitud = locationGPS.getLongitude();

            marker = new MyMarker(getApplicationContext(), new LatLong(userLatitud, userLongitud), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.drawable.img_ubicacion)), 0, 0);

            if (mapView != null) {
                mapView.getLayerManager().getLayers().add(marker);
            }
        }
    }

    /**
     * LocationListener -> Para obtener la ubicación.
     */
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            paintMyMarker(location);
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
    };

}