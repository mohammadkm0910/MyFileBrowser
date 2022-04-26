package com.mohammadkk.myfilebrowser

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.mohammadkk.myfilebrowser.databinding.ActivityMainBinding
import com.mohammadkk.myfilebrowser.extension.externalStorage
import com.mohammadkk.myfilebrowser.extension.getInternalStoragePublicDirectory
import com.mohammadkk.myfilebrowser.extension.navigate
import com.mohammadkk.myfilebrowser.extension.sdcardStorage
import com.mohammadkk.myfilebrowser.fragment.HomeFragment
import com.mohammadkk.myfilebrowser.fragment.InternalFragment


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            navigate(HomeFragment.newInstance())
            binding.navView.setCheckedItem(R.id.homeNav)
        }
        initDrawer()
        initNavigation()
    }
    private fun initDrawer() {
        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }
    private fun initNavigation() {
        val sdStorage = sdcardStorage
        val cardNavigation = binding.navView.menu.findItem(R.id.cardNav)
        cardNavigation.isVisible = sdStorage != null
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.homeNav -> navigate(HomeFragment.newInstance(), true)
                R.id.internalNav -> {
                    val path = externalStorage.absolutePath
                    if (visibleFragment is InternalFragment) {
                        val requireFragment = visibleFragment as InternalFragment
                        val base = requireFragment.requireArguments().getString("path_arg", "/") ?: ""
                        if (base != path) {
                            navigate(InternalFragment.newInstance(path), true)
                        } else requireFragment.refreshFragment()
                    } else navigate(InternalFragment.newInstance(path), true)
                }
                R.id.cardNav -> {
                    val path = sdStorage!!.absolutePath
                    if (visibleFragment is InternalFragment) {
                        val requireFragment = visibleFragment as InternalFragment
                        val base = requireFragment.requireArguments().getString("path_arg", "/") ?: ""
                        if (base != path) {
                            navigate(InternalFragment.newInstance(path), true)
                        } else requireFragment.refreshFragment()
                    } else navigate(InternalFragment.newInstance(path), true)
                }
                R.id.downloadNav -> {
                    val download = getInternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val downloadPath = download.absolutePath
                    if (visibleFragment is InternalFragment) {
                        val requireFragment = visibleFragment as InternalFragment
                        val base = requireFragment.requireArguments().getString("path_arg", "/") ?: ""
                        if (base != downloadPath) {
                            navigate(InternalFragment.newInstance(download.absolutePath), true)
                        } else requireFragment.refreshFragment()
                    } else navigate(InternalFragment.newInstance(download.absolutePath), true)
                }
                R.id.aboutNav -> startActivity(Intent(this, AboutActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    private val visibleFragment: Fragment?
        get() {
            val fragments = supportFragmentManager.fragments
            for (fragment in fragments) {
                if (fragment != null && fragment.isVisible) {
                    return fragment
                }
            }
            return null
        }
}