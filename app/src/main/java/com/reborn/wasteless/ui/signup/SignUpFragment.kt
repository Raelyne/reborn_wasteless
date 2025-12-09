package com.reborn.wasteless.ui.signup

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reborn.wasteless.R
import com.reborn.wasteless.databinding.FragmentSignUpBinding
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.data.model.AuthState

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val vm: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sign up button click handler
        binding.buttonSignup.setOnClickListener {
            val username = binding.textUsernameSignup.text.toString()
            val email = binding.textEmailSignup.text.toString()
            val password = binding.textPasswordSignup.text.toString()

            // Call ViewModel to handle registration logic
            // ViewModel will validate and create account with username
            vm.register(username, email, password)
        }

        //Just a pop back to SignInSelectionFragment
        binding.toolbarNo.setOnClickListener {
            findNavController().popBackStack()
        }

        // Observe registration state and react accordingly
        // Using viewLifecycleOwner prevents memory leaks
        vm.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Show loading indicator (optional)
                    binding.buttonSignup.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.buttonSignup.isEnabled = true
                    // Registration success, so we juts move them to login page (am lazy to make sessions for automatic login)
                    Toast.makeText(requireContext(),
                        getString(R.string.success_account_created), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(SignUpFragmentDirections.actionSignUpToLogin())
                    // Reset state after navigation
                    vm.resetState()
                }
                is AuthState.Error -> {
                    binding.buttonSignup.isEnabled = true
                    // Registration failed, so we just show a error msg based on AuthState(errormessage) where
                    // errormessage was passed on from the SignUpVM
                    val errorMsg = if (state.messageId != null) {
                        getString(state.messageId)
                    } else {
                        state.message ?: "Registration failed"
                    }

                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {
                    binding.buttonSignup.isEnabled = true
                    // Initial idle state, so do nothing
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}