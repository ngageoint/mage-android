package mil.nga.giat.mage.observation;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageEditText;
import mil.nga.giat.mage.form.MageSpinner;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.common.State;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class ObservationEditActivity extends Activity {

	private static final String LOG_NAME = ObservationEditActivity.class.getName();
	
	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String LOCATION = "LOCATION";
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
    public static String INITIAL_ZOOM = "INITIAL_ZOOM";
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;
	private static final int LOCATION_EDIT_ACTIVITY_REQUEST_CODE = 600;
	
	private static final long NEW_OBSERVATION = -1L;

	private final GeometryFactory geometryFactory = new GeometryFactory();
	
	Date date;
	DecimalFormat latLngFormat = new DecimalFormat("###.#####");
	ArrayList<Attachment> attachments = new ArrayList<Attachment>();
	Location l;
	long observationId;
	Observation o;
	GoogleMap map;
	Marker observationMarker;
	Circle accuracyCircle;
	long locationElapsedTimeMs = 0;
	
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.getDefault());
    private DateFormat iso8601 = DateUtility.getISO8601();
	
	// View fields
	Spinner typeSpinner;
	Spinner levelSpinner;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.observation_editor);
		typeSpinner = (Spinner) findViewById(R.id.type_spinner);
		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onTypeOrLevelChanged("type", parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
		
		levelSpinner = (Spinner) findViewById(R.id.level_spinner);
		levelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onTypeOrLevelChanged("EVENTLEVEL", parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
		
		hideKeyboardOnClick(findViewById(R.id.observation_edit));
		
		Intent intent = getIntent();
		observationId = intent.getLongExtra(OBSERVATION_ID, NEW_OBSERVATION);
		
		if (observationId == NEW_OBSERVATION) {
			this.setTitle("Create New Observation");
			l = intent.getParcelableExtra(LOCATION);
			date = new Date();
			((TextView) findViewById(R.id.date)).setText(sdf.format(date));
			
	        // set default type and level values for map marker
			o = new Observation();
			o.getProperties().add(new ObservationProperty("type", typeSpinner.getSelectedItem().toString()));
	        o.getProperties().add(new ObservationProperty("EVENTLEVEL", levelSpinner.getSelectedItem().toString()));
		} else {
			this.setTitle("Edit Observation");
			// this is an edit of an existing observation
			try {
				o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
				attachments.addAll(o.getAttachments());
				for (Attachment a : attachments) {
					addAttachmentToGallery(a);
				}
			
				Map<String, ObservationProperty> propertiesMap = o.getPropertiesMap();
				String dateText = propertiesMap.get("timestamp").getValue();
                try {
                    date = iso8601.parse(dateText);
                    dateText = sdf.format(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }				
				
				((TextView) findViewById(R.id.date)).setText(dateText);
				Geometry geo = o.getObservationGeometry().getGeometry();
				if(geo instanceof Point) {
					Point point = (Point)geo;
					String provider = "manual";
					if(propertiesMap.get("LOCATION_PROVIDER") != null) {
						provider = propertiesMap.get("LOCATION_PROVIDER").getValue();
					}
					l = new Location(provider);
					if (propertiesMap.containsKey("LOCATION_ACCURACY")) {
						l.setAccuracy(Float.parseFloat(propertiesMap.get("LOCATION_ACCURACY").getValue()));
					}
					l.setLatitude(point.getY());
					l.setLongitude(point.getX());
				}
				populatePropertyFieldsFromMap((LinearLayout)findViewById(R.id.form), propertiesMap);
			} catch (ObservationException oe) {
				
			}
		}
		
		findViewById(R.id.date_edit).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ObservationEditActivity.this);
			    // Get the layout inflater
			    LayoutInflater inflater = getLayoutInflater();
			    View dialogView = inflater.inflate(R.layout.date_time_dialog, null);
			    final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
			    final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);
			    // Inflate and set the layout for the dialog
			    // Pass null as the parent view because its going in the dialog layout
			    builder.setView(dialogView)
			    // Add action buttons
			           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			               @Override
			               public void onClick(DialogInterface dialog, int id) {
			                   // set the date and time to what they chose
			            	   int day = datePicker.getDayOfMonth();
			            	   int month = datePicker.getMonth();
			            	   int year = datePicker.getYear();
			            	   int hour = timePicker.getCurrentHour();
			            	   int minute = timePicker.getCurrentMinute();
			            	   int second = 0;
			            	   
			            	   Calendar c = Calendar.getInstance();
			            	   c.set(year, month, day, hour, minute, second);
			            	   date = c.getTime();
			            	   ((TextView) findViewById(R.id.date)).setText(date.toString());
			               }
			           })
			           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			                   dialog.cancel();
			               }
			           });      
			    AlertDialog ad = builder.create();
			    ad.show();

			}
		});
		
		findViewById(R.id.location_edit).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ObservationEditActivity.this, LocationEditActivity.class);
                intent.putExtra(LocationEditActivity.LOCATION, l);
                intent.putExtra(LocationEditActivity.MARKER_BITMAP, ObservationBitmapFactory.bitmap(ObservationEditActivity.this, o));
                startActivityForResult(intent, LOCATION_EDIT_ACTIVITY_REQUEST_CODE);

