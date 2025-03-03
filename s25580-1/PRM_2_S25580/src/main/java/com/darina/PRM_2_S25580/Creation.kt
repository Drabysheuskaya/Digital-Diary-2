import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.darina.PRM_2_S25580.StorageMode
import com.darina.PRM_2_S25580.db.DB
import com.darina.PRM_2_S25580.R
import com.darina.PRM_2_S25580.model.Entity
import com.darina.PRM_2_S25580.rep.EntityRep
import com.darina.PRM_2_S25580.view_model.EntityViewModel
import com.darina.PRM_2_S25580.view_model.EntityViewModelStorage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class Creation : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var capturedImage: ImageView
    private lateinit var storageMode: StorageMode
    private lateinit var buttonCapturePhoto: Button
    private lateinit var buttonSelectFromGallery: Button
    private lateinit var buttonRestartCamera: Button
    private lateinit var buttonRecordAudio: Button
    private lateinit var buttonPlayAudio: Button
    private lateinit var buttonSave: Button
    private lateinit var editTextText: EditText
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var imageCapture: ImageCapture? = null
    private var isRecording = false
    private var lastRecordedFilePath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var buttonViewAllEntries: Button
    private var photoFile: File? = null
    private lateinit var entityViewModel: EntityViewModel
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 102
        private const val TAG = "Creation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.creation)

        setupUI()
        setupViewModel()
        requestPermissions()
    }

    private fun setupUI() {
        viewFinder = findViewById(R.id.viewFinder)
        capturedImage = findViewById(R.id.capturedImage)
        storageMode = findViewById(R.id.storageMode)
        buttonCapturePhoto = findViewById(R.id.buttonCapturePhoto)
        buttonSelectFromGallery = findViewById(R.id.buttonSelectFromGallery)
        buttonRestartCamera = findViewById(R.id.buttonRestartCamera)
        buttonRecordAudio = findViewById(R.id.buttonRecordAudio)
        buttonPlayAudio = findViewById(R.id.buttonPlayAudio)
        buttonSave = findViewById(R.id.buttonSave)
        editTextText = findViewById(R.id.editTextText)
        buttonViewAllEntries = findViewById(R.id.buttonViewAllEntries)

        buttonCapturePhoto.setOnClickListener { takePhoto() }
        buttonSelectFromGallery.setOnClickListener { selectFromGallery() }
        buttonRestartCamera.setOnClickListener { restartCamera() }
        buttonRecordAudio.setOnClickListener { toggleRecording() }
        buttonPlayAudio.setOnClickListener { togglePlayback() }
        buttonSave.setOnClickListener { saveEntry() }
        buttonViewAllEntries.setOnClickListener { viewAllEntries() }
    }

    private fun setupViewModel() {
        val diaryEntryDao = DB.getIstance(application).entityDao()
        val repository = EntityRep(diaryEntryDao)
        val viewModelFactory = EntityViewModelStorage(repository)
        entityViewModel = ViewModelProvider(this, viewModelFactory).get(EntityViewModel::class.java)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(viewFinder.display.rotation)
                .build()

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
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

        photoFile = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                    val rotatedBitmap = rotateImageIfRequired(bitmap, photoFile!!.absolutePath)
                    runOnUiThread {
                        capturedImage.setImageBitmap(rotatedBitmap)
                        capturedImage.visibility = View.VISIBLE
                        viewFinder.visibility = View.GONE
                        buttonCapturePhoto.visibility = View.GONE
                        buttonRestartCamera.visibility = View.VISIBLE
                        storageMode.setBackgroundBitmap(rotatedBitmap)
                        enableDrawingMode()
                        ensureAllButtonsVisible()
                        bringButtonsToFront()
                        buttonCapturePhoto.visibility = View.INVISIBLE
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            })
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResultLauncher.launch(intent)
    }

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            if (selectedImageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImageUri)
                val rotatedBitmap = rotateImageIfRequired(bitmap, selectedImageUri)
                runOnUiThread {
                    capturedImage.setImageBitmap(rotatedBitmap)
                    capturedImage.visibility = View.VISIBLE
                    viewFinder.visibility = View.GONE
                    buttonCapturePhoto.visibility = View.GONE
                    buttonRestartCamera.visibility = View.VISIBLE
                    storageMode.setBackgroundBitmap(rotatedBitmap)
                    enableDrawingMode()
                    ensureAllButtonsVisible()
                    bringButtonsToFront()
                    buttonCapturePhoto.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val ei = inputStream?.let { ExifInterface(it) }
        val orientation = ei?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
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

    private fun restartCamera() {
        capturedImage.setImageBitmap(null)
        capturedImage.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        storageMode.visibility = View.GONE
        buttonRestartCamera.visibility = View.GONE
        buttonCapturePhoto.visibility = View.VISIBLE
        clearDrawing()
        startCamera()
    }

    private fun clearDrawing() {
        storageMode.clear()
    }

    private fun enableDrawingMode() {
        capturedImage.visibility = View.GONE
        storageMode.visibility = View.VISIBLE
        storageMode.bringToFront()
        ensureAllButtonsVisible()
    }

    private fun ensureAllButtonsVisible() {
        buttonCapturePhoto.visibility = View.VISIBLE
        buttonSelectFromGallery.visibility = View.VISIBLE
        buttonRestartCamera.visibility = View.VISIBLE
        buttonRecordAudio.visibility = View.VISIBLE
        buttonSave.visibility = View.VISIBLE
        buttonViewAllEntries.visibility = View.VISIBLE
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)
            return
        }

        val audioFile = File(externalCacheDir?.absolutePath, "${System.currentTimeMillis()}.3gp")
        lastRecordedFilePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        buttonRecordAudio.text = "Stop Recording"
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        buttonRecordAudio.text = "Record Audio"
    }

    private fun togglePlayback() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(lastRecordedFilePath)
            prepare()
            start()
        }

        buttonPlayAudio.text = "Stop Playback"
        mediaPlayer?.setOnCompletionListener {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        buttonPlayAudio.text = "Play Audio"
    }

    private fun saveEntry() {
        val text = editTextText.text.toString()
        val image = if (capturedImage.drawable != null) {
            (capturedImage.drawable as BitmapDrawable).bitmap
        } else {
            null
        }
        val audioPath = lastRecordedFilePath

        if (text.isEmpty() && image == null && audioPath == null) {
            Toast.makeText(this, "Please add some content before saving.", Toast.LENGTH_SHORT).show()
            return
        }

        val outputStream = ByteArrayOutputStream()
        image?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val imageBytes = outputStream.toByteArray()

        val entity = Entity(
            text = text,
            photoData = imageBytes,
            voiceRecordingData = audioPath,
            latitude = currentLocation?.latitude ?: 0.0,
            longitude = currentLocation?.longitude ?: 0.0,
            timestamp = Date().time
        )
        entityViewModel.insert(entity) {

            Toast.makeText(this, "Entry saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun viewAllEntries() {
        val intent = Intent(this, EntityActivity::class.java)
        startActivity(intent)
    }

    private fun bringButtonsToFront() {
        buttonCapturePhoto.bringToFront()
        buttonSelectFromGallery.bringToFront()
        buttonRestartCamera.bringToFront()
        buttonRecordAudio.bringToFront()
        buttonSave.bringToFront()
        buttonViewAllEntries.bringToFront()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                takePhoto()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                toggleRecording()
            } else {
                Toast.makeText(this, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                    Log.d(TAG, "Current location: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            this.interval = 10000
            this.fastestInterval = 5000
            this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
