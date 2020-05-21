package mil.nga.giat.mage.observation;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import mil.nga.giat.mage.R;

/**
 * Observation shape style for lines and polygons including stroke width, stroke color, and fill color
 *
 * @author osbornb
 */
public class ObservationShapeStyle {

    /**
     * Display metrics
     */
    private final DisplayMetrics displayMetrics;

    /**
     * Stroke width for lines and polygons
     */
    private float strokeWidth;

    /**
     * Stroke color for lines and polygons
     */
    private int strokeColor;

    /**
     * Fill color for polygons
     */
    private int fillColor;

    /**
     * Constructor
     *
     * @param context application context
     */
    public ObservationShapeStyle(Context context) {

        this.displayMetrics = context.getResources().getDisplayMetrics();

        TypedValue defaultStrokeWidth = new TypedValue();
        context.getResources().getValue(R.dimen.fill_default_stroke_width, defaultStrokeWidth, true);
        setStrokeWidth(defaultStrokeWidth.getFloat());
        setStrokeColor(ContextCompat.getColor(context, R.color.line_default_color));
        setFillColor(ContextCompat.getColor(context, R.color.fill_default_color));
    }

    /**
     * Get the stroke width
     *
     * @return stroke width
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Set the stroke width
     *
     * @param strokeWidth stroke width
     */
    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Get the stroke color
     *
     * @return stroke color
     */
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * Set the stroke color
     *
     * @param strokeColor stroke color
     */
    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    /**
     * Get the fill color
     *
     * @return fill color
     */
    public int getFillColor() {
        return fillColor;
    }

    /**
     * Set the fill color
     *
     * @param fillColor fill color
     */
    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

}
