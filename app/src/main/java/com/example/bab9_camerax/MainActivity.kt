package com.example.bab9_camerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.bab9_camerax.ui.theme.Bab9_CameraXTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Bab9_CameraXTheme {
                KameraKuApp()
            }
        }
    }
}

@Composable
fun KameraKuApp() {
    val context = LocalContext.current
    val cameraPermission = Manifest.permission.CAMERA

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(cameraPermission)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (hasCameraPermission) {
            CameraScreen(Modifier.padding(innerPadding))
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aplikasi memerlukan izin kamera untuk berfungsi.")
            }
        }
    }
}

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) } // Untuk Thumbnail

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PREVIEW KAMERA
        CameraPreview(
            onPreviewReady = { view -> previewView = view },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Binding kamera ketika PreviewView siap
        LaunchedEffect(previewView) {
            val view = previewView ?: return@LaunchedEffect

            try {
                // 1. Dapatkan Provider
                val provider = context.getCameraProvider()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA

                // 2. Setup Preview
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(view.surfaceProvider)
                }

                // 3. Setup ImageCapture
                val ic = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 4. BIND SEMUA USE CASE SEKALIGUS
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, ic)

                imageCapture = ic // Simpan referensi ImageCapture

                // Atur rotasi target (X.5.5)
                view.display?.rotation?.let {
                    imageCapture?.targetRotation = it
                }

                Log.i("CameraScreen", "Binding sukses.")

            } catch (e: Exception) {
                Log.e("CameraScreen", "Gagal inisialisasi kamera:", e)
                Toast.makeText(context, "Gagal memuat kamera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // TOMBOL DAN THUMBNAIL
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // THUMBNAIL (X.7.1)
            lastPhotoUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Foto Terakhir",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: Spacer(Modifier.size(64.dp)) // Placeholder

            // TOMBOL AMBIL FOTO (X.7.1)
            Button(
                onClick = {
                    if (imageCapture == null) {
                        Log.e("CameraScreen", "ImageCapture belum siap.")
                        Toast.makeText(context, "Kamera belum siap.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    takePhoto(context, imageCapture!!) { uri ->
                        lastPhotoUri = uri // Update state untuk thumbnail
                    }
                },
                contentPadding = PaddingValues(20.dp)
            ) {
                Text("Ambil Foto")
            }

            Spacer(Modifier.size(64.dp)) // Placeholder
        }
    }
}


@Composable
fun CameraPreview(onPreviewReady: (PreviewView) -> Unit, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                post { onPreviewReady(this) }
            }
        }
    )
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            cont.resume(future.get())
        }, ContextCompat.getMainExecutor(this))
    }

fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

    }

    val resolver = ctx.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("Gagal membuat URI MediaStore!")


    return ImageCapture.OutputFileOptions.Builder(resolver, uri, values).build()
}


fun takePhoto(ctx: Context, ic: ImageCapture, onSaved: (Uri) -> Unit) {

    try {
        val opt = outputOptions(ctx, "IMG_${System.currentTimeMillis()}")

        ic.takePicture(
            opt,
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val uri = result.savedUri!!
                    Log.d("CAMERA", "Foto berhasil disimpan: $uri")

                    // Memaksa MediaStore untuk segera meng-index file
                    ctx.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
                    )

                    onSaved(uri)
                    Toast.makeText(ctx, "Foto berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CAMERA", "Error mengambil foto (Kode ${exc.imageCaptureError})", exc)
                    Toast.makeText(ctx, "Gagal (Kode ${exc.imageCaptureError}): ${exc.message}", Toast.LENGTH_LONG).show()
                    exc.printStackTrace()
                }
            }
        )
    } catch (e: Exception) {
        Log.e("CAMERA", "Error sebelum takePicture dipanggil (OutputOptions failed):", e)
        Toast.makeText(ctx, "Penyimpanan gagal: ${e.message}", Toast.LENGTH_LONG).show()
    }
}