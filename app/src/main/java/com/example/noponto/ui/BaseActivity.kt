package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.noponto.R
import com.example.noponto.databinding.AppBarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {

    // Each child activity must provide its own app bar binding
    protected abstract val appBarBinding: AppBarBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Go edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * Call this in the onCreate of child activities after setting the content view.
     */
    protected fun setupAppBar() {
        applyStatusBarInsets()
        setupClickListeners()
        loadUserName()
    }

    private fun applyStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarBinding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }
    }

    private fun setupClickListeners() {
        appBarBinding.menuIcon.setOnClickListener {
            val menuFragment = MenuDialogFragment()
            menuFragment.show(supportFragmentManager, "MenuDialogFragment")
        }

        appBarBinding.userLayout.setOnClickListener { view ->
            showProfilePopupMenu(view)
        }
    }

    private fun showProfilePopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.profile_dropdown_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    val intent = Intent(this, LoginActivityWithBinding::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        appBarBinding.userRole.text = name
                    }
                }
        }
    }
}