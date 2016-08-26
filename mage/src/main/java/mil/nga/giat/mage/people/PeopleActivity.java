package mil.nga.giat.mage.people;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.profile.ProfileFragment;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.widget.DividerItemDecoration;

public class PeopleActivity extends Activity implements PeopleRecyclerAdapter.OnPersonClickListener {

    public static final String USER_REMOTE_IDS = "USER_REMOTE_IDS";

    private static final String LOG_NAME = PeopleActivity.class.getName();

    private List<User> people = new ArrayList<>();
    private RecyclerView recyclerView;
    private PeopleRecyclerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_people);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.people_feed_divider));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Collection<String> userIds = getIntent().getStringArrayListExtra(USER_REMOTE_IDS);
        try {
            people = UserHelper.getInstance(getApplicationContext()).read(userIds);
        } catch (UserException e) {
            Log.e(LOG_NAME, "Error read users for remoteIds: " + userIds.toString(), e);
        }

        adapter = new PeopleRecyclerAdapter(this, people);
        recyclerView.setAdapter(adapter);
        adapter.setOnPersonClickListener(this);
    }

    @Override
    public void onPersonClick(User person) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileFragment.USER_ID, person.getRemoteId());
        startActivity(intent);
    }
}
