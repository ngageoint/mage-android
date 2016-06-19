package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText filterChoices;
    private Button filterClear;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> choicesList;
    private ArrayList<String> filteredChoicesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_edit);
        userSelectedChoices = new ArrayList<String>();
        filteredChoicesList = new ArrayList<String>();

        Intent intent = getIntent();
        JsonParser jsonParser = new JsonParser();

        String choices = intent.getStringExtra(MULTISELECT_CHOICES);
        JsonArray choicesArray = jsonParser.parse(choices).getAsJsonArray();
        choicesList = parseChoicesToGetTitles(choicesArray);
        filteredChoicesList.addAll(choicesList);

        choicesListView = (ListView) findViewById(R.id.select_choices);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
        choicesListView.setAdapter(adapter);
        choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        choicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                if (choicesListView.isItemChecked(position)) {
                    if (!userSelectedChoices.contains(selectedItem)) {
                        userSelectedChoices.add(selectedItem);
                    }
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
            checkSelected();
            selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
        }

        filterClear = (Button) findViewById(R.id.filter_clear);
        filterChoices = (EditText) findViewById(R.id.filter_choices);
        filterChoices.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                filteredChoicesList.clear();
                for (int position = 0; position < choicesList.size(); position++)
                {
                    if (s.length() <= choicesList.get(position).length())
                    {
                        //TODO: Update if contains substring search is wanted
                        if(s.toString().equalsIgnoreCase((String) choicesList.get(position).subSequence(0, s.length())))
                        {
                            filteredChoicesList.add(choicesList.get(position));
                        }
                    }
                }

                adapter = new ArrayAdapter<String>(SelectEditActivity.this,
                        android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
                choicesListView.setAdapter(adapter);
                checkSelected();
            }
        });

        View main = findViewById(R.id.select_parent);
        main.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });

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

    public void clearFilter(View v) {
        filterChoices.getText().clear();
        filteredChoicesList.clear();
        filteredChoicesList.addAll(choicesList);
        checkSelected();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
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

    private void checkSelected() {
        for (int count = 0; count < userSelectedChoices.size(); count++) {
            int index = filteredChoicesList.indexOf(userSelectedChoices.get(count));
            if (index != -1) {
                choicesListView.setItemChecked(index, true);
            }
        }
    }

}
