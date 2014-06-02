package mil.nga.giat.mage.form;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Use this class to build and populate the views concerned with form like information.
 * 
 * @author wiedemanns
 * 
 */
public class LayoutBaker {

	public static List<View> createControlsFromJson(Context context) {
		// add the theme to the context
		context = new ContextThemeWrapper(context, R.style.AppTheme);
		
		List<View> views = new ArrayList<View>();

		String dynamicFormString = PreferenceHelper.getInstance(context).getValue(R.string.dynamicFormKey);
		JsonObject dynamicFormJson = new JsonParser().parse(dynamicFormString).getAsJsonObject();

		JsonArray dynamicFormFields = dynamicFormJson.get("fields").getAsJsonArray();

		for (int i = 0; i < dynamicFormFields.size(); i++) {
			JsonObject field = dynamicFormFields.get(i).getAsJsonObject();

			// get members
			Integer id = field.get("id").getAsInt();
			String title = field.get("title").getAsString();
			DynamicFormType type = DynamicFormType.TEXTAREA;
			String typeString = field.get("type").getAsString();
			if (typeString != null) {
				try {
					type = DynamicFormType.valueOf(typeString.toUpperCase());
				} catch (IllegalArgumentException iae) {
					type = DynamicFormType.TEXTAREA;
				}
			}

			Boolean required = field.get("required").getAsBoolean();
			String name = field.get("name").getAsString();
			JsonArray choicesJson = field.get("choices").getAsJsonArray();
			Collection<String> choices = new ArrayList<String>();
			if (choicesJson != null && !choicesJson.isJsonNull()) {
				for (int j = 0; j < choicesJson.size(); j++) {
					JsonObject choiceJson = choicesJson.get(j).getAsJsonObject();
					String choiceTitle = choiceJson.get("title").getAsString();
					if (choiceTitle != null && !choiceTitle.trim().isEmpty()) {
						choices.add(choiceTitle);
					}
				}
			}

			float density = context.getResources().getDisplayMetrics().density;
			int marginTop = (int) (5 * density);
			int marginBottom = (int) (20 * density);
			if (i == dynamicFormFields.size() - 1) {
				marginBottom = (int) (0 * density);
			}

			TextView textView = new TextView(context);
			textView.setText(title);
			textView.setLayoutParams(new RelativeLayout.LayoutParams((int) LayoutParams.FILL_PARENT, (int) LayoutParams.WRAP_CONTENT));
			textView.setTextAppearance(context, mil.nga.giat.mage.R.style.EditTextView);

			MageControl mageControl = null;
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) LayoutParams.FILL_PARENT, (int) LayoutParams.WRAP_CONTENT);
			params.setMargins(0, marginTop, 0, marginBottom);

