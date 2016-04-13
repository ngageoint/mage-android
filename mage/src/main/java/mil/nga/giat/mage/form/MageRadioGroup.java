package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageRadioGroup extends RadioGroup implements MageControl {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;

	public MageRadioGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
		setPropertyKey(typedArray.getString(R.styleable.MageFormElement_propertyKey));
		setPropertyType(MagePropertyType.getPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0)));
		typedArray.recycle();
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
		String value = null;
		RadioButton radioButton = (RadioButton) findViewById(getCheckedRadioButtonId());
		if (radioButton != null) {
			value = (String) radioButton.getText();
		}
		return value;
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
		if(value == null) {
			return;
		}
		int j = 0;
		for (int index = 0; index < getChildCount(); index++) {
			RadioButton radioButton = (RadioButton) getChildAt(index);
			// default
			if(j++ == 0) {
				radioButton.setChecked(true);	
			} else {
				radioButton.setChecked(radioButton.getText().equals(value));
			}
		}
	}

	@Override
	public CharSequence getError() {
		return null;
	}
}
