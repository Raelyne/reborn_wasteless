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

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val vm: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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

        // Binding to edit profile
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(AccountFragmentDirections.actionAccountToSettings())
        }

        //Observer for _loggedOut state
        vm.loggedOut.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                findNavController().navigate(AccountFragmentDirections.actionAccountToSignInSelection())
            }
        }

        //Listener for log out button
        binding.buttonLogOut.setOnClickListener {
            vm.logOut()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}