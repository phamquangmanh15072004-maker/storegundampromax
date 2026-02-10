package com.example.storepromax.presentation.detail

import android.content.Context
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    DisposableEffect(Unit) {
        centerNode.addChildNode(cameraNode)
        onDispose {
            centerNode.removeChildNode(cameraNode)
        }
    }

    LaunchedEffect(glbUrl) {
        isLoading = true
        val envJob = launch {
            try {
                environment = environmentLoader.createHDREnvironment(assetFileLocation = "sky_2k.hdr")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val modelJob = launch {
            getCachedGlbFile(context, glbUrl)?.let { file ->
                modelLoader.createModelInstance(file)?.let { instance ->
                    modelNode = ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 1.0f
                    ).apply {
                        playAnimation(0)
                    }
                }
            }
        }
        envJob.join()
        modelJob.join()
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                modelNode?.destroy()
                centerNode.destroy()
                cameraNode.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
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
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
suspend fun getCachedGlbFile(context: Context, glbUrl: String): File? = withContext(Dispatchers.IO) {
    try {
        val cacheDir = File(context.cacheDir, "glb_models")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "${glbUrl.hashCode()}.glb")
        if (file.exists() && file.length() > 0) return@withContext file

        URL(glbUrl).openStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        if (file.length() > 0) file else null
    } catch (e: Exception) {
        null
    }
}