// Created by cristigav on 00:23:14 - 03.10.2010

package org.gc.gts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;

import ro.sysopconsulting.util.Const;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

/**
 * Default map view activity.
 * 
 * @author Cristian Gavrila
 * 
 * @author SysOP Consulting SRL
 * 
 */
public class GtsActivity extends Activity {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final int MENU_MY_LOCATION = Menu.FIRST;
    private static final int MENU_LATITUDE = MENU_MY_LOCATION + 1;
    private static final int MENU_ROUTE = MENU_LATITUDE + 1;
    private static final int MENU_ABOUT = MENU_ROUTE + 1;
    private static final int MENU_INFO = MENU_ABOUT + 1;
    private static final int MENU_SETTINGS = MENU_INFO + 1;

    private static final int DIALOG_ABOUT_ID = 1;
    private static final int DIALOG_INFO_ID = 2;
    private static final int DIALOG_LATITUDE_ID = 3;
    private static final int DIALOG_ROUTE_FROM_ID = 4;
    private static final int DIALOG_ROUTE_TO_ID = 5;

    // ===========================================================
    // Fields
    // ===========================================================

    private SharedPreferences mPrefs;
    private OpenStreetMapView mOsmv;
    private MyLocationOverlay mLocationOverlay;
    private DevicesTraceOverlay mDevicesOverlay;
    private LatitudeOverlay mLatitudeOverlay;
    private RouteOverlay mRouteOverlay;

    private Bundle bundle;

    private GeoPoint from, to;

    String TAG = "aGTS";

    Context ctx;

    // ===========================================================
    // Constructors
    // ===========================================================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	ctx = this;

	// mPrefs = getSharedPreferences(TAG, MODE_PRIVATE);
	mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

	this.startService(new Intent(Const.SERVICENAME));
	Log.w(TAG, "Service Started");

	try {
	    ActivityInfo ai = getPackageManager().getActivityInfo(
		    this.getComponentName(), PackageManager.GET_META_DATA);
	    bundle = ai.metaData;

	    // Log.i("Bundle:account", bundle.getString("account"));
	    // Log.i("Bundle:user", bundle.getString("user"));
	    // Log.i("Bundle:password", bundle.getString("password"));
	    this.mDevicesOverlay = DevicesTraceOverlay.getInstance(ctx, mOsmv,
		    bundle.getString("server"), bundle.getString("account"),
		    bundle.getString("user"), bundle.getString("password"));
	} catch (NameNotFoundException e) {
	    e.printStackTrace();
	}

	final RelativeLayout rl = new RelativeLayout(this);

	this.mOsmv = new OpenStreetMapView(this);

	this.mLocationOverlay = new MyLocationOverlay(this.getBaseContext(),
		this.mOsmv);
	this.mOsmv.setBuiltInZoomControls(true);
	this.mOsmv.getOverlays().add(this.mLocationOverlay);

	this.mOsmv.getOverlays().add(this.mDevicesOverlay);

	if (PreferenceManager.getDefaultSharedPreferences(ctx)
		.getString("latitude", "").length() > 1) {
	    mLatitudeOverlay = LatitudeOverlay.getInstance(ctx, mOsmv,
		    PreferenceManager.getDefaultSharedPreferences(ctx)
			    .getString("latitude", ""));

	    mOsmv.getOverlays().add(mLatitudeOverlay);
	}

	rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(
		LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

	this.setContentView(rl);

	ArrayList<GeoPoint> sir = DevicesTraceOverlay.getPath();

	GeoPoint center = getCenter(sir);

	int zoom = Integer.parseInt(mPrefs.getString("ZOOM_LEVEL", "0"));
	if (zoom == 0) {
	    zoom = getZoom(sir, center);
	}

	Log.w(TAG, "Zoom Level: " + zoom);

	mOsmv.getController().setZoom(zoom);
	mOsmv.getController().setCenter(center);
	// mOsmv.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(
	// PREFS_SCROLL_Y, 0));

    }

