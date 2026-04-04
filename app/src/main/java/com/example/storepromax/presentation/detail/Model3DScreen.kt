package com.example.storepromax.presentation.detail

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun Model3DScreen(
    glbUrl: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    var environment by remember { mutableStateOf<Environment?>(null) }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isSceneActive by remember { mutableStateOf(true) }

    BackHandler {
        isSceneActive = false
        onBackClick()
    }

    val centerNode = rememberNode(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = 0.5f, z = 4.0f)
        lookAt(centerNode)
    }
    LaunchedEffect(glbUrl) {
        isLoading = true
        errorMessage = null

        val envJob = launch {
            try {
                environment = environmentLoader.createHDREnvironment(
                    assetFileLocation = "sky_2k.hdr"
                )
            } catch (e: Exception) {
                Log.e("Model3D", "Lỗi HDR: ${e.message}")
            }
        }

        val modelJob = launch {
            try {
                val file = getCachedGlbFile(context, glbUrl)
                if (file != null) {
                    val instance = modelLoader.createModelInstance(file)
                    if (instance != null) {
                        modelNode = ModelNode(
                            modelInstance = instance,
                            scaleToUnits = 1.0f
                        ).apply {
                            playAnimation(0)
                        }
                    } else {
                        errorMessage = "Không thể parse dữ liệu mô hình 3D"
                    }
                } else {
                    errorMessage = "Không thể tải file từ Server (Kiểm tra mạng hoặc Link)"
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi xử lý mô hình: ${e.message}"
            }
        }

        envJob.join()
        modelJob.join()
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Lỗi không xác định",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            isSceneActive && modelNode != null && environment != null -> {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    cameraNode = cameraNode,
                    childNodes = listOf(centerNode, modelNode!!),
                    environment = environment!!,

                    onFrame = { },
                    onGestureListener = rememberOnGestureListener(
                        onDoubleTap = { _, node ->
                            node?.apply { scale *= 1.5f }
                        }
                    )
                )
            }
        }

        IconButton(
            onClick = {
                isSceneActive = false
                onBackClick()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

suspend fun getCachedGlbFile(context: Context, glbUrl: String): File? = withContext(Dispatchers.IO) {
    val cacheDir = File(context.cacheDir, "glb_models")
    if (!cacheDir.exists()) cacheDir.mkdirs()

    val finalFile = File(cacheDir, "${glbUrl.hashCode()}.glb")
    val tempFile = File(cacheDir, "${glbUrl.hashCode()}_temp.glb") // 🌟 Tạo file tạm

    if (finalFile.exists() && finalFile.length() > 0) {
        return@withContext finalFile
    }

    try {
        URL(glbUrl).openStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (tempFile.exists() && tempFile.length() > 0) {
            tempFile.renameTo(finalFile)
            return@withContext finalFile
        }
        return@withContext null
    } catch (e: Exception) {
        Log.e("Model3D", "Lỗi getCachedGlbFile: ${e.message}")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        return@withContext null
    }
}