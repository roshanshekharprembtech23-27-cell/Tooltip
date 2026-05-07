package com.example.tooltipguide

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.tooltipguide.tooltip.TooltipManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private var tooltipManager: TooltipManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDropdown()
        setupSubmitButton()
        setupTourButton()
    }

    // ── Populate the category dropdown with sample items ──────────────
    private fun setupDropdown() {
        val categories = listOf("Bug Report", "Feature Request", "General Feedback", "UI/UX", "Performance")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        findViewById<AutoCompleteTextView>(R.id.category_dropdown).setAdapter(adapter)
    }

    // ── Submit button placeholder logic ───────────────────────────────
    private fun setupSubmitButton() {
        val submitButton = findViewById<MaterialButton>(R.id.submit_button)
        val feedbackInput = findViewById<EditText>(R.id.feedback_input)
        val categoryDropdown = findViewById<AutoCompleteTextView>(R.id.category_dropdown)

        // Initial state: Disabled until a category is selected
        submitButton.isEnabled = false

        // Watch for changes in the category dropdown
        categoryDropdown.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val selected = s?.toString() ?: ""
                // Enable button only if a valid category (not the placeholder) is selected
                submitButton.isEnabled = selected.isNotEmpty() && selected != "Select a category"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        submitButton.setOnClickListener {
            // Clear the feedback input box
            feedbackInput.setText("")
            
            // Reset dropdown selection
            categoryDropdown.setText("Select a category", false)
            
            // Disable button again after reset
            submitButton.isEnabled = false

            Snackbar.make(it, "Feedback submitted – thank you!", Snackbar.LENGTH_SHORT).show()
        }
    }

    // ── Start Guided Tour ─────────────────────────────────────────────
    private fun setupTourButton() {
        val btnStartTour = findViewById<MaterialButton>(R.id.btn_start_tour)

        btnStartTour.setOnClickListener {
            // Read the JSON config from res/raw/tooltip_config.json
            val json = readRawResource(R.raw.tooltip_config)

            tooltipManager = TooltipManager(this, json).apply {
                onTourCompleted = {
                    Snackbar.make(
                        btnStartTour,
                        "🎉 Tour complete! You're all set.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            tooltipManager?.start()
        }
    }

    // ── Utility: read a raw resource file into a String ───────────────
    private fun readRawResource(resId: Int): String {
        val inputStream = resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.appendLine(line)
        }
        reader.close()
        return sb.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any active tooltip overlay when the activity is destroyed
        tooltipManager?.finish()
        tooltipManager = null
    }
}
