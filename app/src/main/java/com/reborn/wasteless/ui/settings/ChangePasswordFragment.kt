package com.reborn.wasteless.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.databinding.FragmentChangePasswordBinding
import com.reborn.wasteless.utils.applyTopWindowInsets

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val vm: ChangePasswordViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.changePasswordToolbar.applyTopWindowInsets()

        binding.toolbarBack.setOnClickListener { findNavController().popBackStack() }

        // Setup Button
        binding.buttonUpdate.setOnClickListener {
            val current = binding.textCurrentPassword.text.toString()
            val newPass = binding.textNewPassword.text.toString()
            val confirm = binding.textConfirmNewPassword.text.toString()
            vm.changePassword(current, newPass, confirm)
        }

        // Observe Status
        vm.statusMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Observe Success to close fragment
        vm.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}