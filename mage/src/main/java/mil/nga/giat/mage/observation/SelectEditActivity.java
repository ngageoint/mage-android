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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class SelectEditActivity extends Activity {

    public static String SELECT_CHOICES = "MULTISELECT_CHOICES";
    public static String SELECT_SELECTED = "MULTISELECT_SELECTED";
    public static String IS_MULTISELECT = "IS_MULTISELECT";
    public static String FIELD_ID = "FIELD_ID";
    public static String MULTISELECT_JSON_CHOICE_KEY = "choices";
    public static String MULTISELECT_JSON_CHOICE_TITLE = "title";

    private static String DEFAULT_TEXT = "";

    private ArrayList<String> userSelectedChoices;
    private ListView choicesListView;
    private TextView selectedChoicesTextView;
    private EditText filterChoices;
    private LinearLayout filterButtonLayout;
    private LinearLayout filterSearchLayout;
    private Button filterSearchButton;
    private ArrayAdapter<String> adapter;
    private Integer fieldId;
    private Boolean isMultiSelect = Boolean.FALSE;
    private ArrayList<String> choicesList;
    private ArrayList<String> filteredChoicesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_edit);
        filteredChoicesList = new ArrayList<String>();

        Intent intent = getIntent();
        JsonParser jsonParser = new JsonParser();

        isMultiSelect = intent.getBooleanExtra(IS_MULTISELECT, false);
        fieldId = intent.getIntExtra(FIELD_ID, 0);
        String choices = intent.getStringExtra(SELECT_CHOICES);
        JsonArray choicesArray = jsonParser.parse(choices).getAsJsonArray();
        choicesList = parseChoicesToGetTitles(choicesArray);
        filteredChoicesList.addAll(choicesList);

        choicesListView = (ListView) findViewById(R.id.select_choices);
        if (isMultiSelect) {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
            choicesListView.setAdapter(adapter);
            choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, filteredChoicesList);
            choicesListView.setAdapter(adapter);
            choicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        choicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);

                if (isMultiSelect) {
                    if (choicesListView.isItemChecked(position)) {
                        if (!userSelectedChoices.contains(selectedItem)) {
                            userSelectedChoices.add(selectedItem);
                        }
                    } else {
                        if (userSelectedChoices.contains(selectedItem)) {
                            userSelectedChoices.remove(selectedItem);
                        }
                    }
                } else {
                    userSelectedChoices.clear();
                    userSelectedChoices.add(selectedItem);
                }
                if (userSelectedChoices.isEmpty()) {
                    selectedChoicesTextView.setText(DEFAULT_TEXT);
                } else {
                    selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
                }
            }
        });

        selectedChoicesTextView = (TextView) findViewById(R.id.selected_choices);
        userSelectedChoices = intent.getStringArrayListExtra(SELECT_SELECTED);

        if (userSelectedChoices == null) {
            userSelectedChoices = new ArrayList<String>();
        }
        if (userSelectedChoices.isEmpty()) {
            selectedChoicesTextView.setText(DEFAULT_TEXT);
        } else {
            checkSelected();
            selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
        }

        filterButtonLayout = (LinearLayout) findViewById(R.id.filter_button_layout);
        filterSearchLayout = (LinearLayout) findViewById(R.id.filter_search_layout);
        filterSearchButton = (Button) findViewById(R.id.filter_button);
        filterSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterButtonLayout.setVisibility(View.GONE);
                filterSearchLayout.setVisibility(View.VISIBLE);
                filterChoices.requestFocus();
                showKeyboard();
            }
        });

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
                        String filterString = s.toString();
                        String currentChoice = choicesList.get(position);
                        if(StringUtils.containsIgnoreCase(currentChoice, filterString))
                        {
                            filteredChoicesList.add(currentChoice);
                        }
                    }
                }

                if (isMultiSelect) {
                    adapter = new ArrayAdapter<String>(SelectEditActivity.this, android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
                    choicesListView.setAdapter(adapter);
                    choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                } else {
                    adapter = new ArrayAdapter<String>(SelectEditActivity.this, android.R.layout.simple_list_item_single_choice, filteredChoicesList);
                    choicesListView.setAdapter(adapter);
                    choicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                }


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
        data.putStringArrayListExtra(SELECT_SELECTED, userSelectedChoices);
        data.putExtra(FIELD_ID, fieldId);
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
        filterButtonLayout.setVisibility(View.VISIBLE);
        filterSearchLayout.setVisibility(View.INVISIBLE);
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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
