package com.reborn.wasteless.ui.login

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.databinding.FragmentSignInSelectionBinding

class SignInSelectionFragment : Fragment() {

    private var _binding: FragmentSignInSelectionBinding? = null
    private val binding get() = _binding!!
    private val vm: SignInSelectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate to login screen
        binding.buttonLogin.setOnClickListener {
            findNavController().navigate(SignInSelectionFragmentDirections.actionSelectionToLogin())
        }

        // Navigate to signup screen
        binding.buttonSignup.setOnClickListener {
            findNavController().navigate(SignInSelectionFragmentDirections.actionSelectionToSignUp())
        }
    }
}