package com.reborn.wasteless.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.R
import com.reborn.wasteless.databinding.FragmentSettingsBinding

class Settings: Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_account)
        }

        binding.buttonChangeUsername.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_changeUsername)
        }

        binding.buttonChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_changePassword)
        }
        binding.buttonDeleteAccount.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_deleteAccount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
