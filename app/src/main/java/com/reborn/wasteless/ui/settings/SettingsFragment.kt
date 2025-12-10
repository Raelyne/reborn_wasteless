package com.reborn.wasteless.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.databinding.FragmentSettingsBinding
import com.reborn.wasteless.utils.applyTopWindowInsets

class SettingsFragment: Fragment() {

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


        //Apply padding to toolbar
        binding.settingsToolbar.applyTopWindowInsets()

        binding.toolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonChangePassword.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToChangePassword())
        }
        binding.buttonDeleteAccount.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToDeleteAccount())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}