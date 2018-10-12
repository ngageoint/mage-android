package mil.nga.giat.mage.people;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
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
    
    private List<User> people = new ArrayList<>();
    private Context context;
    private TeamHelper teamHelper;
    private Collection<Team> eventTeams;
    private OnPersonClickListener personClickListener;

    public PeopleRecyclerAdapter(Context context, List<User> people) {
        this.context = context;
        this.people = people;
        this.teamHelper = TeamHelper.getInstance(context);
        eventTeams = teamHelper.getTeamsByEvent(EventHelper.getInstance(context).getCurrentEvent());
    }

    public void setOnPersonClickListener(OnPersonClickListener personClickListener) {
        this.personClickListener = personClickListener;
    }

    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_people_list_item, viewGroup, false);
        PersonViewHolder viewHolder = new PersonViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final PersonViewHolder viewHolder, int i) {
        final User user = people.get(i);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (personClickListener != null) {
                    personClickListener.onPersonClick(user);
                }
            }
        });

        UserLocal userLocal = user.getUserLocal();
        GlideApp.with(context)
                .asBitmap()
                .load(userLocal.getLocalAvatarPath())
                .fallback(R.drawable.ic_person_gray_48dp)
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
                .into(viewHolder.icon);

        viewHolder.name.setText(user.getDisplayName());

        Collection<Team> userTeams = teamHelper.getTeamsByUser(user);
        userTeams.retainAll(eventTeams);
        Collection<String> teamNames = Collections2.transform(userTeams, new Function<Team, String>() {
            @Override
            public String apply(Team team) {
                return team.getName();
            }
        });

        viewHolder.teams.setText(StringUtils.join(teamNames, ", "));

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (personClickListener != null) {
                    PersonViewHolder holder = (PersonViewHolder) view.getTag();
                    int position = holder.getLayoutPosition();
                    personClickListener.onPersonClick(people.get(position));
                }
            }
        };
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    public class PersonViewHolder extends RecyclerView.ViewHolder {
        protected ImageView avatar;
        protected ImageView icon;
        protected TextView name;
        protected TextView teams;

        public PersonViewHolder(View view) {
            super(view);

            avatar = (ImageView) view.findViewById(R.id.avatarImageView);
            icon = (ImageView) view.findViewById(R.id.iconImageView);
            name = (TextView) view.findViewById(R.id.name);
            teams = (TextView) view.findViewById(R.id.teams);

            view.findViewById(R.id.date).setVisibility(View.GONE);
        }
    }
}
