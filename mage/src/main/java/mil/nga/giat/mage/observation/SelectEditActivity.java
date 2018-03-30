package mil.nga.giat.mage.observation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import mil.nga.giat.mage.R;

public class SelectEditActivity extends AppCompatActivity {

    public static String SELECT_CHOICES = "MULTISELECT_CHOICES";
    public static String SELECT_SELECTED = "MULTISELECT_SELECTED";
    public static String IS_MULTISELECT = "IS_MULTISELECT";
    public static String FIELD_ID = "FIELD_ID";
    public static String FIELD_TITLE = "FIELD_TITLE";
    public static String MULTISELECT_JSON_CHOICE_KEY = "choices";
    public static String MULTISELECT_JSON_CHOICE_TITLE = "title";

    private static String DEFAULT_TEXT = "";

    private ArrayList<String> userSelectedChoices;
    private ListView choicesListView;
    private TextView selectedChoicesTextView;
    private ArrayAdapter<String> adapter;
    private Integer fieldId;
    private Boolean isMultiSelect = Boolean.FALSE;
    private ArrayList<String> choicesList;
    private ArrayList<String> filteredChoicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_edit);

        Intent intent = getIntent();

        Toolbar toolbar = (Toolbar) findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.edit_select_menu);

        String title = intent.getStringExtra(FIELD_TITLE);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(title);

        isMultiSelect = intent.getBooleanExtra(IS_MULTISELECT, false);
        fieldId = intent.getIntExtra(FIELD_ID, 0);
        String choices = intent.getStringExtra(SELECT_CHOICES);
        JsonArray choicesArray = new JsonParser().parse(choices).getAsJsonArray();
        choicesList = parseChoicesToGetTitles(choicesArray);
        filteredChoicesList.addAll(choicesList);

        choicesListView = (ListView) findViewById(R.id.select_choices);
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.AppTheme);
        if (isMultiSelect) {
            adapter = new ArrayAdapter<>(wrapper, android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
            choicesListView.setAdapter(adapter);
            choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else {
            adapter = new ArrayAdapter<>(wrapper, android.R.layout.simple_list_item_single_choice, filteredChoicesList);
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
            userSelectedChoices = new ArrayList<>();
        }
        if (userSelectedChoices.isEmpty()) {
            selectedChoicesTextView.setText(DEFAULT_TEXT);
        } else {
            checkSelected();
            selectedChoicesTextView.setText(getSelectedChoicesString(userSelectedChoices));
        }

        SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearchTextChanged(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_select_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            case R.id.done:
                done();
                return true;
            case R.id.clear_selection:
                clearSelected();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSearchTextChanged(String text) {
        filteredChoicesList.clear();

        for (int position = 0; position < choicesList.size(); position++) {
            if (text.length() <= choicesList.get(position).length()) {
                String currentChoice = choicesList.get(position);
                if(StringUtils.containsIgnoreCase(currentChoice, text)) {
                    filteredChoicesList.add(currentChoice);
                }
            }
        }

        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.AppTheme);
        if (isMultiSelect) {
            adapter = new ArrayAdapter<>(wrapper, android.R.layout.simple_list_item_multiple_choice, filteredChoicesList);
            choicesListView.setAdapter(adapter);
            choicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else {
            adapter = new ArrayAdapter<>(wrapper, android.R.layout.simple_list_item_single_choice, filteredChoicesList);
            choicesListView.setAdapter(adapter);
            choicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        checkSelected();
    }

    public void cancel(View v) {
        onBackPressed();
    }

    public void done() {
        Intent data = new Intent();
        data.setData(getIntent().getData());
        data.putStringArrayListExtra(SELECT_SELECTED, userSelectedChoices);
        data.putExtra(FIELD_ID, fieldId);
        setResult(RESULT_OK, data);
        finish();
    }

    private void clearSelected() {
        choicesListView.clearChoices();
        choicesListView.invalidateViews();
        selectedChoicesTextView.setText(DEFAULT_TEXT);
        userSelectedChoices.clear();
    }

    private ArrayList<String> parseChoicesToGetTitles(JsonArray jsonArray) {
        ArrayList<String> parsedList = new ArrayList<>();
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
        return StringUtils.join(selectedChoices, " | ");
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