//			    builder.setView(dialogView)
//			    // Add action buttons
//			           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//			               @Override
//			               public void onClick(DialogInterface dialog, int id) {
////			                   // set the date and time to what they chose
//			            	   LatLng center = dialogMap.getCameraPosition().target;
//			            	   l.setLatitude(center.latitude);
//			            	   l.setLongitude(center.longitude);
//			            	   l.setProvider("manual");
//			            	   l.setAccuracy(0.0f);
//			            	   l.setTime(System.currentTimeMillis());
//			            	   setupMap();
//
//			            	   com.google.android.gms.maps.MapFragment mapFragment = 
//			                           ((com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map));
//
//			                   if (mapFragment != null) {
//			                       FragmentManager manager = getFragmentManager();
//			                       FragmentTransaction t = manager.beginTransaction();
//			                       FragmentTransaction t2 = t.remove(mapFragment).detach(mapFragment);
//			                       t2.commitAllowingStateLoss();
//			                   }
//			               }
//			           })
//			           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//			               public void onClick(DialogInterface dialog, int id) {
//			            	   com.google.android.gms.maps.MapFragment mapFragment = 
//			                           ((com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map));
//
//			                   if (mapFragment != null) {
//			                       FragmentManager manager = getFragmentManager();
//			                       FragmentTransaction t = manager.beginTransaction();
//			                       FragmentTransaction t2 = t.remove(mapFragment).detach(mapFragment);
//			                       t2.commitAllowingStateLoss();
//			                   }
//			                   dialog.cancel();
//			               }
//			           });      
//			    AlertDialog ad = builder.create();
//			    ad.show();
			}
		});
		
		setupMap();
	}
	
	/**
	 * Hides keyboard when clicking elsewhere
	 * 
	 * @param view
	 */
	private void hideKeyboardOnClick(View view) {
		// Set up touch listener for non-text box views to hide keyboard.
		if (!(view instanceof EditText) && !(view instanceof Button)) {
			view.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (getCurrentFocus() != null) {
						inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
					}
					return false;
				}
			});
		}

		// If a layout container, iterate over children and seed recursion.
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				hideKeyboardOnClick(innerView);
			}
		}
	}
	
	private String elapsedTime(long ms) {
		String s = "";
		long sec = ms/1000;
		long min = sec/60;
		if (min == 0) {
			s = sec + ((sec == 1) ? " sec ago" : " secs ago");
		} else if (min < 60) {
			s = min + ((min == 1) ? " min ago" : " mins ago");
		} else {
			long hour = Math.round(Math.floor(min/60));
			s = hour + ((hour == 1) ? " hour ago" : " hours ago");
		}
		return s;
	}
	
	private long timeMs(long timeNanos) {
		return timeNanos/1000000;
	}
	
	
	@SuppressLint("NewApi")
	private long getElapsedTime() {
		if (Build.VERSION.SDK_INT >= 17) {
			if (l.getElapsedRealtimeNanos() == 0) {
				return 0;
			} else {
				return timeMs(SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos());
			}
		}
		return System.currentTimeMillis() - l.getTime();
	}
		
	private void setupMap() {
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.background_map)).getMap();

		LatLng location = new LatLng(l.getLatitude(), l.getLongitude());
		((TextView) findViewById(R.id.location)).setText(latLngFormat.format(l.getLatitude()) + ", " + latLngFormat.format(l.getLongitude()));
		if (l.getProvider() != null) {
			((TextView)findViewById(R.id.location_provider)).setText("("+l.getProvider()+")");
		} else {
			findViewById(R.id.location_provider).setVisibility(View.GONE);
		}
		if (l.getAccuracy() != 0) {
			((TextView)findViewById(R.id.location_accuracy)).setText("\u00B1" + l.getAccuracy() + "m");
		} else {
			findViewById(R.id.location_accuracy).setVisibility(View.GONE);
		}
		
		locationElapsedTimeMs = getElapsedTime();
		if (locationElapsedTimeMs != 0) {
			((TextView)findViewById(R.id.location_elapsed_time)).setText(elapsedTime(locationElapsedTimeMs));
		} else {
			findViewById(R.id.location_elapsed_time).setVisibility(View.GONE);
		}
		
		
        LatLng latLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
        if (latLng == null) {
            latLng = new LatLng(0,0);
        }
        
        float zoom = getIntent().getFloatExtra(INITIAL_ZOOM, 0);
        
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 18));
		
		if (accuracyCircle != null) {
			accuracyCircle.remove();
		}
		
		CircleOptions circleOptions = new CircleOptions()
		.fillColor(getResources().getColor(R.color.accuracy_circle_fill))
		.strokeColor(getResources().getColor(R.color.accuracy_circle_stroke))
		.strokeWidth(5)
	    .center(location)
	    .radius(l.getAccuracy());
		accuracyCircle = map.addCircle(circleOptions);

		
		if (observationMarker != null) {
			observationMarker.setPosition(location);
			observationMarker.setIcon(ObservationBitmapFactory.bitmapDescriptor(this, o));
		} else {
			observationMarker = map.addMarker(new MarkerOptions().position(location).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		l = savedInstanceState.getParcelable("location");
		attachments = savedInstanceState.getParcelableArrayList("attachments");

		for (Attachment a : attachments) {
			addAttachmentToGallery(a);
		}

		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		populatePropertyFieldsFromSaved(form, savedInstanceState);
		currentImageUri = savedInstanceState.getParcelable("currentImageUri");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("location", l);
		outState.putParcelableArrayList("attachments", new ArrayList<Attachment>(attachments));
		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		savePropertyFieldsToBundle(form, outState);
		outState.putParcelable("currentImageUri", currentImageUri);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.observation_edit_menu, menu);
		return true;
	}
	
	private void populatePropertyFieldsFromSaved(LinearLayout ll, Bundle savedInstanceState) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				String propertyKey = ((MageTextView)v).getPropertyKey();
				((MageTextView)v).setText(savedInstanceState.getString(propertyKey));
			} else if (v instanceof MageEditText) {
				String propertyKey = ((MageEditText)v).getPropertyKey();
				((MageEditText)v).setText(savedInstanceState.getString(propertyKey));
			} else if (v instanceof MageSpinner) {
				MageSpinner spinner = (MageSpinner)v;
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				String value = savedInstanceState.getString(propertyKey);
				int index = 0;
				for (index = 0; index < spinner.getAdapter().getCount(); index++)
			    {
			        if (spinner.getAdapter().getItem(index).equals(value))
			        {
			            spinner.setSelection(index);
			            break;
			        }
			    }
			} else if (v instanceof LinearLayout) {
				populatePropertyFieldsFromSaved((LinearLayout)v, savedInstanceState);
			}
		}
	}
	
	private void savePropertyFieldsToBundle(LinearLayout ll, Bundle outState) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				String propertyKey = ((MageTextView)v).getPropertyKey();
				outState.putString(propertyKey, ((MageTextView)v).getText().toString());
			} else if (v instanceof MageEditText) {
				String propertyKey = ((MageEditText)v).getPropertyKey();
				outState.putString(propertyKey, ((MageEditText)v).getText().toString());
			} else if (v instanceof MageSpinner) {
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				outState.putString(propertyKey, (String) ((MageSpinner)v).getSelectedItem());
			} else if (v instanceof LinearLayout) {
				savePropertyFieldsToBundle((LinearLayout)v, outState);
			}
		}
	}
	
	private Map<String, ObservationProperty> getPropertyFieldsAsMap(LinearLayout ll) {
		 Map<String, ObservationProperty> properties = new HashMap<String, ObservationProperty>();
		 return getPropertyFieldsAsMapRecurse(ll, properties);
	}
	
	private final Map<String, ObservationProperty> getPropertyFieldsAsMapRecurse(LinearLayout ll, Map<String, ObservationProperty> fields) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			
			if (v instanceof LinearLayout) {
				fields.putAll(getPropertyFieldsAsMapRecurse((LinearLayout)v, fields));
			} else {
				String key = null;
				String value = null;
				if (v instanceof MageTextView) {
					key = ((MageTextView)v).getPropertyKey();
					value = ((MageTextView)v).getText().toString();
				} else if (v instanceof MageEditText) {
					key = ((MageEditText)v).getPropertyKey();
					value = ((MageEditText)v).getText().toString();
				} else if (v instanceof MageSpinner) {
					key = ((MageSpinner)v).getPropertyKey();
					value = (String) ((MageSpinner)v).getSelectedItem();
				}
				if (key != null && value != null) {
					fields.put(key, new ObservationProperty(key, value));
				}
			}
		}
		return fields;
	}
	
	private void populatePropertyFieldsFromMap(LinearLayout ll, Map<String, ObservationProperty> propertiesMap) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				MageTextView m = (MageTextView)v;
				String propertyKey = m.getPropertyKey();
				String propertyValue = propertiesMap.get(propertyKey).getValue();
				if (propertyValue == null) continue;
				switch(m.getPropertyType()) {
				case STRING:
				case MULTILINE:
					m.setText(propertyValue);
					break;
				case USER:
					
					break;
				case DATE:
                    String dateText = propertyValue;
                    try {
                        Date date = iso8601.parse(propertyValue);
                        dateText = sdf.format(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    m.setText(dateText);
					break;
				case LOCATION:
					
					break;
				case MULTICHOICE:
					
					break;
				}
			} else if (v instanceof MageEditText) {
				MageEditText m = (MageEditText)v;
				String propertyKey = m.getPropertyKey();
				String propertyValue = propertiesMap.get(propertyKey).getValue();
				m.setText(propertyValue);
			} else if (v instanceof MageSpinner) {
				MageSpinner spinner = (MageSpinner)v;
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				ObservationProperty property = propertiesMap.get(propertyKey);
				if(property != null) {
					int index = 0;
					for (index = 0; index < spinner.getAdapter().getCount(); index++) {
						if (spinner.getAdapter().getItem(index).equals(property.getValue())) {
							spinner.setSelection(index);
							break;
						}
					}
				}
			} else if (v instanceof LinearLayout) {
				populatePropertyFieldsFromMap((LinearLayout)v, propertiesMap);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.observation_save:
			o.setState(State.ACTIVE);
			o.setObservationGeometry(new ObservationGeometry(geometryFactory.createPoint(new Coordinate(l.getLongitude(), l.getLatitude()))));
			
			LinearLayout form = (LinearLayout) findViewById(R.id.form);
			
			Map<String, ObservationProperty> propertyMap = getPropertyFieldsAsMap(form);
			propertyMap.put("type", new ObservationProperty("type", typeSpinner.getSelectedItem().toString()));
			propertyMap.put("EVENTLEVEL", new ObservationProperty("EVENTLEVEL", levelSpinner.getSelectedItem().toString()));
			propertyMap.put("timestamp", new ObservationProperty("timestamp", iso8601.format(date)));
			propertyMap.put("accuracy", new ObservationProperty("accuracy", Float.toString(l.getAccuracy())));
			propertyMap.put("provider", new ObservationProperty("provider", "manual"));
			propertyMap.put("delta", new ObservationProperty("delta", Long.toString(timeMs(locationElapsedTimeMs))));
			
			o.addProperties(propertyMap.values());
			
			o.setAttachments(attachments);

			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				if (o.getId() == null) {
					Observation newObs = oh.create(o);
					Log.i(LOG_NAME, "Created new observation with id: " + newObs.getId());
				} else {
					o.setDirty(true);
					oh.update(o);
					Log.i(LOG_NAME, "Updated observation with remote id: " + o.getRemoteId());
				}
				((MAGE)getApplication()).scheduleObservationAlarm();
				finish();
			} catch (Exception e) {
				Log.e(LOG_NAME, e.getMessage(), e);
			}

			break;
		case R.id.observation_cancel:
			new AlertDialog.Builder(this).setTitle("Discard Changes").setMessage(R.string.cancel_edit).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			}).show();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void cameraButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File f = null;
        try {
        	f = MediaUtility.createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
        	ex.printStackTrace();
        }
        // Continue only if the File was successfully created
        if (f != null) {
        	currentImageUri = Uri.fromFile(f);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
	}

	public void videoButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	public void voiceButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		startActivityForResult(intent, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE);
	}

	public void fromGalleryButtonPressed(View v) {
		Intent intent = new Intent();
		intent.setType("image/*, video/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		Log.i(LOG_NAME, "build version sdk int: " + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT >= 18) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	private void addAttachmentToGallery(final Attachment a) {
		LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
		
		final String absPath = a.getLocalPath();
		final String remoteId = a.getRemoteId();
		ImageView iv = new ImageView(getApplicationContext());
		LayoutParams lp = new LayoutParams(100, 100);
		iv.setLayoutParams(lp);
		iv.setPadding(0, 0, 10, 0);
		iv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
				intent.putExtra("attachment", a);
				intent.putExtra(AttachmentViewerActivity.EDITABLE, true);
				startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
			}
		});
		l.addView(iv);
		
		// get content type from everywhere I can think of
		String contentType = a.getContentType();
		if (contentType == null || "".equalsIgnoreCase(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
			String name = a.getName();
			if (name == null) {
				name = a.getLocalPath();
				if (name == null) {
					name = a.getRemotePath();
				}
			}
			contentType = MediaUtility.getMimeType(name);
		}
		
		if (absPath != null) {
			if (contentType.startsWith("image")) {
				Glide.load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.load(R.drawable.ic_microphone).into(iv);
			}
		} else if (remoteId != null) {
			String url = a.getUrl();
			Log.i("test", "url to load is: " + url);
			Log.i("test", "content type is: " + contentType + " name is: " + a.getName());
			if (contentType.startsWith("image")) {
				Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.load(R.drawable.ic_microphone).into(iv);
			}
		}
		
		
		
//		try {
//			
//			if (absPath.endsWith(".mp4")) {
//				Drawable[] layers = new Drawable[2];
//				Resources r = getResources();
//				layers[0] = new BitmapDrawable(r, ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.MICRO_KIND));
//				layers[1] = r.getDrawable(R.drawable.ic_video_white_2x);
//				LayerDrawable ld = new LayerDrawable(layers);
//				iv.setImageDrawable(ld);
//			} else if (absPath.endsWith(".mp3") || absPath.endsWith("m4a")) {
//				iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone));
//			} else {
//				iv.setImageBitmap(MediaUtility.getThumbnail(new File(absPath), 100));
//			}
//			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
//			iv.setLayoutParams(lp);
//			iv.setPadding(0, 0, 10, 0);
//			iv.setOnClickListener(new View.OnClickListener() {
//
//				@Override
//				public void onClick(View v) {
//					Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
//					intent.setData(Uri.fromFile(new File(absPath)));
//					intent.putExtra(AttachmentViewerActivity.EDITABLE, true);
//					startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
//				}
//			});
//			l.addView(iv);
//			Log.d("image", "Set the image gallery to have an image with absolute path " + absPath);
//		} catch (Exception e) {
//			Log.e("exception", "Error making image", e);
//		}
	}
	
	Uri currentImageUri;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;
		switch (requestCode) {
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			MediaUtility.addImageToGallery(getApplicationContext(), currentImageUri);
			Attachment capture = new Attachment();
			capture.setLocalPath(MediaUtility.getFileAbsolutePath(currentImageUri, this));
			attachments.add(capture);
			addAttachmentToGallery(capture);
			break;
		case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
		case GALLERY_ACTIVITY_REQUEST_CODE:
		case CAPTURE_VOICE_ACTIVITY_REQUEST_CODE:
			ArrayList<Uri> uris = getUris(data);
			Log.i("test", "found " + uris.size() + " uris");
			for (Uri u : uris) {
				Log.i("test", "adding uri: " + u);
				String path = MediaUtility.getPath(getApplicationContext(), u);
				Attachment a = new Attachment();
				a.setLocalPath(path);
				attachments.add(a);
				addAttachmentToGallery(a);
			}
			break;
		case ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE:
			Attachment remove = data.getParcelableExtra("attachment");
			if (remove != null && data.getBooleanExtra("REMOVE", false)) {
				int idx = attachments.indexOf(remove);
				attachments.remove(remove);
				LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
				l.removeViewAt(idx);
			}
			break;
		case LOCATION_EDIT_ACTIVITY_REQUEST_CODE:
			l = data.getParcelableExtra(LocationEditActivity.LOCATION);
			setupMap();
			break;
		}
	}

	private ArrayList<Uri> getUris(Intent intent) {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		addClipDataUris(intent, uris);
		if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		return uris;
	}
	
	@TargetApi(16)
	private void addClipDataUris(Intent intent, ArrayList<Uri> uris) {
		if (Build.VERSION.SDK_INT >= 16) {
			ClipData cd = intent.getClipData();
			if (cd == null) return;
			for (int i = 0; i < cd.getItemCount(); i++) {
				uris.add(cd.getItemAt(i).getUri());
			}
		}
	}

	public void onTypeOrLevelChanged(String field, String value) {
		o.addProperties(Collections.singleton(new ObservationProperty(field, value)));
		if (observationMarker != null) {
			observationMarker.remove();
		}
		observationMarker = map.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(), l.getLongitude())).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
	}
}
