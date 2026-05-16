package com.example.storepromax.presentation.detail

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.storepromax.R
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@Composable
fun Model3DScreen(
    glbUrl: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val useSafeEnvironment = remember { isLikelyEmulator() }

    var environment by remember { mutableStateOf<Environment?>(null) }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cameraDistance by remember { mutableFloatStateOf(4.0f) }

    // 🌟 THÊM BIẾN LƯU TRỮ SỐ BYTE ĐỂ HIỂN THỊ MB
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }

    val centerNode = rememberNode(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = 0.5f, z = 4.0f)
        lookAt(centerNode)
    }

    LaunchedEffect(cameraDistance) {
        cameraNode.position = Position(y = 0.5f, z = cameraDistance)
        cameraNode.lookAt(centerNode)
    }

    DisposableEffect(Unit) {
        onDispose {
            modelNode?.destroy()
            modelNode = null
            environment = null
        }
    }

    BackHandler {
        onBackClick()
    }

    LaunchedEffect(glbUrl) {
        isLoading = true
        errorMessage = null
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L

        val envJob = launch {
            environment = if (useSafeEnvironment) {
                Log.w("Model3D", "Emulator detected, using safe 3D environment")
                Environment()
            } else {
                try {
                    environmentLoader.createHDREnvironment(assetFileLocation = "sky_2k.hdr") ?: Environment()
                } catch (e: Exception) {
                    Log.e("Model3D", "Lỗi tải HDR: ${e.message}", e)
                    Environment()
                }
            }
        }

        val modelJob = launch {
            try {
                // 🌟 LẮNG NGHE THÊM BYTES ĐÃ TẢI VÀ TỔNG BYTES
                val file = getCachedGlbFile(context, glbUrl) { progress, downloaded, total ->
                    downloadProgress = progress
                    downloadedBytes = downloaded
                    totalBytes = total
                }

                if (file != null) {
                    if (downloadProgress == 1f) kotlinx.coroutines.delay(400)

                    val instance = modelLoader.createModelInstance(file)
                    if (instance != null) {
                        modelNode = ModelNode(modelInstance = instance, scaleToUnits = 1.0f).apply {
                            playAnimation(0)
                        }
                    } else {
                        errorMessage = "File 3D bị lỗi hoặc không đúng định dạng."
                    }
                } else if (isActive) {
                    errorMessage = "Lỗi kết nối. Không thể tải mô hình 3D."
                }
            } catch (e: Exception) {
                if (isActive) errorMessage = "Lỗi xử lý mô hình: ${e.message}"
            }
        }

        envJob.join()
        modelJob.join()
        if (isActive) isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))
    ) {
        Crossfade(
            targetState = Triple(isLoading, errorMessage, modelNode),
            animationSpec = tween(durationMillis = 800),
            label = "3DSceneTransition"
        ) { (loading, error, node) ->
            when {
                loading -> {
                    // 🌟 TRUYỀN THÊM DỮ LIỆU VÀO GIAO DIỆN
                    RobotLoadingView(
                        progress = downloadProgress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = error, color = Color.DarkGray, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    }
                }
                node != null -> {
                    Scene(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { event ->
                                if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
                                    val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                                    if (scroll != 0f) {
                                        cameraDistance = (cameraDistance - scroll * 0.35f)
                                            .coerceIn(1.2f, 8.0f)
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            },
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = cameraNode,
                        childNodes = listOf(centerNode, node),
                        environment = environment ?: Environment(),
                        onFrame = { },
                        onGestureListener = rememberOnGestureListener(
                            onDoubleTap = { _, tapNode -> tapNode?.apply { scale *= 1.5f } }
                        )
                    )
                }
            }
        }
        IconButton(
            onClick = { onBackClick() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.White.copy(alpha = 0.8f), CircleShape).size(40.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color(0xFF333333))
        }
    }
}

