package mil.nga.giat.mage.observation;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

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

        JsonObject formJson = observation.getEvent().getForm();

        // Check for a style
        JsonElement styleField = formJson.get(STYLE_ELEMENT);
        if (styleField != null && !styleField.isJsonNull()) {

            // Found the top level style
            JsonObject styleObject = styleField.getAsJsonObject();

            Map<String, ObservationProperty> properties = observation.getPropertiesMap();
            ObservationProperty typeProperty = properties.get(OBSERVATION_TYPE_PROPERTY);
            String type = typeProperty.getValue().toString();

            // Check for a type within the style
            JsonElement typeField = styleObject.get(type);
            if (typeField != null && !typeField.isJsonNull()) {

                // Found the type level style
                styleObject = typeField.getAsJsonObject();

                // Check for a variant
                JsonElement variantField = formJson.get(VARIANT_FIELD_ELEMENT);
                if (variantField != null && !variantField.isJsonNull()) {

                    ObservationProperty variantProperty = properties.get(variantField.getAsString());
                    String variant = variantProperty.getValue().toString();

                    // Check for a variant within the style type
                    JsonElement typeVariantField = styleObject.get(variant);
                    if (typeVariantField != null && !typeVariantField.isJsonNull()) {

                        // Found the variant level style
                        styleObject = typeVariantField.getAsJsonObject();
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
