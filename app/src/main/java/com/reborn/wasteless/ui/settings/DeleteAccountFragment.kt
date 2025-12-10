package com.reborn.wasteless.ui.settings

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.R
import com.reborn.wasteless.databinding.FragmentDeleteAccountBinding
import com.reborn.wasteless.utils.applyTopWindowInsets
import com.reborn.wasteless.data.model.AuthState
import com.reborn.wasteless.ui.settings.DeleteAccountFragmentDirections

class DeleteAccountFragment : Fragment() {

    private var _binding: FragmentDeleteAccountBinding? = null
    private val binding get() = _binding!!
    private val vm: DeleteAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeleteAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Apply padding to toolbar
        binding.deleteAccountToolbar.applyTopWindowInsets()

        binding.toolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }

// Set up the confirmation button (using button_update ID from your XML)
        binding.buttonUpdate.setOnClickListener {
            val password = binding.textLayoutPasswordDeleteAccount.editText?.text.toString()
            val confirmText = binding.textLayoutConfirmDeleteAccount.editText?.text.toString()

            // Call ViewModel to handle re-auth and deletion
            vm.confirmAndDeleteAccount(password, confirmText)
        }

        // Observe the deletion state
        vm.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Disable button during operation
                    binding.buttonUpdate.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.buttonUpdate.isEnabled = true

                    // Show success toast
                    Toast.makeText(requireContext(),
                        getString(R.string.success_delete_account), // Ensure you add this string ID
                        Toast.LENGTH_LONG).show()

                    findNavController().navigate(DeleteAccountFragmentDirections.actionDeleteAccountToSignInSelection())
                    vm.resetState()
                }
                is AuthState.Error -> {
                    binding.buttonUpdate.isEnabled = true

                    // Construct error message from ID or plain string
                    val errorMsg = if (state.messageId != null) {
                        getString(state.messageId)
                    } else {
                        state.message ?: getString(R.string.error_generic_fail)
                    }

                    // Show error toast
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {
                    binding.buttonUpdate.isEnabled = true
                    // Initial state, do nothing
                }
            }
        }
}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}