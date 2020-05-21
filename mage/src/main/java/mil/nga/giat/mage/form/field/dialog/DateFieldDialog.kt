package mil.nga.giat.mage.form.field.dialog

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
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.date_time_dialog.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.DateFormField
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.*

/**
 * Created by wnewman on 2/9/17.
 */

class DateFieldDialog : DialogFragment() {

    companion object {
        private val CALENDAR_INSTANCE = "CALENDAR"
        private val FORM_FIELD_KEY_EXTRA = "FORM_FIELD_KEY_EXTRA"

        @JvmOverloads fun newInstance(fieldKey: String? = null): DateFieldDialog {
            val fragment = DateFieldDialog()
            val bundle = Bundle()
            bundle.putString(FORM_FIELD_KEY_EXTRA, fieldKey)

            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            fragment.arguments = bundle

            return fragment
        }
    }

    private var calendar = Calendar.getInstance()

    private lateinit var model: FormViewModel
    private var fieldKey: String? = null

    private var dateFormat: DateFormat? = null
    private var timeFormat: DateFormat? = null

    internal var datePickerFragment: DatePickerFragment? = null
    internal var timePickerFragment: TimePickerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = activity?.run {
            ViewModelProviders.of(this).get(FormViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        dateFormat = DateFormatFactory.format("MMM dd, yyyy", java.util.Locale.getDefault(), context)
        timeFormat = DateFormatFactory.format("HH:mm zz", java.util.Locale.getDefault(), context)
        calendar.timeZone = dateFormat!!.timeZone

        fieldKey = arguments?.getString(FORM_FIELD_KEY_EXTRA, null)
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
            datePickerFragment = DatePickerFragment.newInstance(fieldKey)
        }
        datePickerFragment!!.setOnDateChangedListener(DatePicker.OnDateChangedListener { dateView, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            dateTab.text = dateFormat!!.format(calendar.time)
        })

        if (timePickerFragment == null) {
            timePickerFragment = TimePickerFragment.newInstance(fieldKey)
        }
        timePickerFragment!!.setOnTimeChangedListener(TimePicker.OnTimeChangedListener { timeView, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)

            timeTab.text = timeFormat!!.format(calendar.time)
        })

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

        view.findViewById<View>(R.id.cancel).setOnClickListener { dismiss() }

        view.ok.setOnClickListener {
            val field = if (fieldKey == null) model.getTimestamp().value else model.getField(fieldKey!!) as DateFormField
            field?.value = calendar.time
            dismiss()
        }

        return view
    }

    class DatePickerFragment : Fragment() {
        companion object {
            fun newInstance(fieldKey: String?): DatePickerFragment {
                val fragment = DatePickerFragment()

                if (fieldKey != null) {
                    val bundle = Bundle()
                    bundle.putString(FORM_FIELD_KEY_EXTRA, fieldKey)
                    fragment.arguments = bundle
                }

                return fragment
            }
        }

        private lateinit var model: FormViewModel
        private var calendar = Calendar.getInstance()

        private var onDateChangedListener: DatePicker.OnDateChangedListener? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            model = activity?.run {
                ViewModelProviders.of(this).get(FormViewModel::class.java)
            } ?: throw Exception("Invalid Activity")

            val fieldKey = arguments?.getString(FORM_FIELD_KEY_EXTRA, null)
            val field = if (fieldKey == null) model.getTimestamp().value else model.getField(fieldKey) as DateFormField
            calendar.time = if (field?.value != null) field.value else Date()
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
            fun newInstance(fieldKey: String?): TimePickerFragment {
                val fragment = TimePickerFragment()

                if (fieldKey != null) {
                    val bundle = Bundle()
                    bundle.putString(FORM_FIELD_KEY_EXTRA, fieldKey)
                    fragment.arguments = bundle
                }

                return fragment
            }
        }

        private lateinit var model: FormViewModel
        private var calendar = Calendar.getInstance()

        private var onTimeChangedListener: TimePicker.OnTimeChangedListener? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            model = activity?.run {
                ViewModelProviders.of(this).get(FormViewModel::class.java)
            } ?: throw Exception("Invalid Activity")

            val fieldKey = arguments?.getString(FORM_FIELD_KEY_EXTRA, null)
            val field = if (fieldKey == null) model.getTimestamp().value else model.getField(fieldKey) as DateFormField
            calendar.time = if (field?.value != null) field.value else Date()
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
