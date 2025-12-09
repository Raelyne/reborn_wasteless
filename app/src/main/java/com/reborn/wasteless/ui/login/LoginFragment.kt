package com.reborn.wasteless.ui.login

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reborn.wasteless.databinding.FragmentLoginBinding
import com.reborn.wasteless.data.model.AuthState
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.R

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Login button click handler
        binding.buttonLogin.setOnClickListener {
            val email = binding.textEmail.text.toString()
            val password = binding.textPassword.text.toString()

            // Call ViewModel to handle login logic
            vm.login(email, password)
        }

        //Just a pop back to SignInSelectionFragment
        binding.toolbarNo.setOnClickListener {
            findNavController().popBackStack()
        }

        //Nav to ForgotPasswordFragment
        binding.forgotPassword.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginToForgotPassword())
        }

        // Observe login state and react accordingly
        // Using viewLifecycleOwner prevents memory leaks
        vm.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Meant to have logic for loading indicator but none atm
                    // You can disable button or show progress bar here
                    binding.buttonLogin.isEnabled = false
                }
                is AuthState.Success -> {
                    // Login successful
                    binding.buttonLogin.isEnabled = false

                    Toast.makeText(requireContext(),
                        getString(R.string.success_login), Toast.LENGTH_SHORT).show()
                    // Navigate to home fragment
                    findNavController().navigate(LoginFragmentDirections.actionLoginToHome())
                    // Reset state after navigation
                    vm.resetState()
                }
                is AuthState.Error -> {
                    // Login failed - show error message
                    binding.buttonLogin.isEnabled = true

                    val errorMsg = if (state.messageId != null) {
                        getString(state.messageId)
                    } else {
                        state.message ?: "Login failed"
                    }

                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {
                    // Initial state - do nothing
                    binding.buttonLogin.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}