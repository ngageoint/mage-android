package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import mil.nga.giat.mage.utils.DateFormatFactory;

public class MageEditText extends TextInputLayout implements MageControl, TextWatcher {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;
	private Date propertyDate = null;
	private final DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();
	private DateFormat dateFormat;

	public MageEditText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MageEditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs);

		dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context);
		AppCompatEditText editText = new AppCompatEditText(context, attrs);
		addView(editText);

		editText.addTextChangedListener(this);

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
		Serializable value = "";

		switch (propertyType) {
			case DATE:
				if (propertyDate != null) {
					value = iso8601Format.format(propertyDate);
				}
				break;
			default:
				value = getEditText().getText().toString();
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
			getEditText().setText(null);
			return;
		}

		switch (getPropertyType()) {
			case DATE:
				if (value instanceof Date) {
					propertyDate = (Date) value;
				} else if (value instanceof String) {
					try {
						propertyDate = iso8601Format.parse((String) value);
					} catch (ParseException e) {
						return;
					}
				}
				getEditText().setText(dateFormat.format(propertyDate));
				break;
			default:
				getEditText().setText(value.toString());
		}
	}

	@Override
	public boolean validate() {
		Serializable value = getPropertyValue();

		String error = null;
		if (isRequired && (value == null || StringUtils.isBlank(value.toString()))) {
			error = "Required, cannot be blank";
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
