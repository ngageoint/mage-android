package mil.nga.giat.mage.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.Point;

/**
 *
 * Unit test that tests the {@link mil.nga.giat.mage.form.LayoutBaker}
 *
 * @author wiedemanns
 */
public class LayoutBakerTest extends ActivityInstrumentationTestCase2<ObservationViewActivity> {

	private static final String LOG_NAME = LayoutBakerTest.class.getName();

	ObservationViewActivity activity;

	public LayoutBakerTest() {
		super(ObservationViewActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
	}

	@MediumTest
	public void testDynamicLayoutMapping() {

		String jsonForm = "{\n" +
				"    \"id\": 1,\n" +
				"    \"fields\": [\n" +
				"      {\n" +
				"        \"id\": 3,\n" +
				"        \"title\": \"Type\",\n" +
				"        \"type\": \"dropdown\",\n" +
				"        \"required\": true,\n" +
				"        \"name\": \"type\",\n" +
				"        \"choices\": [\n" +
				"          {\n" +
				"            \"id\": 0,\n" +
				"            \"title\": \"awesome\",\n" +
				"            \"value\": 0\n" +
				"          }\n" +
				"        ]\n" +
				"      }\n" +
				"    ]\n" +
				"  }";

		final JsonObject dynamicFormJson = new JsonParser().parse(jsonForm).getAsJsonObject();
		List<JsonObject> formDefinitions = new ArrayList<JsonObject>() {{ add(dynamicFormJson); }};

		Map<Long, Collection<View>> controls = LayoutBaker.createControls(activity, LayoutBaker.ControlGenerationType.VIEW, formDefinitions);

		Collection<ObservationProperty> properties = new ArrayList<>();
		properties.add(new ObservationProperty("type", "awesome"));

		final ObservationForm form = new ObservationForm();
		form.addProperties(properties);
		Collection<ObservationForm> forms = new ArrayList() {{ add(form); }};

		Geometry geometry = new Point();
		Observation o = new Observation(geometry, forms, null, null, null);

		final Map<String, ObservationProperty> propertiesMapBefore = form.getPropertiesMap();

		LinearLayout ll = new LinearLayout(activity);

		// add dynamic controls to view
		LayoutBaker.populateLayoutWithControls(ll, controls.entrySet().iterator().next().getValue());

		// check two way mapping
		LayoutBaker.populateLayout(ll, LayoutBaker.ControlGenerationType.VIEW, propertiesMapBefore);

		final Map<Long, Map<String, ObservationProperty>> formsMapAfter = LayoutBaker.populateMapFromForms(controls);

		Map<String, ObservationProperty>  propertiesMapAfter = formsMapAfter.entrySet().iterator().next().getValue();
		for(String key : propertiesMapBefore.keySet()) {
			Serializable before = propertiesMapBefore.get(key).getValue();
			Serializable after = propertiesMapAfter.get(key).getValue();

			Log.i(LOG_NAME, "Before property" + before);
			Log.i(LOG_NAME, "After property" + after);

			assertEquals(before, after);
		}
	}

}