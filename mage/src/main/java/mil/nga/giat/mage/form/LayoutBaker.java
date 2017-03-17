package mil.nga.giat.mage.form;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.DateTimePickerDialog;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;

/**
 * Use this class to build and populate the views concerned with form like information.
 *
 * @author wiedemanns
 *
 */
public class LayoutBaker {

	private static final String LOG_NAME = LayoutBaker.class.getName();

	private static final String EXTRA_PROPERTY_MAP = "EXTRA_PROPERTY_MAP";

	public enum ControlGenerationType {
		VIEW, EDIT
	}

	public static List<View> createControlsFromJson(final AppCompatActivity activity, ControlGenerationType controlGenerationType, JsonObject dynamicFormJson) {
		// add the theme to the context
		final ContextThemeWrapper context = new ContextThemeWrapper(activity, R.style.AppTheme_PrimaryAccent);

		List<View> views = new ArrayList<>();

		JsonArray dynamicFormFields = dynamicFormJson.get("fields").getAsJsonArray();

		Map<Integer, JsonObject> dynamicFormFieldsCollection = new TreeMap<>();

		for (int i = 0; i < dynamicFormFields.size(); i++) {
			JsonObject field = dynamicFormFields.get(i).getAsJsonObject();
			Integer id = field.get("id").getAsInt();
			dynamicFormFieldsCollection.put(id, field);
		}

		int uniqueChildIdIndex = 10000;

		int i = -1;
		for (Integer id : dynamicFormFieldsCollection.keySet()) {
			i++;
			JsonObject field = dynamicFormFieldsCollection.get(id);

			// get members
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
			Serializable value = null;
			JsonElement jsonValue = field.get("value");
			if (jsonValue != null && !jsonValue.isJsonNull()) {
				if (jsonValue.isJsonPrimitive()) {
					value = jsonValue.getAsString();
				} else if (jsonValue.isJsonArray()) {
					JsonArray jsonArray = (JsonArray) jsonValue;
					ArrayList<String> stringArrayList = new ArrayList<>();
					for (JsonElement element: jsonArray) {
						if (!element.isJsonNull()) {
							stringArrayList.add(element.getAsString());
						}
					}
					value = stringArrayList;
				}
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
			Collection<String> choices = new LinkedHashSet<>();
			if (choicesJson != null && !choicesJson.isJsonNull()) {
				for (int j = 0; j < choicesJson.size(); j++) {
					JsonObject choiceJson = choicesJson.get(j).getAsJsonObject();
					JsonElement choiceElement = choiceJson.get("title");
					if (choiceElement != null) {
	                   String choiceTitle = choiceElement.getAsString();
	                    if (choiceTitle != null) {
	                        choices.add(choiceTitle);
	                    }
					} else {
					    choices.add("");
					}

				}
			}

			final float density = context.getResources().getDisplayMetrics().density;

			LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

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
				textView.setTextAppearance(context, mil.nga.giat.mage.R.style.ObservationPropertyLabel);
				break;
			}
			textView.setLayoutParams(textParams);

			switch (controlGenerationType) {
			case EDIT:
				final MageEditText editText = new MageEditText(context, null);
				editText.setLayoutParams(controlParams);
				editText.setId(id);
				editText.setHint(title);
				editText.setRequired(required);
				editText.setPropertyKey(name);
				editText.setPropertyValue(value);

				switch (type) {
				case TEXTFIELD:
				case EMAIL:
					editText.setPropertyType(MagePropertyType.STRING);
					views.add(editText);
					break;
				case NUMBERFIELD:
					final MageNumberControl numberControl = new MageNumberControl(context, null, field.get("min").getAsDouble(), field.get("max").getAsDouble());
					numberControl.setLayoutParams(controlParams);
					numberControl.setId(id);
					numberControl.setHint(title);
					numberControl.setRequired(required);
					numberControl.setPropertyKey(name);
					numberControl.setPropertyValue(value);
					views.add(numberControl);
					break;
				case PASSWORD:
					editText.setPropertyType(MagePropertyType.STRING);
					editText.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					editText.setPasswordVisibilityToggleEnabled(true);
					views.add(editText);
					break;
				case TEXTAREA:
					editText.setPropertyType(MagePropertyType.MULTILINE);
					views.add(editText);
					break;
				case RADIO:
					MageRadioGroup mageRadioGroup = new MageRadioGroup(context, null);
					mageRadioGroup.setId(id);
					mageRadioGroup.setLayoutParams(controlParams);
					mageRadioGroup.setRequired(required);
					mageRadioGroup.setPropertyKey(name);
					mageRadioGroup.setPropertyType(MagePropertyType.MULTICHOICE);

					for (String choice : choices) {
						AppCompatRadioButton radioButton = new AppCompatRadioButton(context);
						radioButton.setId(uniqueChildIdIndex++);
						radioButton.setText(choice);
						mageRadioGroup.addRadioButton(radioButton);
					}

					mageRadioGroup.setPropertyValue(value);

					views.add(textView);
					views.add(mageRadioGroup);
					break;
				case CHECKBOX:
					MageCheckBox checkBox = new MageCheckBox(context, null);
					checkBox.setId(id);
					checkBox.setLayoutParams(controlParams);
					checkBox.setRequired(required);
					checkBox.setPropertyKey(name);
					checkBox.setPropertyType(MagePropertyType.STRING);
					if (value != null && !((String)value).trim().isEmpty()) {
						checkBox.setPropertyValue(Boolean.valueOf(((String)value)));
					}

					views.add(textView);
					views.add(checkBox);
					break;
				case DATE:
					// don't create the timestamp control on the edit page
					if (name.equals("timestamp")) {
						break;
					}

					final MageEditText mageDateText = new MageEditText(context, null);
					mageDateText.setId(id);
					mageDateText.setLayoutParams(controlParams);
					mageDateText.setHint(title);
					mageDateText.getEditText().setFocusableInTouchMode(false);
					mageDateText.getEditText().setFocusable(true);
					mageDateText.getEditText().setTextIsSelectable(false);
					mageDateText.getEditText().setCursorVisible(false);
					mageDateText.getEditText().setClickable(false);
					mageDateText.setRequired(required);
					mageDateText.setPropertyKey(name);
					mageDateText.setPropertyType(MagePropertyType.DATE);

					if (value != null && !((String)value).trim().isEmpty()) {
						try {
                            DateFormat dateFormat = ISO8601DateFormatFactory.ISO8601();
							mageDateText.setPropertyValue(dateFormat.parse(value.toString()));
						} catch (ParseException pe) {
							Log.e(LOG_NAME, "Problem parsing date.", pe);
						}
					}

					mageDateText.getEditText().setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Date date = null;
							try {
								date = ISO8601DateFormatFactory.ISO8601().parse(mageDateText.getPropertyValue().toString());
							} catch (ParseException pe) {
								Log.e(LOG_NAME, "Problem parsing date.", pe);
							}

							DateTimePickerDialog dialog = DateTimePickerDialog.newInstance(date);
							dialog.setOnDateTimeChangedListener(new DateTimePickerDialog.OnDateTimeChangedListener() {
								@Override
								public void onDateTimeChanged(Date date) {
									mageDateText.setPropertyValue(date);
								}
							});

							FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
							dialog.show(ft, "DATE_TIME_PICKER_DIALOG");
						}
					});

					views.add(mageDateText);
					break;
				case DROPDOWN:
					MageSelectView selectView = new MageSelectView(context, null, field, false);
					selectView.setId(id);
					selectView.getEditText().setHint(title);
					selectView.setLayoutParams(controlParams);
					selectView.setRequired(required);
					selectView.setPropertyKey(name);
					selectView.setPropertyType(MagePropertyType.STRING);
					selectView.setPropertyValue(value);

					views.add(selectView);
					break;
				case MULTISELECTDROPDOWN:
					MageSelectView multiSelectView = new MageSelectView(context, null, field, true);
					multiSelectView.setId(id);
					multiSelectView.getEditText().setHint(title);
					multiSelectView.setLayoutParams(controlParams);
					multiSelectView.setRequired(required);
					multiSelectView.setPropertyKey(name);
					multiSelectView.setPropertyType(MagePropertyType.STRING);
					multiSelectView.setPropertyValue(value);

					views.add(multiSelectView);
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
				mageTextView.setFocusable(false);
				mageTextView.setTextIsSelectable(false);
				mageTextView.setClickable(false);
				mageTextView.setPropertyKey(name);
				mageTextView.setPropertyType(MagePropertyType.STRING);
				mageTextView.setTextAppearance(context, mil.nga.giat.mage.R.style.ObservationPropertyValue);
				mageTextView.setPadding((int) (5 * density), (int) (5 * density), (int) (5 * density), (int) (5 * density));

				LinearLayout linearLayout = new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				switch (type) {
				case TEXTFIELD:
				case NUMBERFIELD:
				case EMAIL:
				case RADIO:
					linearLayout.addView(textView);
					linearLayout.addView(mageTextView);
					views.add(linearLayout);
					break;
				case CHECKBOX:
					MageCheckBox mageCheckBox = new MageCheckBox(context, null);
					mageCheckBox.setId(id);
					mageCheckBox.setLayoutParams(controlParams);
					mageCheckBox.setPropertyKey(name);
					mageCheckBox.setPropertyType(MagePropertyType.STRING);
					if(value != null && !((String)value).trim().isEmpty()) {
						mageCheckBox.setPropertyValue(Boolean.valueOf(((String)value)));
					}
					mageCheckBox.setEnabled(false);
					linearLayout.addView(textView);
					linearLayout.addView(mageCheckBox);
					views.add(linearLayout);
					break;
				case PASSWORD:
					linearLayout.addView(textView);
					mageTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					linearLayout.addView(mageTextView);
					views.add(linearLayout);
					break;
				case TEXTAREA:
					mageTextView.setPropertyType(MagePropertyType.MULTILINE);
					views.add(textView);
					views.add(mageTextView);
					break;
				case DATE:
					mageTextView.setPropertyType(MagePropertyType.DATE);
					linearLayout.addView(textView);
					linearLayout.addView(mageTextView);
					views.add(linearLayout);
					break;
				case DROPDOWN:
					mageTextView.setPropertyType(MagePropertyType.STRING);
					views.add(textView);
					views.add(mageTextView);
					break;
				case MULTISELECTDROPDOWN:
					mageTextView.setPropertyType(MagePropertyType.MULTICHOICE);
					views.add(textView);
					views.add(mageTextView);
					break;
				default:
					break;
				}
				break;
			}
		}

		return views;
	}

	public static void populateLayoutWithControls(final LinearLayout linearLayout, Collection<View> controls) {
		for (View control : controls) {
			linearLayout.addView(control);
		}
	}

	public static List<View> validateControls(Collection<View> views) {
		List<View> invalid = new ArrayList<>();

		for (View view : views) {
			if (view instanceof MageControl) {
				MageControl control = (MageControl) view;
				boolean valid = control.validate();

				if (!valid) {
					invalid.add(view);
				}
			}
		}

		return invalid;
	}

	/**
	 * Populates the linearLayout from the key, value pairs in the propertiesMap
	 *
	 * @param linearLayout
	 * @param propertiesMap
	 */
	public static void populateLayoutFromMap(final LinearLayout linearLayout, ControlGenerationType controlGenerationType, final Map<String, ObservationProperty> propertiesMap) {
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

				mageControl.setPropertyValue(propertyValue);

				View textView = linearLayout.getChildAt(Math.max(0, i - 1));
				if(textView != null && textView instanceof TextView) {
					textView.setVisibility(View.VISIBLE);
				}
				v.setVisibility(View.VISIBLE);

				if(controlGenerationType.equals(ControlGenerationType.VIEW) && v instanceof MageTextView &&
						(propertyValue == null ||
								(propertyValue instanceof String && StringUtils.isBlank((String)propertyValue)) ||
								(propertyValue instanceof Collection && ((Collection) propertyValue).isEmpty())
						)) {
					textView = linearLayout.getChildAt(Math.max(0, i - 1));
					if(textView != null && textView instanceof TextView) {
						textView.setVisibility(View.GONE);
					}
					v.setVisibility(View.GONE);
				}

			} else if (v instanceof LinearLayout) {
				populateLayoutFromMap((LinearLayout) v, controlGenerationType, propertiesMap);
			}
		}
	}

	public static void populateLayoutFromBundle(final LinearLayout linearLayout, ControlGenerationType controlGenerationType, Bundle savedInstanceState) {
		Map<String, ObservationProperty> propertiesMap = new HashMap<>();
		for (Map.Entry<String, Serializable> entry : ((Map<String, Serializable>) savedInstanceState.getSerializable(EXTRA_PROPERTY_MAP)).entrySet()) {
			propertiesMap.put(entry.getKey(), new ObservationProperty(entry.getKey(), entry.getValue()));
		}

		populateLayoutFromMap(linearLayout, controlGenerationType, propertiesMap);
	}

	/**
	 * Returns a map of key value pairs form the layout
	 *
	 * @param linearLayout
	 * @return
	 */
	public static Map<String, ObservationProperty> populateMapFromLayout(LinearLayout linearLayout) {
		Map<String, ObservationProperty> properties = new HashMap<>();
		return populateMapFromLayout(linearLayout, properties);
	}

	private static Map<String, ObservationProperty> populateMapFromLayout(LinearLayout linearLayout, Map<String, ObservationProperty> fields) {
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
		HashMap<String, Serializable> properties = new HashMap<>();
		for (Map.Entry<String, ObservationProperty> entry : populateMapFromLayout(linearLayout).entrySet()) {
			properties.put(entry.getKey(), entry.getValue().getValue());
		}

		outState.putSerializable(EXTRA_PROPERTY_MAP, properties);
	}

}
