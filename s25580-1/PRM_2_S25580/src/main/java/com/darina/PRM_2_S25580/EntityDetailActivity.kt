import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.darina.PRM_2_S25580.StorageMode
import com.darina.PRM_2_S25580.db.DB
import com.darina.PRM_2_S25580.model.Entity
import com.darina.PRM_2_S25580.rep.EntityRep
import com.darina.PRM_2_S25580.view_model.EntityViewModel
import com.darina.PRM_2_S25580.view_model.EntityViewModelStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.darina.PRM_2_S25580.R

class EntityDetailActivity : AppCompatActivity() {
    private lateinit var viewModel: EntityViewModel
    private var imageCapture: ImageCapture? = null
    private var photoFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var lastRecordedFilePath: String? = null
    private lateinit var imageViewPhoto: ImageView
    private lateinit var viewFinder: PreviewView
    private lateinit var buttonPlayAudio: Button
    private lateinit var buttonRecordAudio: Button
    private lateinit var buttonUpdate: Button
    private lateinit var buttonRestartCamera: Button
    private lateinit var buttonTakePhoto: Button
    private lateinit var editTextDescription: EditText
    private lateinit var drawingView: StorageMode
    private lateinit var buttonViewAllEntries: Button

    private var diaryEntryId: Long = -1L

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val TAG = "EntityDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        setupViewModel()
        setupUI()
        requestPermissions()
        setupCamera() // Setup camera preview
        loadEntryData()
    }

    private fun setupViewModel() {
        val repository = EntityRep(DB.getIstance(application).entityDao())
        val viewModelFactory = EntityViewModelStorage(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(EntityViewModel::class.java)
    }

    private fun setupUI() {
        imageViewPhoto = findViewById(R.id.imageEntryPhoto)
        viewFinder = findViewById(R.id.viewFinder)
        buttonPlayAudio = findViewById(R.id.buttonPlayAudio)
        buttonRecordAudio = findViewById(R.id.buttonRecordAudio)
        buttonUpdate = findViewById(R.id.buttonUpdate)
        buttonRestartCamera = findViewById(R.id.buttonRestartCamera)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        editTextDescription = findViewById(R.id.editTextDescription)
        drawingView = findViewById(R.id.drawingView)
        buttonViewAllEntries = findViewById(R.id.buttonViewAllEntries)

        buttonRecordAudio.setOnClickListener { toggleRecording() }
        buttonPlayAudio.setOnClickListener { togglePlayback() }
        buttonUpdate.setOnClickListener { updateEntry() }
        buttonRestartCamera.setOnClickListener { restartCamera() }
        buttonTakePhoto.setOnClickListener { takePhoto() }
        buttonViewAllEntries.setOnClickListener { viewAllEntries() }
    }

    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
    }

    private fun loadEntryData() {
        diaryEntryId = intent.getLongExtra("DIARY_ENTRY_ID", -1L)
        if (diaryEntryId != -1L) {
            viewModel.getEntityById(diaryEntryId.toInt()).observe(this) { diaryEntry ->
                diaryEntry?.let { updateUI(it) }
            }
        }
    }

    private fun updateUI(diaryEntry: Entity) {
        editTextDescription.setText(diaryEntry.text)
        diaryEntry.photoData?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            imageViewPhoto.setImageBitmap(bitmap)
            imageViewPhoto.visibility = View.VISIBLE
            viewFinder.visibility = View.GONE
            drawingView.setBackgroundBitmap(bitmap)
            enableDrawingMode()
            bringButtonsToFront()
        }

        diaryEntry.voiceRecordingData?.let { audioData ->
            try {
                val audioFile = File.createTempFile("audio", "3gp", cacheDir)
                val fos = FileOutputStream(audioFile)
                fos.write(audioData.toByteArray())
                fos.close()
                lastRecordedFilePath = audioFile.absolutePath
                buttonPlayAudio.visibility = View.VISIBLE
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create temp audio file", e)
                buttonPlayAudio.visibility = View.GONE
            }
        } ?: run {
            buttonPlayAudio.visibility = View.GONE
        }
    }

    private fun bringButtonsToFront() {
        editTextDescription.bringToFront()
        buttonRestartCamera.bringToFront()
        buttonRecordAudio.bringToFront()
        buttonPlayAudio.bringToFront()
        buttonViewAllEntries.bringToFront()
        buttonUpdate.bringToFront()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            return
        }

        photoFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()

        imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                val rotatedBitmap = rotateImageIfRequired(bitmap, photoFile!!.absolutePath)
                runOnUiThread {
                    imageViewPhoto.setImageBitmap(rotatedBitmap)
                    imageViewPhoto.visibility = View.VISIBLE
                    viewFinder.visibility = View.GONE
                    buttonRestartCamera.visibility = View.VISIBLE
                    buttonTakePhoto.visibility = View.GONE
                    buttonUpdate.visibility = View.VISIBLE
                    drawingView.setBackgroundBitmap(rotatedBitmap)
                    enableDrawingMode()
                    bringButtonsToFront()
                }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }
        })
    }

    private fun restartCamera() {
        imageViewPhoto.setImageBitmap(null)
        imageViewPhoto.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        drawingView.visibility = View.GONE
        buttonRestartCamera.visibility = View.GONE
        buttonTakePhoto.visibility = View.VISIBLE
        buttonUpdate.visibility = View.GONE
        clearDrawing()
        setupCamera()
    }

    private fun clearDrawing() {
        drawingView.clear()
    }

    private fun enableDrawingMode() {
        imageViewPhoto.visibility = View.GONE
        drawingView.visibility = View.VISIBLE
        drawingView.bringToFront()
        ensureAllButtonsVisible()
    }

    private fun ensureAllButtonsVisible() {
        buttonRestartCamera.visibility = View.VISIBLE
        buttonRecordAudio.visibility = View.VISIBLE
        buttonPlayAudio.visibility = View.VISIBLE
        buttonUpdate.visibility = View.VISIBLE
        buttonViewAllEntries.visibility = View.VISIBLE
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
            buttonRecordAudio.text = getString(R.string.record_audio)
        } else {
            startRecording()
            buttonRecordAudio.text = getString(R.string.stop_recording)
        }
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Record audio permission not granted")
            return
        }

        lastRecordedFilePath = getRecordingFilePath()
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(lastRecordedFilePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "MediaRecorder prepare failed", e)
            }
        }
        isRecording = true
        buttonPlayAudio.visibility =  View.GONE
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        buttonPlayAudio.visibility = View.VISIBLE
    }

    private fun togglePlayback() {
        if (mediaPlayer?.isPlaying == true) {
            stopAudioPlayback()
            buttonPlayAudio.text = getString(R.string.play_audio)
        } else {
            startAudioPlayback()
            buttonPlayAudio.text = getString(R.string.stop_audio)
        }
    }

    private fun startAudioPlayback() {
        if (lastRecordedFilePath.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.no_audio_recording), Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(lastRecordedFilePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopAudioPlayback()
                    buttonPlayAudio.text = getString(R.string.play_audio)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Playback failed", e)
            }
        }
    }

    private fun stopAudioPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun getRecordingFilePath(): String {
        val directory = File(getExternalFilesDir(null)?.absolutePath + "/Recordings")
        if (!directory.exists()) directory.mkdirs()
        return "${directory.absolutePath}/${System.currentTimeMillis()}.3gp"
    }

    private fun updateEntry() {
        Log.e(TAG, "updateEntry called")
        if (diaryEntryId == -1L) {
            Toast.makeText(this, getString(R.string.error_entry_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.getEntityById(diaryEntryId.toInt()).observe(this) { existingEntry ->
            existingEntry?.let {
                val updatedText = editTextDescription.text.toString()

                val combinedBitmap = combineBitmapAndDrawing()

                val outputStream = ByteArrayOutputStream()
                combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val updatedPhotoData = outputStream.toByteArray()

                val updatedVoiceData = lastRecordedFilePath?.let {
                    val file = File(it)
                    if (file.exists()) file.readBytes() else null
                } ?: it.voiceRecordingData

                val updatedEntry = Entity(
                    id = it.id,
                    text = updatedText,
                    location = it.location,
                    photoData = updatedPhotoData,
                    voiceRecordingData = updatedVoiceData.toString(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )

                viewModel.update(updatedEntry)

                Toast.makeText(this, getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show()

                runOnUiThread {
                    imageViewPhoto.setImageBitmap(combinedBitmap)
                    imageViewPhoto.visibility = View.VISIBLE
                    drawingView.clear()
                    drawingView.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun combineBitmapAndDrawing(): Bitmap {
        val photoBitmap = (imageViewPhoto.drawable as? BitmapDrawable)?.bitmap
        val drawingBitmap = drawingView.exportDrawing()

        if (photoBitmap == null || drawingBitmap == null) {
            Log.e(TAG, getString(R.string.bitmap_data_null))
            return photoBitmap ?: drawingBitmap!!
        }

        val combinedBitmap = Bitmap.createBitmap(drawingView.width, drawingView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)

        val photoMatrix = Matrix()
        val scale: Float = combinedBitmap.width.toFloat() / photoBitmap.width
        photoMatrix.postScale(scale, scale)
        canvas.drawBitmap(photoBitmap, photoMatrix, null)

        canvas.drawBitmap(drawingBitmap, 0f, 0f, null)

        return combinedBitmap
    }

    private fun rotateImageIfRequired(img: Bitmap, photoPath: String): Bitmap {
        val ei = ExifInterface(photoPath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Log.e(TAG, "Not all permissions granted")
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun viewAllEntries() {
        startActivity(Intent(this, EntityActivity::class.java))
    }
}
