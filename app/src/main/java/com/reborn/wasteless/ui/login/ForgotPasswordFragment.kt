package com.reborn.wasteless.ui.login

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.R
import com.reborn.wasteless.data.model.AuthState
import com.reborn.wasteless.databinding.FragmentForgotPasswordBinding
import com.reborn.wasteless.ui.signup.SignUpFragmentDirections

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val vm: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Just a pop back to SignInSelectionFragment
        binding.toolbarNo.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonSubmit.setOnClickListener {
            val email = binding.textEmail.text.toString()

            vm.resetPassword(email)
        }

        vm.resetPasswordState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Show loading indicator (optional)
                    binding.buttonSubmit.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.buttonSubmit.isEnabled = true
                    Toast.makeText(requireContext(),
                        getString(R.string.success_forgot_password), Toast.LENGTH_LONG).show()
                    findNavController().navigate(ForgotPasswordFragmentDirections.actionForgotPasswordToLogin())
                    // Reset state after navigation
                    vm.resetState()
                }
                is AuthState.Error -> {
                    binding.buttonSubmit.isEnabled = true
                    // Submit failed, so we just show a error msg based on AuthState(errormessage)
                    val errorMsg = if (state.messageId != null) {
                        getString(state.messageId) // Load from strings.xml
                    } else {
                        state.message ?: "Unknown Error" // Fallback
                    }

                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
                AuthState.Idle -> {
                    binding.buttonSubmit.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}