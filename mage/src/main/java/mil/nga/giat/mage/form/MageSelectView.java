package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class MageSelectView extends AppCompatEditText implements MageControl {

    private String propertyKey;
    private MagePropertyType propertyType;
    private JsonObject jsonObject;
    private static String DEFAULT_TEXT = "";
    private ArrayList<String> selectedChoices = new ArrayList<>();;
    private Boolean isMultiSelect = false;
    protected Boolean isRequired = Boolean.FALSE;


    public MageSelectView(Context context, AttributeSet attrs, JsonObject jsonObject, Boolean isMultiSelect) {
        super(context, attrs);
        this.jsonObject = jsonObject;
        this.isMultiSelect = isMultiSelect;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
        setPropertyKey(typedArray.getString(R.styleable.MageFormElement_propertyKey));
        setPropertyType(MagePropertyType.getPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0)));
        typedArray.recycle();
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
        // If multi select return ArrayList else return string
        if (isMultiSelect) {
            return selectedChoices;
        } else {
            return selectedChoices.isEmpty() ? "" : selectedChoices.get(0);
        }
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
        selectedChoices = new ArrayList<>();
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
                    setError(null);
                } else {
                    setText(DEFAULT_TEXT);
                }
            } else {
                selectedChoices.add((String) value);
                setText((String) value);

                if (StringUtils.isNoneBlank((String) value)) {
                    setError(null);
                }
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

