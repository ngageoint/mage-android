package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageRadioGroup extends TextInputLayout implements MageControl, RadioGroup.OnCheckedChangeListener{

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;
	private RadioGroup radioGroup;

	public MageRadioGroup(Context context, AttributeSet attrs) {
		super(context, attrs);

		radioGroup = new RadioGroup(context, attrs);
		radioGroup.setOnCheckedChangeListener(this);
		addView(radioGroup);

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
		AppCompatRadioButton radioButton = (AppCompatRadioButton) findViewById(radioGroup.getCheckedRadioButtonId());
		if (radioButton != null) {
			value = (String) radioButton.getText();
		}
		return value;
	}

	@Override
	public void setRequired(Boolean isRequired) {
		this.isRequired = isRequired;
	}

	@Override
	public void setPropertyValue(Serializable value) {
		if (value == null) {
			return;
		}

		for (int index = 0; index < radioGroup.getChildCount(); index++) {
			RadioButton radioButton = (RadioButton) radioGroup.getChildAt(index);
			radioButton.setChecked(radioButton.getText().equals(value));
		}
	}

	@Override
	public boolean validate() {
		Serializable value = getPropertyValue();

		String error = null;
		if (isRequired && value == null) {
			error = "Required, cannot be blank";
		}

		setError(error);

		return error == null;
	}

	public void addRadioButton(AppCompatRadioButton radioButton) {
		radioGroup.addView(radioButton);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		validate();
	}
}