			// TODO: set required
			switch (type) {
			case TEXTAREA:
				MageEditText mageEditText = new MageEditText(context, null);
				mageEditText.setId(id);
				mageEditText.setLayoutParams(params);
				mageEditText.setHint(title);
				mageEditText.setMinLines(4);
				mageEditText.setPropertyKey(name);
				mageEditText.setPropertyType(MagePropertyType.MULTILINE);
				mageControl = mageEditText;
				break;
			case DROPDOWN:
				MageSpinner mageSpinner = new MageSpinner(context, null);
				mageSpinner.setId(id);
				mageSpinner.setLayoutParams(params);
				mageSpinner.setPropertyKey(name);
				mageSpinner.setPropertyType(MagePropertyType.MULTICHOICE);

				ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, choices.toArray(new String[choices.size()]));
				spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mageSpinner.setAdapter(spinnerArrayAdapter);
				mageControl = mageSpinner;
				break;
			default:
				mageControl = null;
				break;
			}
			if (mageControl != null) {
				// add both the text and control here!
				views.add(textView);
				views.add((View) mageControl);
			}
		}

		return views;
	}

	public static void populateLayoutWithControls(final LinearLayout linearLayout, List<View> controls) {
		for (View control : controls) {
			linearLayout.addView(control);
		}
	}

	/**
	 * Populates the linearLayout from the key, value pairs in the propertiesMap
	 * 
	 * @param linearLayout
	 * @param propertiesMap
	 */
	public static void populateLayoutFromMap(final LinearLayout linearLayout, final Map<String, ObservationProperty> propertiesMap) {
		for (int i = 0; i < linearLayout.getChildCount(); i++) {
			View v = linearLayout.getChildAt(i);
			if (v instanceof MageControl) {
				MageControl mageControl = (MageControl) v;
				String propertyKey = mageControl.getPropertyKey();
				ObservationProperty property = propertiesMap.get(propertyKey);
				if (property != null) {
					String propertyValue = property.getValue();
					if (propertyValue != null) {
						if (v instanceof MageTextView) {
							MageTextView m = (MageTextView) v;
							switch (m.getPropertyType()) {
							case STRING:
							case MULTILINE:
								m.setText(propertyValue);
								break;
							case USER:

								break;
							case DATE:
								String dateText = propertyValue;
								try {
									Date date = DateUtility.getISO8601().parse(propertyValue);
									dateText = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.getDefault()).format(date);
								} catch (ParseException e) {
									e.printStackTrace();
								}
								m.setText(dateText);
								break;
							case LOCATION:
								// location is not a property, it lives in the parent
								break;
							case MULTICHOICE:

								break;
							}
						} else if (v instanceof MageEditText) {
							MageEditText m = (MageEditText) v;
							m.setText(propertyValue);
						} else if (v instanceof MageSpinner) {
							MageSpinner spinner = (MageSpinner) v;
							for (int index = 0; index < spinner.getAdapter().getCount(); index++) {
								if (spinner.getAdapter().getItem(index).equals(propertyValue)) {
									spinner.setSelection(index);
									break;
								}
							}
						}
					}
				}
			} else if (v instanceof LinearLayout) {
				populateLayoutFromMap((LinearLayout) v, propertiesMap);
			}
		}
	}

	public static void populateLayoutFromBundle(final LinearLayout linearLayout, Bundle savedInstanceState) {
		Map<String, ObservationProperty> propertiesMap = new HashMap<String, ObservationProperty>();
		for (String key : savedInstanceState.keySet()) {
			propertiesMap.put(key, new ObservationProperty(key, savedInstanceState.getString(key)));
		}
		populateLayoutFromMap(linearLayout, propertiesMap);
	}

	/**
	 * Returns a map of key value pairs form the layout
	 * 
	 * @param linearLayout
	 * @return
	 */
	public static Map<String, ObservationProperty> populateMapFromLayout(LinearLayout linearLayout) {
		Map<String, ObservationProperty> properties = new HashMap<String, ObservationProperty>();
		return populateMapFromLayout(linearLayout, properties);
	}

	private static final Map<String, ObservationProperty> populateMapFromLayout(LinearLayout linearLayout, Map<String, ObservationProperty> fields) {
		for (int i = 0; i < linearLayout.getChildCount(); i++) {
			View v = linearLayout.getChildAt(i);

			if (v instanceof LinearLayout) {
				fields.putAll(populateMapFromLayout((LinearLayout) v, fields));
			} else if (v instanceof MageControl) {
				MageControl mageControl = (MageControl) v;
				String key = mageControl.getPropertyKey();
				String value = mageControl.getPropertyValue();
				if (key != null && value != null) {
					fields.put(key, new ObservationProperty(key, value));
				}
			}
		}
		return fields;
	}

	public static void populateBundleFromLayout(LinearLayout linearLayout, Bundle outState) {
		Map<String, ObservationProperty> properties = populateMapFromLayout(linearLayout);
		for (String key : properties.keySet()) {
			outState.putString(key, properties.get(key).getValue());
		}
	}
}
