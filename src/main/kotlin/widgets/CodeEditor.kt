package widgets

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.rememberTextFieldScrollState
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LineNumberColumn(
    textLayoutResultState: State<TextLayoutResult?>,
    lineNumberScrollState: ScrollState,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    lineNumberColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
    lineNumberBackgroundColor: Color = MaterialTheme.colors.surface.copy(alpha = 0.2f)
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val currentTextLayoutResult = textLayoutResultState.value

    val numbersColumnWidth = remember(currentTextLayoutResult, textStyle, density, textMeasurer) {
        val lines = currentTextLayoutResult?.lineCount ?: 1
        val maxLineNumText = lines.toString()
        val measuredTextWidth = textMeasurer.measure(AnnotatedString(maxLineNumText), style = textStyle).size.width
        density.run { measuredTextWidth.toDp() } + 16.dp
    }

    val lineNumbersString = remember(currentTextLayoutResult) {
        if (currentTextLayoutResult != null && currentTextLayoutResult.lineCount > 0) {
            (1..currentTextLayoutResult.lineCount).joinToString("\n")
        } else {
            "1"
        }
    }

    Box(
        modifier = modifier
            .width(numbersColumnWidth)
            .fillMaxHeight()
            .background(lineNumberBackgroundColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = lineNumbersString,
            style = textStyle,
            color = lineNumberColor,
            textAlign = TextAlign.End,
            modifier = Modifier
                .verticalScroll(lineNumberScrollState)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
    editorTextStyle: TextStyle = LocalTextStyle.current.copy(
        color = MaterialTheme.colors.onSurface,
        fontFamily = FontFamily.Monospace
    )
) {
    val textLayoutResultState: MutableState<TextLayoutResult?> =
        remember { mutableStateOf(null) }
    val currentTextLayout = textLayoutResultState.value

    val editorScrollState = rememberTextFieldScrollState(Orientation.Vertical)
    val lineNumberScrollState = rememberScrollState()

    LaunchedEffect(editorScrollState) {
        snapshotFlow { editorScrollState.offset }
            .distinctUntilChanged()
            .collect { offset ->
                lineNumberScrollState.scrollTo(offset.toInt())
            }
    }

    val cursorLine = remember(textFieldValue.selection, currentTextLayout) {
        currentTextLayout?.let { layoutResult ->
            if (textFieldValue.text.isNotEmpty()) {
                val offset = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                try {
                    layoutResult.getLineForOffset(offset)
                } catch (e: IndexOutOfBoundsException) {
                    textFieldValue.text.substring(0, offset).count { it == '\n' }
                }
            } else {
                0
            }
        } ?: 0
    }

    Row(modifier = modifier) {
        LineNumberColumn(
            textLayoutResultState = textLayoutResultState,
            lineNumberScrollState = lineNumberScrollState,
            textStyle = editorTextStyle,
        )

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = editorTextStyle,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                scrollState = editorScrollState,
                onTextLayout = { textLayoutResult ->
                    textLayoutResultState.value = textLayoutResult
                },
                decorationBox = { innerTextField ->
                    Box {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            currentTextLayout?.let { layout ->
                                if (cursorLine >= 0 && cursorLine < layout.lineCount) {
                                    val scrollOffset = editorScrollState.offset

                                    val lineTop = layout.getLineTop(cursorLine) - scrollOffset
                                    val lineBottom = layout.getLineBottom(cursorLine) - scrollOffset
                                    val lineWidth = size.width

                                    if (lineBottom > 0 && lineTop < size.height) {
                                        drawRect(
                                            color = highlightColor,
                                            topLeft = Offset(0f, lineTop),
                                            size = Size(lineWidth, (lineBottom - lineTop))
                                        )
                                    }
                                }
                            }
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

fun main() = application {
    var textState by remember {
        mutableStateOf(
            TextFieldValue(
                """
                fun main() = application {
                    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
                    Window(
                        onCloseRequest = {
                            val tempDir = File(TEMP_DIR_NAME)
                            if (tempDir.exists()) {
                                println("cleaning up temporary files on exit...")
                                tempDir.deleteRecursively()
                            }
                            exitApplication()
                        },
                        title = "Simple Kotlin/Java IDE",
                        state = windowState
                    ) {
                        Ide(File("."))
                    }
                }
                """.trimIndent(),
                selection = TextRange(0)
            )
        )
    }

    Window(onCloseRequest = ::exitApplication, title = "Text Editor with Synced Line Numbers") {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                CodeEditor(
                    textFieldValue = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    highlightColor = Color.Yellow.copy(alpha = 0.2f),
                    editorTextStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colors.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.body1.fontSize
                    )
                )
            }
        }
    }
}
