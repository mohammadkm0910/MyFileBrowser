package com.mohammadkk.myfilebrowser

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.mohammadkk.myfilebrowser.databinding.ActivityMainBinding
import com.mohammadkk.myfilebrowser.extension.externalStorage
import com.mohammadkk.myfilebrowser.extension.getExternalStoragePublicDir
import com.mohammadkk.myfilebrowser.extension.navigate
import com.mohammadkk.myfilebrowser.extension.sdcardStorage
import com.mohammadkk.myfilebrowser.fragment.HomeFragment
import com.mohammadkk.myfilebrowser.fragment.InternalFragment
import com.mohammadkk.myfilebrowser.helper.PATH_ARG_FRAG


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
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
    }
    private fun initDrawer() {
        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        val sdStorage = sdcardStorage
        val cardNavigation = binding.navView.menu.findItem(R.id.cardNav)
        cardNavigation.isVisible = sdStorage != null
        binding.navView.setNavigationItemSelectedListener(this)
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.homeNav -> {
                val currentFragment = visibleFragment
                if (currentFragment is HomeFragment) {
                    currentFragment.refreshFragment()
                } else navigate(HomeFragment.newInstance(), true)
            }
            R.id.internalNav -> {
                val path = externalStorage.absolutePath
                val currentFragment = visibleFragment
                if (currentFragment is InternalFragment) {
                    val base = currentFragment.requireArguments().getString(PATH_ARG_FRAG, "/") ?: ""
                    if (base != path) {
                        navigate(InternalFragment.newInstance(path), true)
                    } else currentFragment.refreshFragment()
                } else navigate(InternalFragment.newInstance(path), true)
            }
            R.id.cardNav -> {
                val path = sdcardStorage!!.absolutePath
                val currentFragment = visibleFragment
                if (currentFragment is InternalFragment) {
                    val base = currentFragment.requireArguments().getString(PATH_ARG_FRAG, "/") ?: ""
                    if (base != path) {
                        navigate(InternalFragment.newInstance(path), true)
                    } else currentFragment.refreshFragment()
                } else navigate(InternalFragment.newInstance(path), true)
            }
            R.id.downloadNav -> {
                val download = getExternalStoragePublicDir(Environment.DIRECTORY_DOWNLOADS)
                val downloadPath = download.absolutePath
                val currentFragment = visibleFragment
                if (currentFragment is InternalFragment) {
                    val base = currentFragment.requireArguments().getString(PATH_ARG_FRAG, "/") ?: ""
                    if (base != downloadPath) {
                        navigate(InternalFragment.newInstance(download.absolutePath), true)
                    } else currentFragment.refreshFragment()
                } else navigate(InternalFragment.newInstance(download.absolutePath), true)
            }
            R.id.aboutNav -> startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
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