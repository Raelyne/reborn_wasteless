package com.reborn.wasteless.ui.account

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.reborn.wasteless.R
import com.reborn.wasteless.databinding.FragmentEditProfileBinding
import com.reborn.wasteless.utils.applyTopWindowInsets

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val vm: EditProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    // Register Activity Result for ImagePicker
    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK) {
                // Image Uri will not be null for RESULT_OK
                val fileUri = data?.data!!
                selectedImageUri = fileUri
                binding.profileImage.setImageURI(fileUri)
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editProfileToolbar.applyTopWindowInsets()

        binding.toolbarBack.setOnClickListener { findNavController().popBackStack() }

        // Load current data
        vm.currentUser.observe(viewLifecycleOwner) { user ->
            // Only set text if user hasn't typed anything yet (avoid overwriting user input on rotation)
            if (binding.textUsernameChange.text.isNullOrEmpty()) {
                binding.textUsernameChange.setText(user.username)
            }
            // Load existing image if we haven't picked a new one yet
            if (selectedImageUri == null && user.profilePictureUrl.isNotEmpty()) {
                Glide.with(this).load(user.profilePictureUrl).into(binding.profileImage)
            }
        }

        // image picker listener
        binding.profileImage.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(1024)         // Final image size will be less than 1 MB
                .maxResultSize(1080, 1080)  // Final image resolution will be less than 1080 x 1080
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }

        // update button
        binding.buttonUpdate.setOnClickListener {
            val newName = binding.textUsernameChange.text.toString()
            if (newName.isBlank()) {
                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.updateProfile(newName, selectedImageUri)
        }

        // Observe status
        vm.statusMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // Observe loading
        vm.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.buttonUpdate.isEnabled = !isLoading
            binding.buttonUpdate.text = if(isLoading) "Updating..." else "Update Profile"
        }

        vm.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) findNavController().popBackStack()
        }
    }
}