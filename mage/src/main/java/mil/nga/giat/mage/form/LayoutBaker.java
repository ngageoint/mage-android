package mil.nga.giat.mage.form;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

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

	public static List<View> createControlsFromJson(Context pContext, ControlGenerationType controlGenerationType, JsonObject dynamicFormJson) {
		// add the theme to the context
		final Context context = new ContextThemeWrapper(pContext, R.style.AppTheme);

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
				textView.setTextAppearance(context, mil.nga.giat.mage.R.style.ViewTextView);
				break;
			}
			textView.setLayoutParams(textParams);

			// FIXME: set required, add remaining controls
			switch (controlGenerationType) {
			case EDIT:
				final MageEditText mageEditText = new MageEditText(context, null);
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
					views.add(mageEditText);
					break;
				case NUMBERFIELD:
					final double min = field.get("min").getAsDouble();
					final double max = field.get("max").getAsDouble();

					mageEditText.setPropertyType(MagePropertyType.NUMBER);
					mageEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
					mageEditText.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}

						@Override
						public void afterTextChanged(Editable s) {
							if (s.toString().isEmpty()) {
								return;
							}

							try {
								double value = Double.parseDouble(s.toString());
								if (value < min) {
									mageEditText.setError("Must be greater than " + min);
								} else if (value > max) {
									mageEditText.setError("Must be less than " + max);
								}
							} catch (NumberFormatException e) {
								mageEditText.setError("Value must be a number");
							}
						}
					});

					views.add(textView);
					views.add(mageEditText);
					break;
				case PASSWORD:
					mageEditText.setPropertyType(MagePropertyType.STRING);
					views.add(textView);
					mageEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					views.add(mageEditText);
					break;
				case TEXTAREA:
					mageEditText.setMinLines(2);
					mageEditText.setPropertyType(MagePropertyType.MULTILINE);
					views.add(textView);
					views.add(mageEditText);
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
					views.add(mageRadioGroup);
					break;
				case CHECKBOX:
					MageCheckBox mageCheckBox = new MageCheckBox(context, null);
					mageCheckBox.setId(id);
					mageCheckBox.setLayoutParams(controlParams);
					mageCheckBox.setRequired(required);
					mageCheckBox.setPropertyKey(name);
					mageCheckBox.setPropertyType(MagePropertyType.STRING);
					if(value != null && !((String)value).trim().isEmpty()) {
						mageCheckBox.setPropertyValue(Boolean.valueOf(((String)value)));
					}

					views.add(textView);
					views.add((View) mageCheckBox);
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
					imageView.setImageResource(mil.nga.giat.mage.R.drawable.ic_mode_edit_black_24dp);

					final MageTextView mageDateText = new MageTextView(context, null);
					mageDateText.setId(id);
					LinearLayout.LayoutParams mageDateTextLayoutParams = new LinearLayout.LayoutParams((int) LayoutParams.MATCH_PARENT, (int) LayoutParams.WRAP_CONTENT);
					mageDateText.setLayoutParams(mageDateTextLayoutParams);
					mageDateText.setTextAppearance(context, mil.nga.giat.mage.R.style.EditTextView);
					mageDateText.setRequired(required);
					mageDateText.setPropertyKey(name);
					mageDateText.setPropertyType(MagePropertyType.DATE);
					mageDateText.setTextSize(16);

					if (value != null && !((String)value).trim().isEmpty()) {
						try {
                            DateFormat dateFormat = DateFormatFactory.ISO8601();
							mageDateText.setPropertyValue(dateFormat.parse(((String)value)));
						} catch (ParseException pe) {
							Log.e(LOG_NAME, "Problem parsing date.", pe);
						}
					}

					LinearLayout linearLayout = new LinearLayout(context);
					linearLayout.setFocusable(false);
					linearLayout.setLayoutParams(controlParams);
					linearLayout.setOrientation(LinearLayout.HORIZONTAL);
					linearLayout.addView(imageView);
					linearLayout.addView(mageDateText);
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
				case DROPDOWN:
						MageSelectView mageSingleSelectView = new MageSelectView(context, null, field, false);
						mageSingleSelectView.setId(id);
						mageSingleSelectView.setLayoutParams(controlParams);
						mageSingleSelectView.setRequired(required);
						mageSingleSelectView.setPropertyKey(name);
						mageSingleSelectView.setPropertyType(MagePropertyType.STRING);
						mageSingleSelectView.setPropertyValue(value);
						mageSingleSelectView.setFocusable(false);
						mageSingleSelectView.setTextIsSelectable(false);
						mageSingleSelectView.setClickable(true);
						mageSingleSelectView.setTextSize(18);
						views.add(textView);
						views.add(mageSingleSelectView);

						break;
				case MULTISELECTDROPDOWN:
						MageSelectView mageMultiSelectView = new MageSelectView(context, null, field, true);
						mageMultiSelectView.setId(id);
						mageMultiSelectView.setLayoutParams(controlParams);
						mageMultiSelectView.setRequired(required);
						mageMultiSelectView.setPropertyKey(name);
						mageMultiSelectView.setPropertyType(MagePropertyType.MULTICHOICE);
						mageMultiSelectView.setPropertyValue(value);
						mageMultiSelectView.setFocusable(false);
						mageMultiSelectView.setTextIsSelectable(false);
						mageMultiSelectView.setClickable(true);
						mageMultiSelectView.setTextSize(18);
						views.add(textView);
						views.add(mageMultiSelectView);
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
					mageTextView.setPadding((int) (5 * density), (int) (5 * density), (int) (5 * density), (int) (5 * density));
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
					mageTextView.setPadding((int) (5 * density), (int) (5 * density), (int) (5 * density), (int) (5 * density));
					mageTextView.setPropertyType(MagePropertyType.STRING);
					views.add(textView);
					views.add(mageTextView);
					break;
				case MULTISELECTDROPDOWN:
					mageTextView.setPadding((int) (5 * density), (int) (5 * density), (int) (5 * density), (int) (5 * density));
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
			if (view instanceof  MageControl) {
				if (((MageControl) view).getError() != null) {
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

				if(controlGenerationType.equals(ControlGenerationType.VIEW) && v instanceof MageTextView && (propertyValue == null || (propertyValue instanceof String && StringUtils.isBlank((String)propertyValue)))) {
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
