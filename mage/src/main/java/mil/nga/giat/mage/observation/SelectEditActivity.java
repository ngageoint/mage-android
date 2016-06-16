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

import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class SelectEditActivity extends Activity {

    public static String MULTISELECT_CHOICES = "MULTISELECT_CHOICES";
    public static String MULTISELECT_SELECTED = "MULTISELECT_SELECTED";
    public static String MULTISELECT_JSON_CHOICE_KEY = "choices";
    public static String MULTISELECT_JSON_CHOICE_TITLE = "title";

    private static String DEFAULT_TEXT = "Please select a value below.";
    private ArrayList<String> userSelectedChoices;
    private ListView choicesListView;
    private TextView selectedChoicesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_edit);
        userSelectedChoices = new ArrayList<String>();

        Intent intent = getIntent();
        JsonParser jsonParser = new JsonParser();

        String choices = intent.getStringExtra(MULTISELECT_CHOICES);
        JsonArray choicesArray = jsonParser.parse(choices).getAsJsonArray();
        ArrayList<String> choicesList = parseChoicesToGetTitles(choicesArray);


        choicesListView = (ListView) findViewById(R.id.select_choices);
        //TODO: Verify a custom adapter is not needed
        choicesListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, choicesList));
        choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        choicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                if (choicesListView.isItemChecked(position)) {
                    userSelectedChoices.add(selectedItem);
                } else {
                    if (userSelectedChoices.contains(selectedItem)) {
                        userSelectedChoices.remove(selectedItem);
                    }
                }
                if (userSelectedChoices.isEmpty()) {
                    selectedChoicesTextView.setText(DEFAULT_TEXT);
                } else {
                    selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
                }
            }
        });

        selectedChoicesTextView = (TextView) findViewById(R.id.selected_choices);

        userSelectedChoices = intent.getStringArrayListExtra(MULTISELECT_SELECTED);

        if (userSelectedChoices.isEmpty()) {
            selectedChoicesTextView.setText(DEFAULT_TEXT);
        } else {
            for (int count = 0; count < userSelectedChoices.size(); count++) {
                int index = choicesList.indexOf(userSelectedChoices.get(count));
                if (index != -1) {
                    choicesListView.setItemChecked(index, true);
                }
            }
            selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
        }

    }

    public void cancel(View v) {
        onBackPressed();
    }

    public void updateSelected(View v) {
        Intent data = new Intent();
        data.setData(getIntent().getData());
        data.putStringArrayListExtra(MULTISELECT_SELECTED, userSelectedChoices);
        setResult(RESULT_OK, data);
        finish();
    }

    public void clearSelected(View v) {
        choicesListView.clearChoices();
        choicesListView.invalidateViews();
        selectedChoicesTextView.setText(DEFAULT_TEXT);
        userSelectedChoices.clear();
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
                parsedList.add(jsonObject.get(MULTISELECT_JSON_CHOICE_TITLE).getAsString());
            }
        }
        return parsedList;
    }

    private String getSelectedChoicesString(ArrayList<String> selectedChoices) {
        StringBuilder displayValue = new StringBuilder();
        for (int count = 0; count < selectedChoices.size(); count++) {
            if (count < selectedChoices.size() - 1) {
                displayValue.append(selectedChoices.get(count) + ", ");
            } else {
                displayValue.append(selectedChoices.get(count));
            }
        }
        return displayValue.toString();
    }

}
