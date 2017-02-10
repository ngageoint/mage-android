package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

public class MageEditText extends AppCompatEditText implements MageControl {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;
	private Date propertyDate = new Date();
	private final DateFormat iso8601Format = DateFormatFactory.ISO8601();
	private final DateFormat dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault());

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
		Serializable value = null;

		switch (propertyType) {
			case NUMBER:
				try {
					value = Double.parseDouble(getText().toString());
				} catch (NumberFormatException e) {
				}
				break;
			case DATE:
				value = iso8601Format.format(propertyDate);
				break;
			default:
				return getText().toString();
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
		switch (getPropertyType()) {
			case DATE:
				if (value instanceof Date) {
					propertyDate = (Date) value;
				} else if (value instanceof String) {
					try {
						propertyDate = iso8601Format.parse((String) value);
					} catch (ParseException e) {
					}
				}
				setText(dateFormat.format(propertyDate));
				break;
			default:
				if (value != null) {
					setText(value.toString());
				}
		}
	}

	@Override
	public CharSequence getError() {
		return super.getError();
	}
}
