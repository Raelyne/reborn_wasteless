package com.reborn.wasteless

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.AppBarConfiguration
import com.reborn.wasteless.databinding.ActivityMainBinding
import com.reborn.wasteless.R
import com.reborn.wasteless.repo.AuthRepository
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        //Check if there's a current user logged in AFTER navController is loaded
        binding.root.post {
            val isSignedIn = authRepository.isUserSignedIn()
            val destination = if (isSignedIn) R.id.navigation_home else R.id.navigation_sign_in_selection
            navController.navigate(destination)
            Log.d("Auth", "User signed in: $isSignedIn, navigating to: $destination")
        }

        val navView: BottomNavigationView = binding.navView

        navView.setupWithNavController(navController)

        //Listener for navigation bar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.navigation_home -> {
                    showBottomNav()
                }
                R.id.navigation_diary -> {
                    showBottomNav()
                }
                R.id.navigation_tamagotchi -> {
                    hideBottomNav()
                }
                R.id.navigation_account -> {
                    showBottomNav()
                }
                R.id.navigation_login,
                R.id.navigation_sign_up,
                R.id.navigation_sign_in_selection,
                R.id.navigation_logging -> {
                    hideBottomNav()
                }
            }
        }
    }

    fun showBottomNav() {
        binding.navView.visibility = View.VISIBLE
    }

    fun hideBottomNav() {
        binding.navView.visibility = View.GONE
    }
}

