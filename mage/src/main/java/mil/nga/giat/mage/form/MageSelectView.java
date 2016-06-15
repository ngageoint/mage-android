package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.JsonObject;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageSelectView extends ListView implements MageControl {

    private String propertyKey;
    private MagePropertyType propertyType;
    private JsonObject jsonObject;
    protected Boolean isRequired = Boolean.FALSE;


    public MageSelectView(Context context, AttributeSet attrs, JsonObject jsonObject) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
        setPropertyKey(typedArray.getString(R.styleable.MageFormElement_propertyKey));
        setPropertyType(MagePropertyType.getPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0)));
        typedArray.recycle();
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
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
    public String getPropertyValue() {
        return (String) getSelectedItem();
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
        if (value != null) {
            setSelection(Math.max(0, ((ArrayAdapter<String>) getAdapter()).getPosition(value.toString())));
        } else {
            setSelection(0);
        }
    }
}

