package mil.nga.giat.mage.people;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.data.datasource.team.TeamLocalDataSource;
import mil.nga.giat.mage.database.model.event.Event;
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource;
import mil.nga.giat.mage.database.model.team.Team;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.database.model.user.User;
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource;
import mil.nga.giat.mage.sdk.exceptions.UserException;

@AndroidEntryPoint
public class PeopleActivity extends AppCompatActivity implements PeopleRecyclerAdapter.OnPersonClickListener {

    public static final String USER_REMOTE_IDS = "USER_REMOTE_IDS";

    private static final String LOG_NAME = PeopleActivity.class.getName();

    private List<User> people = new ArrayList<>();
    private RecyclerView recyclerView;
    private PeopleRecyclerAdapter adapter;

    @Inject protected UserLocalDataSource userLocalDataSource;
    @Inject protected TeamLocalDataSource teamLocalDataSource;
    @Inject protected EventLocalDataSource eventLocalDataSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_people);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Collection<String> userIds = getIntent().getStringArrayListExtra(USER_REMOTE_IDS);
        try {
            people = userLocalDataSource.read(userIds);
        } catch (UserException e) {
            Log.e(LOG_NAME, "Error read users for remoteIds: " + userIds, e);
        }

        Event event = eventLocalDataSource.getCurrentEvent();
        List<Team> eventTeams = teamLocalDataSource.getTeamsByEvent(event);

        adapter = new PeopleRecyclerAdapter(
            getApplicationContext(),
            people,
            eventTeams,
            teamLocalDataSource
        );
        recyclerView.setAdapter(adapter);
        adapter.setOnPersonClickListener(this);
    }

    @Override
    public void onPersonClick(User person) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA, person.getId());
        startActivity(intent);
    }
}
