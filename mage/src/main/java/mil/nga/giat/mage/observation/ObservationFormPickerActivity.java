package mil.nga.giat.mage.observation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.wkb.geom.Point;

public class ObservationFormPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation_form_picker);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.forms);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        recyclerView.setLayoutManager(layoutManager);

        JsonArray formDefinitions = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getNonArchivedForms();
        Adapter adapter = new Adapter(this, formDefinitions);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel(v);
            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void cancel(View v) {
        onBackPressed();
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        ObservationLocation location = getIntent().getParcelableExtra(ObservationEditActivity.LOCATION);
        Point centroid = location.getCentroid();
        LatLng latLng = new LatLng(centroid.getY(), centroid.getX());

        float zoom = getIntent().getFloatExtra(ObservationEditActivity.INITIAL_ZOOM, 0);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        protected class ViewHolder extends RecyclerView.ViewHolder {

            public TextView name;
            public ImageView icon;

            public ViewHolder(View view) {
                super(view);

                name = (TextView) view.findViewById(R.id.name);
                icon = (ImageView) view.findViewById(R.id.icon);
            }

        }

        private Context context;
        private LayoutInflater layoutInflater;

        private JsonArray formDefinitions;

        public Adapter(Context context, JsonArray formDefinitions) {
            this.context = context;
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.formDefinitions = formDefinitions;
        }

        @Override
        public int getItemCount() {
            return formDefinitions.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.view_observation_form, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final JsonObject formDefinition = (JsonObject) formDefinitions.get(position);

            String name = formDefinition.get("name").getAsString();
            holder.name.setText(name);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFormClicked(formDefinition);
                }
            });

            String color = formDefinition.get("color").getAsString();
            // Lets add a tiny bit of transparency to soften things up.
            color = "#D7" + color.substring(1, color.length());

            LayerDrawable pickerIcon = (LayerDrawable) holder.icon.getDrawable();
            Drawable background = pickerIcon.findDrawableByLayerId(R.id.background);
            DrawableCompat.setTint(background, Color.parseColor(color));
        }

        private void onFormClicked(JsonObject formDefinition) {
            Intent intent = new Intent(context, ObservationEditActivity.class);
            intent.putExtra(ObservationEditActivity.OBSERVATION_FORM_ID, formDefinition.get("id").getAsLong());
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();
        }

    }

}
