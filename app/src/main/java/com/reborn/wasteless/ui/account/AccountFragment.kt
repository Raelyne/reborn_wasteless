package com.reborn.wasteless.ui.account

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.reborn.wasteless.databinding.FragmentAccountBinding
import com.reborn.wasteless.utils.applyTopWindowInsets
import com.bumptech.glide.Glide

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val vm: AccountViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Apply padding to toolbar
        binding.accountToolbar.applyTopWindowInsets()

        //Observer for _loggedOut state
        vm.loggedOut.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                findNavController().navigate(AccountFragmentDirections.actionAccountToSignInSelection())
            }
        }

        // Observer for the full user object to get the image
        vm.currentUser.observe(viewLifecycleOwner) { user ->
            // 1. Check if the user has a profile picture URL
            if (user.profilePictureUrl.isNotEmpty()) {
                // 2. Use Glide to load it
                Glide.with(this)
                    .load(user.profilePictureUrl)
                    .placeholder(com.reborn.wasteless.R.drawable.no_photo_found) // Show this while loading
                    .error(com.reborn.wasteless.R.drawable.no_photo_found)       // Show this if error
                    .circleCrop() // Make the image round
                    .into(binding.profileImage)
            }
        }

        //Observer for username
        vm.usernameTag.observe(viewLifecycleOwner) { username ->
            binding.accountUsername.text = username
        }

        //Observer for account date of creation
        vm.dateOfCreation.observe(viewLifecycleOwner) { date ->
            binding.accountCreatedAt.text = date
        }

        //Listener for log out button
        binding.buttonLogOut.setOnClickListener {
            vm.logOut()
        }

        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(AccountFragmentDirections.actionAccountToEditProfile())
        }
        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(AccountFragmentDirections.actionAccountToSettings())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}