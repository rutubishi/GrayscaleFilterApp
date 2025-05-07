package com.rutubishi.imagefilterktcpp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.rutubishi.imagefilterktcpp.ui.theme.ImageFilterKtCppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageFilterKtCppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Grayscale App",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                },
                    ) { innerPadding ->
                    GrayscaleScreen(
                        modifier = Modifier.padding(innerPadding),
                        processImageNative = ::processToGrayscale,
                        processImageNative2 = ::processToGrayscaleOptimal
                    )
                }
            }
        }
    }


    external fun jniWelcomeMessage(): String

    external fun processToGrayscale(bitmap: Bitmap)

    external fun processToGrayscaleOptimal(bitmap: Bitmap): Bitmap

    companion object {
        init {
            System.loadLibrary("image-filter")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun GrayscaleScreen(
    modifier: Modifier,
    processImageNative: (Bitmap) -> Unit = {},
    processImageNative2: (Bitmap) -> Bitmap,
) {
0
    val context = LocalContext.current

    val bitmapOptions = BitmapFactory.Options().apply { this.inSampleSize = 2 }

    var bitmap by remember {
        mutableStateOf(
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.sample_image,
                bitmapOptions
            )
        )
    }

    var bitmapKtx by remember {
        mutableStateOf(
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.sample_image,
                bitmapOptions
            )
        )
    }

    var processedBitmap by remember { mutableStateOf(bitmap) }
    var processedBitmapKtx by remember { mutableStateOf(bitmapKtx) }

    var timer by remember { mutableStateOf(TransformTimer()) }
    val scrollState = rememberScrollState()


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.Center
    ) {

        IconButton(onClick =  {
            processedBitmapKtx = bitmapKtx
            processedBitmap = bitmap
            timer = timer.copy(cppTime = null, ktxTime = null)
        }) {
            Icon(
                imageVector = Icons.TwoTone.Refresh,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }

        // C++ GrayScale
        ImageGrayScaler(
            processedBitmap = processedBitmap,
            time = timer.cppTime,
            onGrayFinish = {
                val time = measureTimeMillis {
                    //processImageNative(bitmap)
                    processImageNative2(bitmap)
                }
                timer = timer.copy(cppTime = time)
                processedBitmap = processImageNative2(bitmap)
            },
            buttonLabel = "Native C++"
        )

        // KTX GrayScale
        ImageGrayScaler(
            processedBitmap = processedBitmapKtx,
            time = timer.ktxTime,
            onGrayFinish = {
                val time = measureTimeMillis {
                    ktxToGrayScale(bitmapKtx)
                }
                timer = timer.copy(ktxTime = time)
                processedBitmapKtx = processImageNative2(bitmapKtx)
            },
            buttonLabel = "KTX"
        )

    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ImageGrayScaler(
    scope: CoroutineScope = rememberCoroutineScope { Dispatchers.IO.limitedParallelism(1) },
    processedBitmap: Bitmap,
    time: Long?,
    onGrayFinish: () -> Unit,
    buttonLabel: String = "KTX",

) {
    Column {
        Image(
            bitmap = processedBitmap.asImageBitmap(),
            contentDescription = "Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
        )

        time?.let {
            Text(text = "$buttonLabel: $it ms")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    onGrayFinish()
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),

        ) {
            Text(text = "Grayscale It ($buttonLabel)!")
        }
    }
}

fun ktxToGrayScale(original: Bitmap): Bitmap {
    val width = original.width
    val height = original.height
    val grayscaleBitmap = createBitmap(width, height)

    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)

    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter

    canvas.drawBitmap(original, 0f, 0f, paint)

    return original
}

data class TransformTimer(
    val cppTime: Long? = null,
    val ktxTime: Long? = null,
)