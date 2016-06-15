package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class SelectEditActivity extends Activity {

    public static String MULTISELECT_CHOICES = "MULTISELECT_CHOICES";
    public static String MULTISELECT_SELECTED = "MULTISELECT_SELECTED";
    public static String MULTISELECT_JSON_CHOICE_KEY = "choices";
    public static String MULTISELECT_JSON_CHOICE_TITLE = "title";

    private JsonArray userSelectedChoices;
    private ListView choicesListView;
    private TextView selectedChoicesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_edit);
        userSelectedChoices = new JsonArray();

        Intent intent = getIntent();
        JsonParser jsonParser = new JsonParser();

        String choices = intent.getStringExtra(MULTISELECT_CHOICES);
        JsonArray choicesArray = jsonParser.parse(choices).getAsJsonArray();
        final ArrayList<String> choicesList = parseChoicesToGetTitles(choicesArray);

        String selected = intent.getStringExtra(MULTISELECT_SELECTED);
        JsonArray selectedArray = jsonParser.parse(selected).getAsJsonArray();
        ArrayList<String> selectedList = parseChoicesToGetTitles(selectedArray);

        choicesListView = (ListView) findViewById(R.id.select_choices);
        choicesListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, choicesList));
        choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        choicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                JsonPrimitive item = new JsonPrimitive(selectedItem);
                if (choicesListView.isItemChecked(position)) {
                    userSelectedChoices.add(item);
                } else {
                    if (userSelectedChoices.contains(item)) {
                        userSelectedChoices.remove(item);
                    }
                }
                selectedChoicesTextView.setText(userSelectedChoices.toString());
            }
        });

        selectedChoicesTextView = (TextView) findViewById(R.id.selected_choices);

        if (selectedList.isEmpty()) {
            selectedChoicesTextView.setText("Please select a value below.");
        } else {
            selectedChoicesTextView.setText(selectedList.toString());
            //TODO: Clean up
            for (int count = 0; count < selectedList.size(); count++) {
                int index = choicesList.indexOf(selectedList.get(count));
                if (index != -1) {
                    choicesListView.setItemChecked(index, true);
                    userSelectedChoices.add(new JsonPrimitive(selectedList.get(count)));
                }
            }
        }

    }

    public void cancel(View v) {
        onBackPressed();
    }

    public void updateSelected(View v) {
        Intent data = new Intent();
        data.setData(getIntent().getData());

        if (userSelectedChoices.size() == 0) {
            userSelectedChoices.add(new JsonPrimitive(""));
        }
        data.putExtra(MULTISELECT_SELECTED, userSelectedChoices.toString());
        setResult(RESULT_OK, data);
        finish();
    }

    private ArrayList<String> parseChoicesToGetTitles(JsonArray jsonArray) {
        ArrayList<String> parsedList = new ArrayList<String>();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement;
        JsonObject jsonObject;
        if (jsonArray != null) {
            for (int count = 0; count < jsonArray.size(); count++) {
                jsonElement = jsonParser.parse(jsonArray.get(count).toString());
                jsonObject = jsonElement.getAsJsonObject();
                parsedList.add(jsonObject.get(MULTISELECT_JSON_CHOICE_TITLE).toString());
            }
        }

        return parsedList;
    }

}
