package mil.nga.giat.mage.observation;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Input filter to enforce decimal ranges and precision
 *
 * @author osbornb
 */
public class InputFilterDecimal implements InputFilter {

    private Double min;
    private Double max;
    private Integer precision;

    public InputFilterDecimal() {
    }

    public InputFilterDecimal(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public InputFilterDecimal(int precision) {
        this.precision = precision;
    }

    public InputFilterDecimal(double min, double max, int precision) {
        this.min = min;
        this.max = max;
        this.precision = precision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        String value = dest.subSequence(0, dstart).toString()
                + source.subSequence(start, end)
                + dest.subSequence(dend, dest.length());
        if (value.isEmpty() || value.equals("-")) {
            return null;
        }
        double input;
        try {
            input = Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            return dest.subSequence(dstart, dend);
        }
        if (min != null && min > input
                || max != null && max < input) {
            return dest.subSequence(dstart, dend);
        }

        if (precision != null) {
            int decimalIndex = value.indexOf(".");
            if (decimalIndex > -1 && value.length() - decimalIndex - 1 > precision) {
                return dest.subSequence(dstart, dend);
            }
        }

        return null;
    }

}
