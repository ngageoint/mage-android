package mil.nga.giat.mage.form;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Use this class to build and populate the views concerned with form like information.
 * 
 * @author wiedemanns
 * 
 */
public class LayoutBaker {

	private static final String LOG_NAME = LayoutBaker.class.getName();

	public enum ControlGenerationType {
		VIEW, EDIT;
	}

	public static List<View> createControlsFromJson(Context pContext, ControlGenerationType controlGenerationType) {
		// add the theme to the context
		final Context context = new ContextThemeWrapper(pContext, R.style.AppTheme);

		List<View> views = new ArrayList<View>();

		String dynamicFormString = PreferenceHelper.getInstance(context).getValue(R.string.dynamicFormKey);
		JsonObject dynamicFormJson = new JsonParser().parse(dynamicFormString).getAsJsonObject();

		JsonArray dynamicFormFields = dynamicFormJson.get("fields").getAsJsonArray();

		int uniqueChildIdIndex = 10000;

		for (int i = 0; i < dynamicFormFields.size(); i++) {
			JsonObject field = dynamicFormFields.get(i).getAsJsonObject();

			// get members
			Integer id = field.get("id").getAsInt();
			String title = field.get("title").getAsString();
			DynamicFormType type = DynamicFormType.TEXTFIELD;
			String typeString = field.get("type").getAsString();
			if (typeString != null) {
				try {
					type = DynamicFormType.valueOf(typeString.toUpperCase());
				} catch (IllegalArgumentException iae) {
					Log.e(LOG_NAME, "Unknown type: " + typeString, iae);
					type = DynamicFormType.TEXTFIELD;
				}
			}

			Boolean required = field.get("required").getAsBoolean();
			String value = null;
			JsonElement jsonValue = field.get("value");
			if (jsonValue != null && !jsonValue.isJsonNull() && jsonValue.isJsonPrimitive()) {
				value = jsonValue.getAsString();
			}
			

			Boolean archived = false;
			JsonElement jsonArchived = field.get("archived");
			if (jsonArchived != null && !jsonArchived.isJsonNull() && jsonArchived.isJsonPrimitive()) {
				archived = jsonArchived.getAsBoolean();
			}
			if(archived) {
				continue;
			}
			
			String name = field.get("name").getAsString();
			JsonArray choicesJson = field.get("choices").getAsJsonArray();
			Collection<String> choices = new HashSet<String>();
			if (choicesJson != null && !choicesJson.isJsonNull()) {
				for (int j = 0; j < choicesJson.size(); j++) {
					JsonObject choiceJson = choicesJson.get(j).getAsJsonObject();
					JsonElement choiceElement = choiceJson.get("title");
					if (choiceElement != null) {
	                   String choiceTitle = choiceElement.getAsString();
	                    if (choiceTitle != null && !choiceTitle.trim().isEmpty()) {
	                        choices.add(choiceTitle);
	                    }
					} else {
					    choices.add("");
					}

				}
			}

			final float density = context.getResources().getDisplayMetrics().density;

			LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams((int) LayoutParams.MATCH_PARENT, (int) LayoutParams.WRAP_CONTENT);
			LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams((int) LayoutParams.MATCH_PARENT, (int) LayoutParams.WRAP_CONTENT);

			TextView textView = new TextView(context);
			textView.setText(title);
			textView.setFocusable(false);
			textView.setTextIsSelectable(false);
			textView.setClickable(false);
			switch (controlGenerationType) {
			case EDIT:
				int controlMarginTop = (int) (5 * density);
				int controlMarginBottom = (i == dynamicFormFields.size() - 1) ? (int) (0 * density) : (int) (15 * density);
				int controlMarginLeft = (int) (0 * density);
				int controlMarginRight = (int) (0 * density);
				controlParams.setMargins(controlMarginLeft, controlMarginTop, controlMarginRight, controlMarginBottom);

				int textMarginTop = (int) (5 * density);
				int textMarginBottom = (int) (0 * density);
				int textMarginLeft = (int) (0 * density);
				int textMarginRight = (int) (0 * density);
				textParams.setMargins(textMarginLeft, textMarginTop, textMarginRight, textMarginBottom);
				textView.setTextAppearance(context, mil.nga.giat.mage.R.style.EditTextView);
				break;
			default:
			case VIEW:
				controlMarginTop = (int) (5 * density);
				controlMarginBottom = (i == dynamicFormFields.size() - 1) ? (int) (0 * density) : (int) (10 * density);
				controlMarginLeft = (int) (0 * density);
				controlMarginRight = (int) (0 * density);
				controlParams.setMargins(controlMarginLeft, controlMarginTop, controlMarginRight, controlMarginBottom);

				textMarginTop = (int) (5 * density);
				textMarginBottom = (int) (0 * density);
				textMarginLeft = (int) (0 * density);
				textMarginRight = (int) (5 * density);
				textParams.setMargins(textMarginLeft, textMarginTop, textMarginRight, textMarginBottom);
				textView.setTextAppearance(context, mil.nga.giat.mage.R.style.ViewTextView);
				break;
			}
			textView.setLayoutParams(textParams);

			// FIXME: set required, add remaining controls
			switch (controlGenerationType) {
			case EDIT:
				MageEditText mageEditText = new MageEditText(context, null);
				mageEditText.setId(id);
				mageEditText.setLayoutParams(controlParams);
				mageEditText.setHint(title);
				mageEditText.setRequired(required);
				mageEditText.setPropertyKey(name);
				mageEditText.setPropertyValue(value);
				switch (type) {
				case TEXTFIELD:
				case EMAIL:
					mageEditText.setPropertyType(MagePropertyType.STRING);
					views.add(textView);
					views.add((View) mageEditText);
					break;
				case PASSWORD:
					mageEditText.setPropertyType(MagePropertyType.STRING);
					views.add(textView);
					mageEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					views.add((View) mageEditText);
					break;
				case TEXTAREA:
					mageEditText.setMinLines(2);
					mageEditText.setPropertyType(MagePropertyType.MULTILINE);
					views.add(textView);
					views.add((View) mageEditText);
					break;
				case RADIO:
					MageRadioGroup mageRadioGroup = new MageRadioGroup(context, null);
					mageRadioGroup.setId(id);
					mageRadioGroup.setLayoutParams(controlParams);
					mageRadioGroup.setRequired(required);
					mageRadioGroup.setPropertyKey(name);
					mageRadioGroup.setPropertyType(MagePropertyType.MULTICHOICE);

					for (String choice : choices) {
						RadioButton radioButton = new RadioButton(context);
						radioButton.setId(uniqueChildIdIndex++);
						radioButton.setText(choice);
						mageRadioGroup.addView(radioButton);
					}
					mageRadioGroup.setPropertyValue(value);

					views.add(textView);
					views.add((View) mageRadioGroup);
					break;
				case CHECKBOX:
					MageCheckBox mageCheckBox = new MageCheckBox(context, null);
					mageCheckBox.setId(id);
					mageCheckBox.setLayoutParams(controlParams);
					mageCheckBox.setRequired(required);
					mageCheckBox.setPropertyKey(name);
					mageCheckBox.setPropertyType(MagePropertyType.STRING);
					if(value != null && !value.trim().isEmpty()) {
						mageCheckBox.setPropertyValue(Boolean.valueOf(value));
					}

					views.add(textView);
					views.add((View) mageCheckBox);
					break;
				case DROPDOWN:
					MageSpinner mageSpinner = new MageSpinner(context, null);
					mageSpinner.setId(id);
					mageSpinner.setLayoutParams(controlParams);
					mageSpinner.setRequired(required);
					mageSpinner.setPropertyKey(name);
					mageSpinner.setPropertyType(MagePropertyType.MULTICHOICE);

					ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, choices.toArray(new String[choices.size()]));
					spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					mageSpinner.setAdapter(spinnerArrayAdapter);
					mageSpinner.setPropertyValue(value);

					views.add(textView);
					views.add((View) mageSpinner);
					break;
				case DATE:
					// don't create the timestamp control on the edit page
					if (name.equals("timestamp")) {
						break;
					}

					ImageView imageView = new ImageView(context);
					// dip?
					LinearLayout.LayoutParams imageViewLayoutParams = new LinearLayout.LayoutParams((int) (18 * density), (int) (18 * density));
					imageViewLayoutParams.gravity = Gravity.LEFT;
					imageView.setLayoutParams(imageViewLayoutParams);
					imageView.setFocusable(false);
					imageView.setImageResource(mil.nga.giat.mage.R.drawable.ic_edit);

					final MageTextView mageDateText = new MageTextView(context, null);
					mageDateText.setId(id);
					LinearLayout.LayoutParams mageDateTextLayoutParams = new LinearLayout.LayoutParams((int) LayoutParams.MATCH_PARENT, (int) LayoutParams.WRAP_CONTENT);
					mageDateText.setLayoutParams(mageDateTextLayoutParams);
					mageDateText.setTextAppearance(context, mil.nga.giat.mage.R.style.EditTextView);
					mageDateText.setRequired(required);
					mageDateText.setPropertyKey(name);
					mageDateText.setPropertyType(MagePropertyType.DATE);
					mageDateText.setTextSize(16);

					if (value != null && !value.trim().isEmpty()) {
						try {
							mageDateText.setPropertyValue(DateUtility.getISO8601().parse(value));
						} catch (ParseException pe) {
							Log.e(LOG_NAME, "Problem parsing date.", pe);
						}
					}

					LinearLayout linearLayout = new LinearLayout(context);
					linearLayout.setFocusable(false);
					linearLayout.setLayoutParams(controlParams);
					linearLayout.setOrientation(LinearLayout.HORIZONTAL);
					linearLayout.addView(imageView);
					linearLayout.addView((View) mageDateText);
					linearLayout.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							// Get the layout inflater
							LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
							View dialogView = inflater.inflate(mil.nga.giat.mage.R.layout.date_time_dialog, null);
							final DatePicker datePicker = (DatePicker) dialogView.findViewById(mil.nga.giat.mage.R.id.date_picker);
							final TimePicker timePicker = (TimePicker) dialogView.findViewById(mil.nga.giat.mage.R.id.time_picker);
							// Inflate and set the layout for the dialog
							// Pass null as the parent view because its going in the dialog layout
							builder.setView(dialogView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									Calendar c = Calendar.getInstance();
									c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
									c.set(Calendar.MILLISECOND, 0);
									mageDateText.setPropertyValue(c.getTime());
								}
							}).setNegativeButton(mil.nga.giat.mage.R.string.cancel, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
								}
							});
							AlertDialog ad = builder.create();
							ad.show();
						}
					});

					views.add(textView);
					views.add(linearLayout);

					break;
				default:
					break;
				}
				break;
			case VIEW:
			default:
				MageTextView mageTextView = new MageTextView(context, null);
				mageTextView.setId(id);
				mageTextView.setLayoutParams(controlParams);
				mageTextView.setTextSize(18);
				mageTextView.setFocusable(false);
				mageTextView.setTextIsSelectable(false);
				mageTextView.setClickable(false);
				mageTextView.setPropertyKey(name);
				mageTextView.setPropertyType(MagePropertyType.STRING);
				LinearLayout linearLayout = new LinearLayout(context);
				linearLayout.setLayoutParams(new LinearLayout.LayoutParams((int) LayoutParams.WRAP_CONTENT, (int) LayoutParams.WRAP_CONTENT));
				switch (type) {
				case TEXTFIELD:
				case EMAIL:
				case DROPDOWN:
				case RADIO:
					linearLayout.addView(textView);
					linearLayout.addView((View) mageTextView);
					views.add(linearLayout);
					break;
				case CHECKBOX:
					MageCheckBox mageCheckBox = new MageCheckBox(context, null);
					mageCheckBox.setId(id);
					mageCheckBox.setLayoutParams(controlParams);
					mageCheckBox.setPropertyKey(name);
					mageCheckBox.setPropertyType(MagePropertyType.STRING);
					if(value != null && !value.trim().isEmpty()) {
						mageCheckBox.setPropertyValue(Boolean.valueOf(value));
					}
					mageCheckBox.setEnabled(false);
					linearLayout.addView(textView);
					linearLayout.addView((View) mageCheckBox);
					views.add(linearLayout);
					break;
				case PASSWORD:
					linearLayout.addView(textView);
					mageTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					linearLayout.addView((View) mageTextView);
					views.add(linearLayout);
					break;
				case TEXTAREA:
					mageTextView.setPropertyType(MagePropertyType.MULTILINE);
					mageTextView.setPadding((int) (5 * density), (int) (5 * density), (int) (5 * density), (int) (5 * density));
					linearLayout.addView(textView);
					linearLayout.addView((View) mageTextView);
					views.add(linearLayout);
					break;
				case DATE:
					mageTextView.setPropertyType(MagePropertyType.DATE);
					linearLayout.addView(textView);
					linearLayout.addView((View) mageTextView);
					views.add(linearLayout);
					break;
				default:
					break;
				}
				break;
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
				
				Serializable propertyValue = null;
				if (property != null && property.getValue() != null) {
					propertyValue = property.getValue();
				}
				
				((MageControl) v).setPropertyValue(propertyValue);
			} else if (v instanceof LinearLayout) {
				populateLayoutFromMap((LinearLayout) v, propertiesMap);
			}
		}
	}

	public static void populateLayoutFromBundle(final LinearLayout linearLayout, Bundle savedInstanceState) {
		Map<String, ObservationProperty> propertiesMap = new HashMap<String, ObservationProperty>();
		for (String key : savedInstanceState.keySet()) {
			propertiesMap.put(key, new ObservationProperty(key, savedInstanceState.getSerializable(key)));
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

			if (v instanceof MageControl) {
				MageControl mageControl = (MageControl) v;
				String key = mageControl.getPropertyKey();
				Serializable value = mageControl.getPropertyValue();
				if (key != null && value != null) {
					fields.put(key, new ObservationProperty(key, value));
				}
			} else if (v instanceof LinearLayout) {
				fields.putAll(populateMapFromLayout((LinearLayout) v, fields));
			}
		}
		return fields;
	}

	public static void populateBundleFromLayout(LinearLayout linearLayout, Bundle outState) {
		Map<String, ObservationProperty> properties = populateMapFromLayout(linearLayout);
		for (String key : properties.keySet()) {
			outState.putSerializable(key, properties.get(key).getValue());
		}
	}

	/**
	 * 
	 * @param linearLayout
	 * @return true if there were no issues with the form, false otherwise
	 */
	public static Boolean checkAndFlagRequiredFields(LinearLayout linearLayout) {
		final String error = "Can not be blank";
		Boolean status = true;
		for (int i = linearLayout.getChildCount() - 1; i >= 0; i--) {
			View v = linearLayout.getChildAt(i);

			if (v instanceof MageControl) {
				MageControl mageControl = (MageControl) v;
				if (mageControl.isRequired()) {
					String value = (mageControl.getPropertyValue()==null)?null:mageControl.getPropertyValue().toString();
					Boolean controlStatus = !(value == null || value.isEmpty());
					if (!controlStatus) {
						status = false;
					}
					if (mageControl instanceof MageTextView) {
						MageTextView textView = (MageTextView) v;
						if (!controlStatus) {
							textView.requestFocus();
						}
						textView.setError(controlStatus ? null : error);
					} else if (mageControl instanceof MageEditText) {
						MageEditText editText = (MageEditText) v;
						if (!controlStatus) {
							editText.requestFocus();
						}
						editText.setError(controlStatus ? null : error);
					} else if (mageControl instanceof MageSpinner) {
						// Don't need to check this, as one will already be selected
					} else if (mageControl instanceof MageRadioGroup) {
						// Don't need to check this, as one will already be selected
					} else if (mageControl instanceof MageCheckBox) {
						MageCheckBox checkBox = (MageCheckBox) v;
						if (!controlStatus) {
							checkBox.requestFocus();
						}
						checkBox.setError(controlStatus ? null : error);
					}
				}
			} else if (v instanceof LinearLayout) {
				status = status && checkAndFlagRequiredFields((LinearLayout) v);
			}
		}
		return status;
	}

}
