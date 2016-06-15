package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class MageSelectView extends EditText implements MageControl {

    private String propertyKey;
    private MagePropertyType propertyType;
    private JsonObject jsonObject;
    private static String DEFAULT_TEXT = "";
    private ArrayList<String> selectedChoices;
    private Boolean isMultiSelect = false;
    protected Boolean isRequired = Boolean.FALSE;


    public MageSelectView(Context context, AttributeSet attrs, JsonObject jsonObject, Boolean isMultiSelect) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
        setPropertyKey(typedArray.getString(R.styleable.MageFormElement_propertyKey));
        setPropertyType(MagePropertyType.getPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0)));
        typedArray.recycle();
        this.jsonObject = jsonObject;
        this.selectedChoices = new ArrayList<String>();
        this.isMultiSelect = isMultiSelect;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public Boolean isMultiSelect() {
        return isMultiSelect;
    }

    @Override
    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    @Override
    public String getPropertyKey() {
        return this.propertyKey;
    }

    @Override
    public void setPropertyType(MagePropertyType propertyType) {
        this.propertyType = propertyType;
    }

    @Override
    public MagePropertyType getPropertyType() {
        return this.propertyType;
    }

    @Override
    public Serializable getPropertyValue() {
        Serializable returnValue = null;
        //if multi select return ArrayList else return string
        if (!selectedChoices.isEmpty()) {
            if (isMultiSelect) {
                returnValue = selectedChoices;
            } else {
                returnValue = selectedChoices.get(0);
            }
        }
        return returnValue;
    }

    @Override
    public Boolean isRequired() {
        return isRequired;
    }

    @Override
    public void setRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    @Override
    public void setPropertyValue(Serializable value) {
        selectedChoices = new ArrayList<String>();
        if (value != null) {
            if (isMultiSelect) {
                selectedChoices = (ArrayList<String>) value;
                if (!selectedChoices.isEmpty()) {
                    StringBuilder displayValue = new StringBuilder();
                    for (int count = 0; count < selectedChoices.size(); count++) {
                        if (count < selectedChoices.size() - 1) {
                            displayValue.append(selectedChoices.get(count) + ", ");
                        } else {
                            displayValue.append(selectedChoices.get(count));
                        }
                    }
                    setText(displayValue.toString());
                } else {
                    setText(DEFAULT_TEXT);
                }
            } else {
                selectedChoices.add((String) value);
                setText((String) value);
            }
        } else {
            setText(DEFAULT_TEXT);
        }
    }

    @Override
    public CharSequence getError() {
        return super.getError();
    }
}

