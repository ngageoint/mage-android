package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageNumberControl extends TextInputLayout implements MageControl, TextWatcher {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;
	private double min;
	private double max;

	public MageNumberControl(Context context, AttributeSet attrs, double min, double max) {
		super(context, attrs);

		this.min = min;
		this.max = max;
		this.propertyType = MagePropertyType.NUMBER;

		AppCompatEditText editText = new AppCompatEditText(context, attrs);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		editText.addTextChangedListener(this);
		addView(editText);

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
	public Serializable getPropertyValue() {
		Serializable value = null;

		try {
			value = Double.parseDouble(getEditText().getText().toString());
		} catch (NumberFormatException e) {
		}

		return value;
	}

	@Override
	public void setRequired(Boolean isRequired) {
		this.isRequired = isRequired;
	}

	@Override
	public void setPropertyValue(Serializable value) {
		if (value != null) {
			getEditText().setText(value.toString());
		}
	}

	@Override
	public boolean validate() {
		Serializable value = getPropertyValue();

		String error = null;

		if (isRequired && value == null) {
			error = "Required, cannot be blank";
		}

		if (value != null) {
			try {
				double number = (Double) value;
				if (number < min) {
					error = "Must be greater than " + min;
				} else if (number > max) {
					error = "Must be less than " + max;
				}
			} catch (Exception e) {
				error = "Value must be a number";
			}
		}

		setError(error);

		return error == null;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (StringUtils.isNoneBlank(s)) {
			validate();
		}
	}

	@Override
	public void afterTextChanged(Editable s) {

	}
}
