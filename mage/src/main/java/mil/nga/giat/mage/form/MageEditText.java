package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageEditText extends AppCompatEditText implements MageControl {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;

	public MageEditText(Context context, AttributeSet attrs) {
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
	public Serializable getPropertyValue() {
		switch (propertyType) {
			case NUMBER:
				try {
					return Double.parseDouble(getText().toString());
				} catch (NumberFormatException e) {
					return null;
				}
			default:
				return getText().toString();
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
		if(value == null) {
			return;
		}
		setText(value.toString());
	}

	@Override
	public CharSequence getError() {
		return super.getError();
	}
}