    private int getZoom(ArrayList<GeoPoint> sir, GeoPoint center) {
	// TODO Auto-generated method stub
	// creaza un sir cu distantele pina la toate masinile
	// cauta nivelul de zoom optim, functie de dimensiunea ecranului
	Display display = getWindowManager().getDefaultDisplay();
	int width = display.getWidth();
	int height = display.getHeight();
	int mix = Math.min(width, height);

	ArrayList<Integer> distances = new ArrayList<Integer>();
	Iterator<GeoPoint> it = sir.iterator();
	while (it.hasNext()) {
	    distances.add(center.distanceTo(it.next()));
	}
	int max_distance = Collections.max(distances);
	int dpixel = max_distance / mix;
	Log.w(TAG, "Distance per pixel: " + dpixel);

	if (dpixel < 2)
	    return 15;
	else if (dpixel < 5)
	    return 14;
	else if (dpixel < 15)
	    return 13;
	else if (dpixel < 25)
	    return 12;
	else if (dpixel < 45)
	    return 11;
	else if (dpixel < 95)
	    return 10;
	else if (dpixel < 190)
	    return 9;
	else

	    return 4;
    }

    private GeoPoint getCenter(ArrayList<GeoPoint> sir) {
	// method to return the centre of locations
	int min_latitude, max_latitude, min_longitude, max_longitude;

	ArrayList<Integer> latitude = new ArrayList<Integer>();
	ArrayList<Integer> longitude = new ArrayList<Integer>();
	Iterator<GeoPoint> it = sir.iterator();
	while (it.hasNext()) {
	    GeoPoint gp = it.next();
	    latitude.add(gp.getLatitudeE6());
	    longitude.add(gp.getLongitudeE6());
	}
	max_latitude = Collections.max(latitude);
	min_latitude = Collections.min(latitude);

	max_longitude = Collections.max(longitude);
	min_longitude = Collections.min(longitude);

	GeoPoint mean = new GeoPoint((max_latitude + min_latitude) / 2,
		(max_longitude + min_longitude) / 2);
	Log.w(TAG, "Center =  " + mean.toString());
	return mean;
    }

    @Override
    protected void onPause() {
	/*
	 * SharedPreferences.Editor edit = mPrefs.edit();
	 * edit.putInt(PREFS_SCROLL_X, mOsmv.getScrollX());
	 * edit.putInt(PREFS_SCROLL_Y, mOsmv.getScrollY());
	 * edit.putInt(PREFS_ZOOM_LEVEL, mOsmv.getZoomLevel());
	 * edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay
	 * .isMyLocationEnabled()); edit.putBoolean(PREFS_FOLLOW_LOCATION,
	 * mLocationOverlay .isLocationFollowEnabled()); edit.commit();
	 * 
	 * this.mLocationOverlay.disableMyLocation();
	 */

	super.onPause();
    }

    @Override
    protected void onResume() {
	super.onResume();
	// mOsmv.setRenderer();
	/*
	 * if (mPrefs.getBoolean(PREFS_SHOW_LOCATION, false))
	 * this.mLocationOverlay.enableMyLocation();
	 * this.mLocationOverlay.followLocation(mPrefs.getBoolean(
	 * PREFS_FOLLOW_LOCATION, true));
	 */
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu pMenu) {
	pMenu.add(0, MENU_MY_LOCATION, Menu.NONE, R.string.my_location)
		.setIcon(android.R.drawable.ic_menu_mylocation);

	pMenu.add(0, MENU_LATITUDE, Menu.NONE, R.string.latitude).setIcon(
		android.R.drawable.ic_menu_compass);

	pMenu.add(0, MENU_ROUTE, Menu.NONE, R.string.route).setIcon(
		android.R.drawable.ic_menu_directions);

	pMenu.add(0, MENU_INFO, Menu.NONE, R.string.info).setIcon(
		android.R.drawable.ic_menu_info_details);

	pMenu.add(ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE,
		R.string.menu_settings).setIcon(R.drawable.preferences)
		.setAlphabeticShortcut('S');

	return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
	switch (item.getItemId()) {
	case MENU_MY_LOCATION:
	    this.mLocationOverlay.followLocation(true);
	    this.mLocationOverlay.enableMyLocation();
	    /*
	     * Location lastFix = this.mLocationOverlay.getLastFix(); if
	     * (lastFix != null) this.mOsmv.setMapCenter(new GeoPoint(lastFix));
	     */
	    return true;

	case MENU_LATITUDE:
	    showDialog(DIALOG_LATITUDE_ID);
	    return true;

	case MENU_ROUTE:
	    showDialog(DIALOG_ROUTE_FROM_ID);
	    return true;

	case MENU_INFO:
	    showDialog(DIALOG_INFO_ID);
	    return true;
	case MENU_SETTINGS:
	    Intent i = new Intent(this, ro.sysopconsulting.SettingsDialog.class);

	    startActivity(i);
	    return true;

	default: // Map mode submenu items

	}
	return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	Dialog dialog;

	switch (id) {
	case DIALOG_ABOUT_ID:
	    return new AlertDialog.Builder(GtsActivity.this)
		    .setIcon(R.drawable.icon)
		    .setTitle(R.string.app_name)
		    .setMessage(R.string.about_message)
		    .setPositiveButton("OK",
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				}
			    }).create();

