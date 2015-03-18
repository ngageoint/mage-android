package mil.nga.giat.mage.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

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

		String form = "{\n" +
				"    \"variantField\": null,\n" +
				"    \"fields\": [\n" +
				"      {\n" +
				"        \"id\": 1,\n" +
				"        \"title\": \"Date\",\n" +
				"        \"type\": \"date\",\n" +
				"        \"required\": true,\n" +
				"        \"name\": \"timestamp\",\n" +
				"        \"choices\": []\n" +
				"      },\n" +
				"      {\n" +
				"        \"id\": 2,\n" +
				"        \"title\": \"Location\",\n" +
				"        \"type\": \"geometry\",\n" +
				"        \"required\": true,\n" +
				"        \"name\": \"geometry\",\n" +
				"        \"choices\": []\n" +
				"      },\n" +
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

		JsonObject dynamicFormJson = new JsonParser().parse(form).getAsJsonObject();;

		List<View> controls = LayoutBaker.createControlsFromJson(activity, LayoutBaker.ControlGenerationType.VIEW, dynamicFormJson);

		Collection<ObservationProperty> properties = new ArrayList<ObservationProperty>();
		properties.add(new ObservationProperty("type", "awesome"));
		properties.add(new ObservationProperty("timestamp", DateFormatFactory.ISO8601().format(new Date())));

		Observation o = new Observation(null, properties, null, null, null);

		final Map<String, ObservationProperty> propertiesMapBefore = o.getPropertiesMap();

		LinearLayout ll = new LinearLayout(activity);

		// add dynamic controls to view
		LayoutBaker.populateLayoutWithControls(ll, controls);

		// check two way mapping
		LayoutBaker.populateLayoutFromMap(ll, propertiesMapBefore);

		final Map<String, ObservationProperty> propertiesMapAfter = LayoutBaker.populateMapFromLayout(ll);

		for(String key : propertiesMapBefore.keySet()) {
			Serializable before = propertiesMapBefore.get(key).getValue();
			Serializable after = propertiesMapAfter.get(key).getValue();

			assertEquals(before, after);
		}
	}

}