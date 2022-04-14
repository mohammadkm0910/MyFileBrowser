package com.mohammadkk.myfilebrowser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mohammadkk.myfilebrowser.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_MyFileBrowser_AboutUs)
        setContentView(binding.root)
        binding.mainToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}