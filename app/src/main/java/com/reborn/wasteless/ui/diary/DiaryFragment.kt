package com.reborn.wasteless.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reborn.wasteless.databinding.FragmentDiaryBinding
import com.reborn.wasteless.ui.adapter.FoodLogAdapter
import com.reborn.wasteless.utils.applyTopWindowInsets
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!
    private val vm: DiaryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Apply padding to toolbar
        binding.diaryToolbar.applyTopWindowInsets()

        //Nav to logging or add entry
        binding.toolbarDiaryAdd.setOnClickListener {
            findNavController().navigate(DiaryFragmentDirections.actionDiaryToLogging())
        }

        /**
         * RecyclerView summary mapping
         */
        val recyclerDiary = binding.recyclerDiary

        //Pass the click listener (for safe args as well)
        val adapter = FoodLogAdapter(mode = "DIARY") { summary ->
            // Use SafeArgs to pass the ID
            val action = DiaryFragmentDirections.actionDiaryToLogging(logId = summary.id)
            findNavController().navigate(action)
        }
        recyclerDiary.adapter = adapter
        recyclerDiary.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vm.summary.observe(viewLifecycleOwner) { summaries ->
            adapter.updateData(summaries)
        }

        /**
         * Swipe handler for deleting logs
         */
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false // We don't want move/drag support

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val itemToDelete = adapter.getItem(position) ?: return
                adapter.removeAt(position)
                vm.deleteLog(itemToDelete.id)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerDiary)

        vm.deleteMessage.observe(viewLifecycleOwner) { messageId ->
            messageId?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}