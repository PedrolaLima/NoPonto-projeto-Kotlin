package com.example.noponto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.noponto.databinding.ActivityOccurrenceBinding
import com.example.noponto.databinding.AppBarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OccurrenceActivity : BaseActivity() {

    private lateinit var binding: ActivityOccurrenceBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    // armazena a Uri selecionada na galeria (se houver)
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            selectedImageUri = imageUri
            binding.attestationImage.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOccurrenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        setupInitialValues()
        setupInputMasks()
        setupValidation()

        binding.attestationLayout.setOnClickListener {
            openGalleryForImage()
        }

        binding.buttonConfirmar.setOnClickListener {
            // implementar cadastro de ocorrência
            binding.buttonConfirmar.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                val justificativa = binding.justificativaEditText.text.toString().trim()
                val dateStr = binding.dateEditText.text.toString().trim()
                val timeStr = binding.timeEditText.text.toString().trim()
                val atestadoPath = selectedImageUri?.toString() // sem Storage, armazenamos o URI como referência (opcional)

                try {
                    val repo = com.example.noponto.data.repository.OcorrenciaRepository()
                    val service = com.example.noponto.domain.services.OcorrenciaService(repo)
                    val result = service.criarOcorrenciaFromUi(justificativa, dateStr, timeStr, atestadoPath)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(this@OccurrenceActivity, "Ocorrência registrada com sucesso", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(this@OccurrenceActivity, "Erro: ${ex?.localizedMessage ?: "Falha ao registrar"}", Toast.LENGTH_LONG).show()
                            binding.buttonConfirmar.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OccurrenceActivity, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        binding.buttonConfirmar.isEnabled = true
                    }
                }
            }
        }

        binding.buttonCancelar.setOnClickListener {
            finish()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun setupInitialValues() {
        val brazilTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = brazilTimeZone }
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = brazilTimeZone }

        binding.dateEditText.setText(sdfDate.format(Date()))
        binding.timeEditText.setText(sdfTime.format(Date()))
    }

    private fun setupInputMasks() {
        // Date Mask with Validation
        binding.dateEditText.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == current) return

                var clean = s.toString().replace("[^\\d]".toRegex(), "")
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)

                if (clean.length >= 2) {
                    val day = clean.substring(0, 2).toIntOrNull()
                    if (day != null && day > 31) {
                        clean = "31" + if (clean.length > 2) clean.substring(2) else ""
                    }
                }
                if (clean.length >= 4) {
                    val month = clean.substring(2, 4).toIntOrNull()
                    if (month != null && month > 12) {
                        clean = clean.substring(0, 2) + "12" + if (clean.length > 4) clean.substring(4) else ""
                    }
                }
                if (clean.length == 8) {
                    val year = clean.substring(4, 8).toIntOrNull()
                    if (year != null && year > currentYear) {
                        clean = clean.substring(0, 4) + currentYear.toString()
                    }
                }

                val len = clean.length
                var newText = ""
                if (len > 0) newText = clean.substring(0, Math.min(2, len))
                if (len > 2) newText += "/" + clean.substring(2, Math.min(4, len))
                if (len > 4) newText += "/" + clean.substring(4, Math.min(8, len))

                current = newText
                s.replace(0, s.length, current)
            }
        })

        // Time Mask with Validation
        binding.timeEditText.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == current) return

                var clean = s.toString().replace("[^\\d]".toRegex(), "")

                if (clean.length >= 2) {
                    val hours = clean.substring(0, 2).toIntOrNull()
                    if (hours != null && hours > 23) {
                        clean = "23"
                    }
                }
                if (clean.length >= 4) {
                    val minutes = clean.substring(2, 4).toIntOrNull()
                    if (minutes != null && minutes > 59) {
                        clean = clean.substring(0, 2) + "59"
                    }
                }

                val len = clean.length
                var newText = ""
                if (len > 0) newText = clean.substring(0, Math.min(2, len))
                if (len > 2) newText += ":" + clean.substring(2, Math.min(4, len))

                current = newText
                s.replace(0, s.length, current)
            }
        })
    }

    private fun setupValidation() {
        binding.buttonConfirmar.isEnabled = false
        binding.buttonConfirmar.alpha = 0.5f

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        binding.justificativaEditText.addTextChangedListener(textWatcher)
        binding.dateEditText.addTextChangedListener(textWatcher)
        binding.timeEditText.addTextChangedListener(textWatcher)
    }

    private fun validateInputs() {
        val isJustificativaValid = binding.justificativaEditText.text.toString().isNotEmpty()
        val isDateValid = binding.dateEditText.text.toString().length == 10
        val isTimeValid = binding.timeEditText.text.toString().length == 5

        val allFieldsValid = isJustificativaValid && isDateValid && isTimeValid

        binding.buttonConfirmar.isEnabled = allFieldsValid
        binding.buttonConfirmar.alpha = if (allFieldsValid) 1.0f else 0.5f
    }
}