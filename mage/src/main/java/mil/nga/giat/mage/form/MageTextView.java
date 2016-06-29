package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

public class MageTextView extends TextView implements MageControl {

	private static final String LOG_NAME = MageTextView.class.getName();

    private final DateFormat iso8601Format = DateFormatFactory.ISO8601();
	private final DateFormat dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault());

	private String propertyKey;
	private MagePropertyType propertyType;
	private Date propertyDate = new Date();
	private List<String> multiChoiceList = new ArrayList<>();
	protected Boolean isRequired = Boolean.FALSE;

	public MageTextView(Context context, AttributeSet attrs) {
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

		switch (getPropertyType()) {
		case STRING:
		case MULTILINE:
			value = getText().toString();
			break;
		case USER:
			break;
		case DATE:
			value = iso8601Format.format(propertyDate);
			break;
		case LOCATION:
			break;
		case MULTICHOICE:
			value = new ArrayList<>(multiChoiceList);
			break;
		case NUMBER:
			value = Double.parseDouble(getText().toString());
			break;
		default:
			break;
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
			value = "";
		}
		switch (getPropertyType()) {
		case STRING:
		case MULTILINE:
			setText(value.toString());
			break;
		case USER:

			break;
		case DATE:
			if (value instanceof Date) {
				propertyDate = (Date) value;
			} else if (value instanceof String) {
				try {
					propertyDate = iso8601Format.parse((String) value);
				} catch (ParseException e) {
					Log.e(LOG_NAME, "Could not parse date: " + value);
				}
			}
			setText(dateFormat.format(propertyDate));
			break;
		case LOCATION:
			// location is not a property, it lives in the parent
			break;
		case MULTICHOICE:
			multiChoiceList = new ArrayList<>((ArrayList<String>)value);
			setText(value.toString().substring(1, value.toString().length() - 1));
			break;
		default:
			break;
		}
	}

	@Override
	public CharSequence getError() {
		return super.getError();
	}
}
