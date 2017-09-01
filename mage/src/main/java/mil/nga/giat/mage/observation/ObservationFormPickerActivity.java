package mil.nga.giat.mage.observation;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

public class ObservationFormPickerActivity extends AppCompatActivity {
    private List<String> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation_form_picker);

        JsonArray formDefinitions = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getForms();

        items.add("Form 1");
        items.add("Form 2");
        items.add("Form 3");
        items.add("Form 4");
        items.add("Form 5");

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.lst_items);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        recyclerView.setLayoutManager(layoutManager);


//        GridLayoutManager manager = new GridLayoutManager(this, 6);
//        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                if (items.size() % 3 != 0) {
////                    if (items.size() % 3 == 2) {
////
////                    } else if (items.size() % 3 == 1) {
////
////                    } else {
////                        return 1;
////                    }
//
//                    // if three items in last spot return 1
//                    // if 2 items return 2
//                    // if 3 items return 3
//
//                    int span;
//                    span = items.size() % 3;
//                    if (items.size() < 3) {
//                        return 6;
//                    } else if (span == 0 || (position <= ((items.size() - 1) - span))) {
//                        return 2;
//                    } else if (span == 1) {
//                        return 6;
//                    } else {
//                        return 3;
//                    }
//
////                    return (position == items.size() - 1) ? 2 : 1;
//                } else {
//                    return 1;
//                }
//            }
//        });
//
//        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.lst_items);
//        recyclerView.setLayoutManager(manager);
//
        Adapter adapter = new Adapter(this, items);
        recyclerView.setAdapter(adapter);


//        gridview.setOnItemClickListener(new OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View v,
//                                    int position, long id) {
//                Toast.makeText(HelloGridView.this, "" + position,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });

//        JsonArray formDefinitions = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getForms();
//
//        int formCount = 7;
//        int evenCells = formCount / 3;
//        int oddCells = formCount % 3;
//
//        TableLayout tableLayout = (TableLayout) findViewById(R.id.table_layout);
//
//        TableRow tableRow = new TableRow(this);
//        tableLayout.addView(tableRow);
//
//        for (int i = 0; i < formCount; i++) {
//            if (i % 3 == 0) {
//                tableRow = new TableRow(this);
//                tableLayout.addView(tableRow);
//            }
//
//            View formView = View.inflate(this, R.layout.view_observation_form, null);
//            tableRow.addView(formView);
//        }
    }



    public void cancel(View v) {
        onBackPressed();
    }

    public static class FormAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<String> items = new ArrayList();

        public FormAdapter(Context context) {
            this.context = context;

            items.add("Form 1");
            items.add("Form 2");
            items.add("Form 3");
//            items.add("Form 4");
//            items.add("Form 5");
//            items.add("Form 6");
//            items.add("Form 7");
        }

        public int getCount() {
            return items.size();
        }

        public Object getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return position;
        }


        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.view_observation_form, parent, false);
            }

//            ImageView imageView;
//            if (convertView == null) {
//                // if it's not recycled, initialize some attributes
//                imageView = new ImageView(mContext);
//                imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
//                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//                imageView.setPadding(8, 8, 8, 8);
//            } else {
//                imageView = (ImageView) convertView;
//            }
//
//            imageView.setImageResource(mThumbIds[position]);
//            return imageView;
            return convertView;
        }
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.GridViewHolder> {

        protected class GridViewHolder extends RecyclerView.ViewHolder {

            View card;
            TextView txt_label;
            ImageView img_banner;

            public GridViewHolder(View itemView) {
                super(itemView);
//                card = itemView.findViewById(R.id.view_observation_form);
//                txt_label = (TextView) itemView.findViewById(R.id.tvCard);
//                img_banner = (ImageView) itemView.findViewById(R.id.ivCard);
//                card.getLayoutParams().width = width;
            }

        }

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        private List<String> mItems = new ArrayList<>();

        public Adapter(Context context, List<String> items) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mItems = items;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mLayoutInflater.inflate(R.layout.view_observation_form, parent, false);
//            // let's start by considering number of columns
//            int width = parent.getMeasuredWidth() / 3;
//            // then, let's remove recyclerview padding
//            width -= 32;//mContext.getResources().getDimensionPixelSize(R.dimen.recycler_view_padding);
            return new GridViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(GridViewHolder viewHolder, int position) {
            String item = mItems.get(position);
//            viewHolder.txt_label.setText(item);
        }

    }

}
