package mil.nga.giat.mage.observation;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.FormField;
import mil.nga.giat.mage.form.FormViewModel;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import mil.nga.giat.mage.utils.DateFormatFactory;

/**
 * Created by wnewman on 2/9/17.
 */

public class DateTimePickerDialog extends DialogFragment {

    public interface OnDateTimeChangedListener {
        void onDateTimeChanged(Date date);
    }

    private static final String CALENDAR_INSTANCE = "CALENDAR";
    private static final String DATE_TIME_EXTRA ="DATE_TIME_EXTRA";
    private static final String FORM_FIELD_KEY_EXTRA ="FORM_FIELD_KEY_EXTRA";

    private Calendar calendar = Calendar.getInstance();

    private FormViewModel model;
    private String fieldKey;

    private DateFormat dateFormat;
    private DateFormat timeFormat;

    DatePickerFragment datePickerFragment;
    TimePickerFragment timePickerFragment;

    private OnDateTimeChangedListener onDateTimeChangedListener;

    public static DateTimePickerDialog newInstance(Date date) {
        DateTimePickerDialog fragment = new DateTimePickerDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(DATE_TIME_EXTRA, date);

        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        fragment.setArguments(bundle);

        return fragment;
    }

    public static DateTimePickerDialog newInstance(long fieldId) {
        DateTimePickerDialog fragment = new DateTimePickerDialog();
        Bundle bundle = new Bundle();
        bundle.putLong(FORM_FIELD_KEY_EXTRA, fieldId);

        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new RuntimeException("Invalid Activity");
        }

        model = ViewModelProviders.of(activity).get(FormViewModel.class);

        dateFormat = DateFormatFactory.format("MMM dd, yyyy", java.util.Locale.getDefault(), getContext());
        timeFormat = DateFormatFactory.format("HH:mm zz", java.util.Locale.getDefault(), getContext());
        calendar.setTimeZone(dateFormat.getTimeZone());
        Date date = (Date) getArguments().getSerializable(DATE_TIME_EXTRA);
        if (date == null) {
            date = new Date();
        }

        if (getArguments().containsKey(FORM_FIELD_KEY_EXTRA)) {
            fieldKey = getArguments().getString(FORM_FIELD_KEY_EXTRA, null);
            FormField<?> field = model.getField(fieldKey);

            Object value = field.getValue();

            if (value instanceof Date) {
                date = (Date) value;
            } else if (value instanceof String) {
                try {
                    date = ISO8601DateFormatFactory.ISO8601().parse((String) value);
                } catch (ParseException e) {
                    return;
                }
            }
        }

        calendar.setTime(date);



    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(CALENDAR_INSTANCE, calendar);

        FragmentManager manager = getChildFragmentManager();
        manager.putFragment(outState, DatePickerFragment.class.getName(), datePickerFragment);
        manager.putFragment(outState, TimePickerFragment.class.getName(), timePickerFragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater localInflater = inflater.cloneInContext(getContext());
        View view = localInflater.inflate(R.layout.date_time_dialog, container, false);

        if (savedInstanceState != null) {
            calendar = (Calendar) savedInstanceState.getSerializable(CALENDAR_INSTANCE);

            FragmentManager manager = getChildFragmentManager();
            datePickerFragment = (DatePickerFragment) manager.getFragment(savedInstanceState, DatePickerFragment.class.getName());
            timePickerFragment = (TimePickerFragment) manager.getFragment(savedInstanceState, TimePickerFragment.class.getName());
        }

        final TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);

        final TabLayout.Tab dateTab = tabLayout.newTab().setText(dateFormat.format(calendar.getTime()));
        tabLayout.addTab(dateTab);

        final TabLayout.Tab timeTab = tabLayout.newTab().setText(timeFormat.format(calendar.getTime()));
        tabLayout.addTab(timeTab);

        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.pager);

        if (datePickerFragment == null) {
            datePickerFragment = DatePickerFragment.newInstance(calendar.getTime());
        }
        datePickerFragment.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                dateTab.setText(dateFormat.format(calendar.getTime()));
            }
        });

        if (timePickerFragment == null) {
            timePickerFragment = TimePickerFragment.newInstance(calendar.getTime());
        }
        timePickerFragment.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                timeTab.setText(timeFormat.format(calendar.getTime()));
            }
        });


        PagerAdapter adapter = new DateTimePagerAdapter(getChildFragmentManager(), Arrays.asList(new Fragment[] {datePickerFragment, timePickerFragment}));
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO just update form view model here and dismiss the dialog
                if (fieldKey != null) {
                    FormField<?> field = model.getField(fieldKey);
//                    field.setValue(calendar.getTime());
                }

                if (onDateTimeChangedListener != null) {
                    onDateTimeChangedListener.onDateTimeChanged(calendar.getTime());
                }

                dismiss();
            }
        });

        return view;
    }

    public void setOnDateTimeChangedListener(OnDateTimeChangedListener onDateTimeChangedListener) {
        this.onDateTimeChangedListener = onDateTimeChangedListener;
    }

    public static class DatePickerFragment extends Fragment {

        private DatePicker.OnDateChangedListener onDateChangedListener;
        Calendar calendar = Calendar.getInstance();

        public static DatePickerFragment newInstance(Date date) {
            DatePickerFragment fragment = new DatePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(DATE_TIME_EXTRA, date);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Date date = (Date) getArguments().getSerializable(DATE_TIME_EXTRA);
            if (date == null) {
                date = new Date();
            }

            calendar.setTimeZone(DateFormatFactory.getTimeZone(getContext()));
            calendar.setTime(date);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_date_picker, container, false);

            DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    if (onDateChangedListener !=  null) {
                        onDateChangedListener.onDateChanged(view, year, monthOfYear, dayOfMonth);
                    }
                }
            });

            return view;
        }

        public void setOnDateChangedListener(DatePicker.OnDateChangedListener onDateChangedListener) {
            this.onDateChangedListener = onDateChangedListener;
        }
    }

    public static class TimePickerFragment extends Fragment {

        private TimePicker.OnTimeChangedListener onTimeChangedListener;
        private Calendar calendar = Calendar.getInstance();

        public static TimePickerFragment newInstance(Date date) {
            TimePickerFragment fragment = new TimePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(DATE_TIME_EXTRA, date);
            fragment.setArguments(bundle);

            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Date date = (Date) getArguments().getSerializable(DATE_TIME_EXTRA);
            if (date == null) {
                date = new Date();
            }
            calendar.setTimeZone(DateFormatFactory.getTimeZone(getContext()));

            calendar.setTime(date);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_time_picker, container, false);

            TimePicker timePicker = (TimePicker) view.findViewById(R.id.time_picker);
            timePicker.setIs24HourView(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                timePicker.setMinute(calendar.get(Calendar.MINUTE));
            } else {
                timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
                timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
            }

            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    if (onTimeChangedListener != null) {
                        onTimeChangedListener.onTimeChanged(view, hourOfDay, minute);
                    }
                }
            });

            return view;
        }

        public void setOnTimeChangedListener(TimePicker.OnTimeChangedListener onTimeChangedListener) {
            this.onTimeChangedListener = onTimeChangedListener;
        }
    }

    public static class DateTimePagerAdapter extends FragmentPagerAdapter {
        List<Fragment> fragments;

        public DateTimePagerAdapter(FragmentManager fragmentManager, List<Fragment> fragments) {
            super(fragmentManager);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
