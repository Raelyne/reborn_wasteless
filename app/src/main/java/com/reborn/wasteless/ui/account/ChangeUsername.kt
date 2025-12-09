package com.example.wasteless.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.R
import com.reborn.wasteless.databinding.FragmentChangeUsernameBinding
import com.reborn.wasteless.repo.UserDataRepository
import com.google.firebase.auth.FirebaseAuth

class ChangeUsername : Fragment() {

    private var _binding: FragmentChangeUsernameBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val repo: UserDataRepository by lazy { UserDataRepository() }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChangeUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonSaveUsername.setOnClickListener {
            val newUsername = binding.textNewUsername.text.toString().trim()

            if (newUsername.isEmpty()) {
                Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                repo.updateUsername(currentUser.uid, newUsername)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Username updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_changeUsername_to_settings)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update username: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "You must be logged in to change your username", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
