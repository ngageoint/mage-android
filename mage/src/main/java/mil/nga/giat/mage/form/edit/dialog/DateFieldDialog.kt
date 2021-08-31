package mil.nga.giat.mage.form.edit.dialog

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.date_time_dialog.toolbar
import mil.nga.giat.mage.R
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.*

/**
 * Created by wnewman on 2/9/17.
 */

class DateFieldDialog: DialogFragment() {

    interface DateFieldDialogListener {
        fun onDate(date: Date?)
    }

    companion object {
        private val CALENDAR_INSTANCE = "CALENDAR"
        private const val TITLE_KEY = "TITLE_KEY"
        private const val TIMESTAMP_KEY = "TIMESTAMP_KEY"
        private const val CLEARABLE_KEY = "CLEARABLE_KEY"

        @JvmOverloads
        fun newInstance(title: String, date: Date, clearable: Boolean = true): DateFieldDialog {
            val fragment = DateFieldDialog()
            val bundle = Bundle()
            bundle.putString(TITLE_KEY, title)
            bundle.putLong(TIMESTAMP_KEY, date.time)
            bundle.putBoolean(CLEARABLE_KEY, clearable)

            fragment.setStyle(STYLE_NO_TITLE, 0)
            fragment.isCancelable = false
            fragment.arguments = bundle

            return fragment
        }
    }

    var listener: DateFieldDialogListener? = null

    private var title = "Date"
    private var calendar = Calendar.getInstance()
    private var clearable = true

    private var dateFormat: DateFormat? = null
    private var timeFormat: DateFormat? = null

    private var datePickerFragment: DatePickerFragment? = null
    private var timePickerFragment: TimePickerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)

        dateFormat = DateFormatFactory.format("MMM dd, yyyy", Locale.getDefault(), context)
        timeFormat = DateFormatFactory.format("HH:mm zz", Locale.getDefault(), context)
        calendar.timeZone = dateFormat!!.timeZone

        val timestamp = requireArguments().getLong(TIMESTAMP_KEY)
        calendar.time = Date(timestamp)
        title = requireArguments().getString(TITLE_KEY, "Date")
        clearable = requireArguments().getBoolean(CLEARABLE_KEY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(CALENDAR_INSTANCE, calendar)

        val manager = childFragmentManager
        manager.putFragment(outState, DatePickerFragment::class.java.name, datePickerFragment!!)
        manager.putFragment(outState, TimePickerFragment::class.java.name, timePickerFragment!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val localInflater = inflater.cloneInContext(context)
        val view = localInflater.inflate(R.layout.date_time_dialog, container, false)

        if (savedInstanceState != null) {
            calendar = savedInstanceState.getSerializable(CALENDAR_INSTANCE) as Calendar

            val manager = childFragmentManager
            datePickerFragment = manager.getFragment(savedInstanceState, DatePickerFragment::class.java.name) as DatePickerFragment?
            timePickerFragment = manager.getFragment(savedInstanceState, TimePickerFragment::class.java.name) as TimePickerFragment?
        }

        val tabLayout = view.findViewById<View>(R.id.tab_layout) as TabLayout
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout.tabMode = TabLayout.MODE_FIXED

        val dateTab = tabLayout.newTab().setText(dateFormat!!.format(calendar.time))
        tabLayout.addTab(dateTab)

        val timeTab = tabLayout.newTab().setText(timeFormat!!.format(calendar.time))
        tabLayout.addTab(timeTab)

        val viewPager = view.findViewById<View>(R.id.pager) as ViewPager

        if (datePickerFragment == null) {
            datePickerFragment = DatePickerFragment.newInstance(calendar.time)
        }
        datePickerFragment!!.setOnDateChangedListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            dateTab.text = dateFormat!!.format(calendar.time)
        }

        if (timePickerFragment == null) {
            timePickerFragment = TimePickerFragment.newInstance(calendar.time)
        }
        timePickerFragment!!.setOnTimeChangedListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)

            timeTab.text = timeFormat!!.format(calendar.time)
        }

        val adapter = DateTimePagerAdapter(childFragmentManager, listOf(datePickerFragment as Fragment, timePickerFragment as Fragment))
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.inflateMenu(R.menu.location_edit_menu)
        toolbar.title = title
        if (!clearable) {
            toolbar.menu.removeItem(R.id.clear)
        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear -> {
                    listener?.onDate(null)
                    dismiss()
                    true
                }
                R.id.apply -> {
                    listener?.onDate(calendar.time)
                    dismiss()
                    true
                }
                else -> super.onOptionsItemSelected(it)
            }
        }
    }

    class DatePickerFragment : Fragment() {
        companion object {
            private const val TIMESTAMP_KEY = "TIMESTAMP_KEY"

            fun newInstance(date: Date): DatePickerFragment {
                val fragment = DatePickerFragment()
                val bundle = Bundle()
                bundle.putLong(TIMESTAMP_KEY, date.time)
                fragment.arguments = bundle

                return fragment
            }
        }

        private var calendar = Calendar.getInstance()

        private var onDateChangedListener: DatePicker.OnDateChangedListener? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            calendar = Calendar.getInstance()
            val timestamp = requireArguments().getLong(TIMESTAMP_KEY, 0)
            calendar.time = Date(timestamp)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.dialog_date_picker, container, false)

            val datePicker = view.findViewById<View>(R.id.date_picker) as DatePicker
            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)) { dateView, year, monthOfYear, dayOfMonth ->
                if (onDateChangedListener != null) {
                    onDateChangedListener!!.onDateChanged(dateView, year, monthOfYear, dayOfMonth)
                }
            }

            return view
        }

        fun setOnDateChangedListener(onDateChangedListener: DatePicker.OnDateChangedListener) {
            this.onDateChangedListener = onDateChangedListener
        }
    }

    class TimePickerFragment : Fragment() {
        companion object {
            private const val TIMESTAMP_KEY = "TIMESTAMP_KEY"

            fun newInstance(date: Date): TimePickerFragment {
                val fragment = TimePickerFragment()
                val bundle = Bundle()
                bundle.putLong(TIMESTAMP_KEY, date.time)
                fragment.arguments = bundle

                return fragment
            }
        }

        private var calendar = Calendar.getInstance()

        private var onTimeChangedListener: TimePicker.OnTimeChangedListener? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            calendar = Calendar.getInstance()
            val timestamp = requireArguments().getLong(TIMESTAMP_KEY, 0)
            calendar.time = Date(timestamp)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.dialog_time_picker, container, false)

            val timePicker = view.findViewById<View>(R.id.time_picker) as TimePicker
            timePicker.setIs24HourView(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                timePicker.minute = calendar.get(Calendar.MINUTE)
            } else {
                timePicker.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                timePicker.currentMinute = calendar.get(Calendar.MINUTE)
            }

            timePicker.setOnTimeChangedListener { view, hourOfDay, minute ->
                if (onTimeChangedListener != null) {
                    onTimeChangedListener!!.onTimeChanged(view, hourOfDay, minute)
                }
            }

            return view
        }

        fun setOnTimeChangedListener(onTimeChangedListener: TimePicker.OnTimeChangedListener) {
            this.onTimeChangedListener = onTimeChangedListener
        }
    }

    class DateTimePagerAdapter(fragmentManager: FragmentManager, internal var fragments: List<Fragment>) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }
    }
}
