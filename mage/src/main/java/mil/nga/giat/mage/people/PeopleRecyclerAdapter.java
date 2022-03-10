package mil.nga.giat.mage.people;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;

/**
 * Created by wnewman on 8/26/16.
 */
public class PeopleRecyclerAdapter extends RecyclerView.Adapter<PeopleRecyclerAdapter.PersonViewHolder> {
    public interface OnPersonClickListener {
        void onPersonClick(User person);
    }

    private Event event;
    private List<User> people;
    private Context context;
    private TeamHelper teamHelper;
    private Collection<Team> eventTeams;
    private OnPersonClickListener personClickListener;

    public PeopleRecyclerAdapter(Context context, List<User> people) {
        this.context = context;
        this.people = people;
        this.teamHelper = TeamHelper.getInstance(context);
        this.event = EventHelper.getInstance(context).getCurrentEvent();
        eventTeams = teamHelper.getTeamsByEvent(event);
    }

    public void setOnPersonClickListener(OnPersonClickListener personClickListener) {
        this.personClickListener = personClickListener;
    }

    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.favorite_user_list_item, viewGroup, false);
        PersonViewHolder viewHolder = new PersonViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final PersonViewHolder viewHolder, int i) {
        final User user = people.get(i);

        viewHolder.card.setOnClickListener(v -> {
            if (personClickListener != null) {
                personClickListener.onPersonClick(user);
            }
        });

        UserLocal userLocal = user.getUserLocal();
        GlideApp.with(context)
                .asBitmap()
                .load(Avatar.Companion.forUser(user))
                .fallback(R.drawable.ic_person_gray_24dp)
                .centerCrop()
                .into(new BitmapImageViewTarget(viewHolder.avatar) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        viewHolder.avatar.setImageDrawable(circularBitmapDrawable);
                    }
                });

        GlideApp.with(context)
                .load(userLocal.getLocalIconPath())
                .centerCrop()
                .into(viewHolder.avatar);

        viewHolder.name.setText(user.getDisplayName());

        Collection<Team> userTeams = teamHelper.getTeamsByUser(user);
        userTeams.retainAll(eventTeams);
        Collection<String> teamNames = Collections2.transform(userTeams, team -> team.getName());

        viewHolder.teams.setText(StringUtils.join(teamNames, ", "));
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    public class PersonViewHolder extends RecyclerView.ViewHolder {
        protected View card;
        protected TextView name;
        protected TextView teams;
        protected ImageView avatar;

        public PersonViewHolder(View view) {
            super(view);

            card = view.findViewById(R.id.card);
            avatar = (ImageView) view.findViewById(R.id.avatarImageView);
            name = (TextView) view.findViewById(R.id.name);
            teams = (TextView) view.findViewById(R.id.teams);
        }
    }
}