	case DIALOG_INFO_ID:
	    return new AlertDialog.Builder(GtsActivity.this)
		    .setIcon(R.drawable.icon)
		    .setTitle("Account info")
		    .setMessage(
			    "Account: " + bundle.getString("account")
				    + "\nUser: " + bundle.getString("user"))
		    .setPositiveButton("OK",
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				}
			    }).create();

	case DIALOG_LATITUDE_ID:
	    AlertDialog.Builder alert = new AlertDialog.Builder(this);

	    alert.setTitle("Set latitude account");
	    alert.setMessage("ID");

	    // Set an EditText view to get user input
	    final EditText input = new EditText(this);
	    input.setText(mPrefs.getString("LATITUDE_ID", ""));
	    alert.setView(input);

	    alert.setPositiveButton("Ok",
		    new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
			    String value = input.getText().toString();
			    Log.i("Google latitude", value);

			    if (null != mLatitudeOverlay)
				mOsmv.getOverlays().remove(mLatitudeOverlay);

			    mLatitudeOverlay = LatitudeOverlay.getInstance(ctx,
				    mOsmv, value);
			    // "-6518729008335465161");

			    mOsmv.getOverlays().add(mLatitudeOverlay);

			    mOsmv.invalidate();

			    SharedPreferences.Editor edit = mPrefs.edit();
			    edit.putString("LATITUDE_ID", value);
			    edit.commit();
			}
		    });

	    alert.setNegativeButton("Cancel",
		    new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
			    // Canceled.
			}
		    });

	    return alert.create();

	case DIALOG_ROUTE_FROM_ID:
	    AlertDialog.Builder routeDialog = new AlertDialog.Builder(this);

	    routeDialog.setTitle("Set route from");

	    CharSequence[] items = { "My location", "Google latitude" };

	    routeDialog.setItems(items, new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {
		    from = null;

		    if (which == 0)
			from = mLocationOverlay.getMyLocation();
		    if (which == 1)
			from = mLatitudeOverlay.getLocation();

		    dialog.dismiss();

		    showDialog(DIALOG_ROUTE_TO_ID);
		}
	    });

	    return routeDialog.create();

	case DIALOG_ROUTE_TO_ID:
	    AlertDialog.Builder routeToDialog = new AlertDialog.Builder(this);

	    routeToDialog.setTitle("Set route to");

	    routeToDialog.setItems(mDevicesOverlay.getItems(),
		    new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
			    to = mDevicesOverlay.getLocation(which);

			    dialog.dismiss();

			    Log.i("Route", "from " + from + " to " + to);

			    // add route demo overlay
			    if (null != mRouteOverlay)
				mOsmv.getOverlays().remove(mRouteOverlay);

			    mRouteOverlay = RouteOverlay.getInstance(ctx,
				    mOsmv, from, to);
			    mOsmv.getOverlays().add(mRouteOverlay);
			    mOsmv.invalidate();
			}
		    });

	    return routeToDialog.create();

	default:
	    dialog = null;
	    break;
	}
	return dialog;
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
	return this.mOsmv.onTrackballEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	if (event.getAction() == MotionEvent.ACTION_MOVE)
	    this.mLocationOverlay.followLocation(false);

	/*
	 * if (event.getAction() == MotionEvent.ACTION_DOWN) {
	 * mDevicesOverlay.onSingleTapUp(event, mOsmv);
	 * mPoiOverlay.onSingleTapUp(event, mOsmv); }
	 */

	return super.onTouchEvent(event);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
