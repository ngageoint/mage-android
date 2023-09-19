package mil.nga.giat.mage.people;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.data.datasource.team.TeamLocalDataSource;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.database.model.team.Team;
import mil.nga.giat.mage.database.model.user.User;
import mil.nga.giat.mage.database.model.user.UserLocal;

public class PeopleRecyclerAdapter extends RecyclerView.Adapter<PeopleRecyclerAdapter.PersonViewHolder> {
    public interface OnPersonClickListener {
        void onPersonClick(User person);
    }

    private final List<User> people;
    private final Context context;
    private final Collection<Team> eventTeams;
    private final TeamLocalDataSource teamLocalDataSource;
    private OnPersonClickListener personClickListener;

    public PeopleRecyclerAdapter(
        Context context,
        List<User> people,
        Collection<Team> eventTeams,
        TeamLocalDataSource teamLocalDataSource
    ) {
        this.context = context;
        this.people = people;
        this.eventTeams = eventTeams;
        this.teamLocalDataSource = teamLocalDataSource;
    }

    public void setOnPersonClickListener(OnPersonClickListener personClickListener) {
        this.personClickListener = personClickListener;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.favorite_user_list_item, viewGroup, false);
        return new PersonViewHolder(view);
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

        Collection<Team> userTeams = teamLocalDataSource.getTeamsByUser(user);
        userTeams.retainAll(eventTeams);
        Collection<String> teamNames = Collections2.transform(userTeams, Team::getName);

        viewHolder.teams.setText(StringUtils.join(teamNames, ", "));
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
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
