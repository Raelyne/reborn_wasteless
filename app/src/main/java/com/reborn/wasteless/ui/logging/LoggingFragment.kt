package com.reborn.wasteless.ui.logging

import android.app.Activity
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.databinding.FragmentLoggingBinding
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.reborn.wasteless.R
import com.reborn.wasteless.data.CalcType
import com.reborn.wasteless.data.WasteType
import java.util.Calendar
import android.view.WindowManager
import androidx.navigation.NavOptions
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoggingFragment : Fragment() {

    private var _binding: FragmentLoggingBinding? = null
    private val binding get() = _binding!!
    private val vm: LoggingViewModel by viewModels()

    //ActivityResult launcher
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val uri = result.data?.data
                    if (uri != null) {
                        binding.photoCaptureView.setImageURI(uri)
                        // NEW: stash for upload
                        vm.imageUri.value = uri
                    }
                }
                ImagePicker.RESULT_ERROR -> {
                    val errorMsg = ImagePicker.getError(result.data)
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoggingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This single block handles the initial "Autofill" AND any future updates.
        vm.dateTime.observe(viewLifecycleOwner) { timestamp ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp

            binding.dateTimeInput.text = formatCalendarToString(cal)
        }

        //UI features from top to bottom (according to XML)
        // gonna number them so they appear neater

        //1. Cancel button
        binding.toolbarLoggingNo.setOnClickListener {
            findNavController().popBackStack()
        }

        //2. Toolbar save button
        binding.toolbarLoggingYes.setOnClickListener {
            vm.saveAll(requireContext())
        }

        //2a. Big green save button
        binding.buttonSaveLog.setOnClickListener {
            vm.saveAll(requireContext())
        }

        //3. Photo Picker
        binding.photoCaptureView.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        //4. Input for date/time, it should autofill onCreate
        binding.dateTimeInput.setOnClickListener {
            showDateTimePicker()
        }

        //5. Title input
        binding.titleInput.doOnTextChanged { text, _, _, _ ->
            vm.title.value = text.toString()
        }

        //6. WasteType selection → VM update + show overlay
        binding.wasteTypeRadio.setOnCheckedChangeListener { _, checkedId ->
            //a. Figure out which WasteType was tapped
            val selectedType = when (checkedId) {
                R.id.button_unavoidable -> WasteType.UNAVOIDABLE
                R.id.button_avoidable   -> WasteType.AVOIDABLE
                else                     -> WasteType.FOOD_RELATED
            }
            Log.i("LoggingFragment", "User clicked on button $selectedType")

            //b. Push the observation into ViewModel (will refresh vm.selections)
            vm.wasteType.value = selectedType

            //c. Show overlay + load the RecyclerView
            showOverlay()
        }

        //6a. RecyclerView setup for overlay
        val adapter = WasteItemAdapter { item, qty ->
            vm.updateQuantity(item, qty)
        }

        //6b. Setting the default layout to be linear/vertical
        binding.recyclerItemWaste.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        //6c. Observe selected changes
        vm.selections.observe(viewLifecycleOwner) { list ->
            Log.d("LoggingFragment", "New selection by ${list.size}")
            adapter.submitList(list)
        }

        //6d. Overlay done button
        binding.overlayDone.setOnClickListener { hideOverlay() }

        //7. Calculation‐type radios
        binding.calculationTypeRadio.setOnCheckedChangeListener { _, id ->
            vm.calcType.value =
                if (id == R.id.button_grams) CalcType.GRAMS else CalcType.PORTION
        }

        //8. Live total‐weight display
        vm.totalWeight.observe(viewLifecycleOwner) { total ->
            binding.tvComputedWeight.text = getString(R.string.weight_format, total)
        }

        //9. React to save result
        vm.saveStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.navigation_home, false)
                    .build()
                findNavController().navigate(LoggingFragmentDirections.actionLoggingToDiary(),navOptions)
            }.onFailure { err ->
                Log.e("LoggingFragment", "Save failed")
                Toast.makeText(requireContext(), err.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Helper function to ensure date format is consistent
     * between the "Autofill" and the "Picker"
     *
     * @param calendar The instance of calendar dialog, based on the user's sys
     */
    private fun formatCalendarToString(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-indexed
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Format the output for the TextView
        // Example: 08/Dec/2025 03:45 AM
        return "%02d/%02d/%04d %02d:%02d".format(day, month, year, hour, minute)
    }

    /**
     * Function for showing the date picker -> followed by the time picker based on android's dialog lib
     * Call this function when DateTimeButton is clicked
     *
     */
    private fun showDateTimePicker() {
        val currentTimeStamp = vm.dateTime.value ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeStamp

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->

                // Show the TimePickerDialog right after the date selection
                TimePickerDialog(
                    requireContext(),
                    { _, selectedHour, selectedMinute ->

                        // Combine results into Calendar inst.
                        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)

                        // Store the combined timestamp in the ViewModel, so no necessary conversion is needed later
                        vm.dateTime.value = calendar.timeInMillis

                    },
                    calendar.get(Calendar.HOUR_OF_DAY), // Initial hour for TimePicker
                    calendar.get(Calendar.MINUTE), // Initial minute for TimePicker
                    false // Set to true for 24-hour format
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showOverlay() {
        binding.loggingOverlayPanel.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        binding.loggingOverlayPanel.visibility = View.GONE
    }
}