private fun isLikelyEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
    val model = Build.MODEL.lowercase(Locale.US)
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
    val brand = Build.BRAND.lowercase(Locale.US)
    val device = Build.DEVICE.lowercase(Locale.US)
    val product = Build.PRODUCT.lowercase(Locale.US)
    val hardware = Build.HARDWARE.lowercase(Locale.US)

    return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("sdk_gphone") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            brand.startsWith("generic") && device.startsWith("generic")
}

@Composable
fun RobotLoadingView(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.robot_loading))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    // Tính toán số MB
    val downloadedMB = downloadedBytes / (1024f * 1024f)
    val totalMB = totalBytes / (1024f * 1024f)
    val mbText = if (totalBytes > 0L) {
        String.format(Locale.US, "%.1f / %.1f MB", downloadedMB, totalMB)
    } else {
        String.format(Locale.US, "%.1f MB", downloadedMB) // Trường hợp Server không trả về tổng dung lượng
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 🌟 XỬ LÝ LỖI LỆCH TÂM ROBOT
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val robotSize = 80.dp
            // Lấy tọa độ của đầu mút thanh Progress
            val tipPosition = maxWidth * animatedProgress
            // Căn giữa robot vào đầu mút, nhưng chặn (coerceIn) không cho robot tràn ra khỏi màn hình 2 bên
            val offsetInDp = (tipPosition - (robotSize / 2)).coerceIn(0.dp, maxWidth - robotSize)

            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier
                    .offset(x = offsetInDp)
                    .size(robotSize)
                    .scale(scaleX = -1f, scaleY = 1f)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100.dp)).background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = animatedProgress).background(Color(0xFF0D47A1))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🌟 BẢNG THÔNG SỐ CHUẨN UI/UX
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Đang tải dữ liệu 3D...",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mbText,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontWeight = FontWeight.Black,
                color = Color(0xFF0D47A1),
                fontSize = 20.sp
            )
        }
    }
}

// 🌟 HÀM TẢI FILE CẬP NHẬT TRẢ VỀ CẢ BYTES VÀ TOTAL
suspend fun getCachedGlbFile(
    context: Context,
    glbUrl: String,
    onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
): File? = withContext(Dispatchers.IO) {
    val cacheDir = File(context.cacheDir, "glb_models")
    if (!cacheDir.exists()) cacheDir.mkdirs()

    val finalFile = File(cacheDir, "${glbUrl.hashCode()}.glb")
    val tempFile = File(cacheDir, "${glbUrl.hashCode()}_temp.glb")

    if (finalFile.exists() && finalFile.length() > 0) {
        val size = finalFile.length()
        withContext(Dispatchers.Main) { onProgress(1f, size, size) }
        return@withContext finalFile
    }

    var connection: HttpURLConnection? = null
    try {
        val url = URL(glbUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("Model3D", "Lỗi Server: ${connection.responseCode}")
            return@withContext null
        }

        val fileLength = connection.contentLength.toLong()

        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val data = ByteArray(16 * 1024)
                var totalBytesRead = 0L
                var bytesRead: Int

                while (isActive) {
                    bytesRead = input.read(data)
                    if (bytesRead == -1) break
                    totalBytesRead += bytesRead.toLong()
                    output.write(data, 0, bytesRead)

                    if (fileLength > 0) {
                        val progress = (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) { onProgress(progress, totalBytesRead, fileLength) }
                    } else {
                        // Trường hợp server không báo trước file nặng bao nhiêu
                        withContext(Dispatchers.Main) { onProgress(0f, totalBytesRead, 0L) }
                    }
                }
            }
        }
        if (!isActive) {
            if (tempFile.exists()) tempFile.delete()
            return@withContext null
        }

        if (tempFile.exists() && tempFile.length() > 0) {
            tempFile.renameTo(finalFile)
            return@withContext finalFile
        }
        return@withContext null

    } catch (e: Exception) {
        Log.e("Model3D", "Lỗi tải GLB: ${e.message}")
        if (tempFile.exists()) tempFile.delete()
        return@withContext null
    } finally {
        connection?.disconnect()
    }
}
