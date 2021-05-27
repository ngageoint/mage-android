package mil.nga.giat.mage.observation;

import android.content.Context;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import mil.nga.giat.mage.form.FormState;
import mil.nga.giat.mage.form.field.FieldState;
import mil.nga.giat.mage.form.field.FieldValue;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;

/**
 * Parses the observation form json and retrieves the style
 *
 * @author osbornb
 */
public class ObservationShapeStyleParser {

    /**
     * Style element
     */
    private static final String STYLE_ELEMENT = "style";

    /**
     * Observation type property
     */
    private static final String OBSERVATION_TYPE_PROPERTY = "type";

    /**
     * Variant field element
     */
    private static final String VARIANT_FIELD_ELEMENT = "variantField";

    /**
     * Style fill element
     */
    private static final String FILL_ELEMENT = "fill";

    /**
     * Style stroke element
     */
    private static final String STROKE_ELEMENT = "stroke";

    /**
     * Style fill opacity element
     */
    private static final String FILL_OPACITY_ELEMENT = "fillOpacity";

    /**
     * Style stroke opacity element
     */
    private static final String STROKE_OPACITY_ELEMENT = "strokeOpacity";

    /**
     * Style stroke width element
     */
    private static final String STROKE_WIDTH_ELEMENT = "strokeWidth";

    /**
     * Max alpha value
     */
    private static final float MAX_ALPHA = 255.0f;

    /**
     * Get the observation style
     *
     * @param context     application context
     * @param observation observation
     * @return shape style
     */
    public static ObservationShapeStyle getStyle(Context context, Observation observation) {

        ObservationShapeStyle style = new ObservationShapeStyle(context);

        // Check for a style
        JsonElement styleField = observation.getStyle();
        if (styleField != null && !styleField.isJsonNull()) {

            // Found the top level style
            JsonObject styleObject = styleField.getAsJsonObject();

            ObservationProperty primaryProperty = observation.getPrimaryMapField();
            if (primaryProperty != null) {
                String primary = primaryProperty.getValue().toString();

                // Check for a type within the style
                JsonElement primaryField = styleObject.get(primary);
                if (primaryField != null && !primaryField.isJsonNull()) {

                    // Found the type level style
                    styleObject = primaryField.getAsJsonObject();

                    ObservationProperty variantProperty = observation.getSecondaryMapField();
                    if (variantProperty != null) {
                        String variant = variantProperty.getValue().toString();

                        // Check for a variant within the style type
                        JsonElement typeVariantField = styleObject.get(variant);
                        if (typeVariantField != null && !typeVariantField.isJsonNull()) {

                            // Found the variant level style
                            styleObject = typeVariantField.getAsJsonObject();
                        }
                    }
                }
            }

            // Get the style properties
            String fill = styleObject.get(FILL_ELEMENT).getAsString();
            String stroke = styleObject.get(STROKE_ELEMENT).getAsString();
            float fillOpacity = styleObject.get(FILL_OPACITY_ELEMENT).getAsFloat();
            float strokeOpacity = styleObject.get(STROKE_OPACITY_ELEMENT).getAsFloat();
            float strokeWidth = styleObject.get(STROKE_WIDTH_ELEMENT).getAsFloat();

            // Set the stroke width
            style.setStrokeWidth(strokeWidth);

            // Create and set the stroke color
            int strokeColor = Color.parseColor(stroke);
            int strokeColorWithAlpha = ColorUtils.setAlphaComponent(strokeColor, getAlpha(strokeOpacity));
            style.setStrokeColor(strokeColorWithAlpha);

            // Create and set the fill color
            int fillColor = Color.parseColor(fill);
            int fillColorWithAlpha = ColorUtils.setAlphaComponent(fillColor, getAlpha(fillOpacity));
            style.setFillColor(fillColorWithAlpha);
        }

        return style;
    }

    public static ObservationShapeStyle getStyle(Context context, FormState formState) {

        ObservationShapeStyle style = new ObservationShapeStyle(context);

        // Check for a style
        if (formState != null) {

            // Found the top level style
            JsonObject styleObject = formState.getDefinition().getStyle();

            FieldValue.Text primaryValue = null;
            for (FieldState<?, ?> fieldState : formState.getFields()) {
                if (fieldState.getDefinition().getName().equals(formState.getDefinition().getPrimaryMapField()) &&
                    fieldState.getAnswer() instanceof FieldValue.Text) {
                    primaryValue = ((FieldValue.Text) fieldState.getAnswer());
                    break;
                }
            }

            FieldValue.Text secondaryValue = null;
            for (FieldState<?, ?> fieldState : formState.getFields()) {
                if (fieldState.getDefinition().getName().equals(formState.getDefinition().getSecondaryMapField()) &&
                    fieldState.getAnswer() instanceof FieldValue.Text){
                    secondaryValue = ((FieldValue.Text) fieldState.getAnswer());
                    break;
                }
            }

            if (primaryValue != null) {
                // Check for a type within the style
                JsonElement primaryElement = styleObject.get(primaryValue.getText());
                if (primaryElement != null && !primaryElement.isJsonNull()) {

                    // Found the type level style
                    styleObject = primaryElement.getAsJsonObject();

                    if (secondaryValue != null) {
                        // Check for a variant within the style type
                        JsonElement secondaryElement = styleObject.get(secondaryValue.getText());
                        if (secondaryElement != null && !secondaryElement.isJsonNull()) {
                            // Found the variant level style
                            styleObject = secondaryElement.getAsJsonObject();
                        }
                    }
                }
            }

            // Get the style properties
            String fill = styleObject.get(FILL_ELEMENT).getAsString();
            String stroke = styleObject.get(STROKE_ELEMENT).getAsString();
            float fillOpacity = styleObject.get(FILL_OPACITY_ELEMENT).getAsFloat();
            float strokeOpacity = styleObject.get(STROKE_OPACITY_ELEMENT).getAsFloat();
            float strokeWidth = styleObject.get(STROKE_WIDTH_ELEMENT).getAsFloat();

            // Set the stroke width
            style.setStrokeWidth(strokeWidth);

            // Create and set the stroke color
            int strokeColor = Color.parseColor(stroke);
            int strokeColorWithAlpha = ColorUtils.setAlphaComponent(strokeColor, getAlpha(strokeOpacity));
            style.setStrokeColor(strokeColorWithAlpha);

            // Create and set the fill color
            int fillColor = Color.parseColor(fill);
            int fillColorWithAlpha = ColorUtils.setAlphaComponent(fillColor, getAlpha(fillOpacity));
            style.setFillColor(fillColorWithAlpha);
        }

        return style;
    }

    /**
     * Get the alpha value from the opacity
     *
     * @param opacity opacity between 0.0 and 1.0
     * @return alpha
     */
    private static int getAlpha(float opacity) {
        return (int) (opacity * MAX_ALPHA);
    }

}
