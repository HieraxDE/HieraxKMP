import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

const val TEMP_DIR_NAME = "ide_temp_run"
const val TEMP_KOTLIN_SCRIPT_NAME = "Main.kt"
const val TEMP_JAVA_SCRIPT_NAME = "Main.java"
const val TEMP_JAR_NAME = "Main.jar"
const val TEMP_CLASS_NAME = "Main"

@Composable
@Preview
fun Ide() {
    var codeInput by remember {
        mutableStateOf(TextFieldValue(
            """
            // These are template programs for Kotlin and Java provided by the IDE.    
                
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello from Java!");
                }
            }

            // fun main() {
            //     println("Hello from Simple Kotlin IDE!")
            //     for (i in 1..5) {
            //         println("Count: " + i)
            //         Thread.sleep(500)
            //     }
            // }
            """.trimIndent()
        ))
    }
    var outputText by remember { mutableStateOf("Output will appear here...\n") }
    var isRunning by remember { mutableStateOf(false) }
    var currentProcess by remember { mutableStateOf<Process?>(null) }
    var isKotlinMode by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val defaultColorTheme = IDETheme(
        SpanStyle(color = Color.Cyan, fontWeight = FontWeight.Bold),
        SpanStyle(Color.Blue),
        SpanStyle(Color.DarkGray),
        SpanStyle(Color.Blue),
        SpanStyle(Color.hsv(30f, 0.76f, 0.4f)),
        SpanStyle(Color.Blue),
        SpanStyle(Color.Black),
        SpanStyle(Color.hsv(24f, 1f, 1f)),
        SpanStyle(Color.hsv(24f, 1f, 1f)),
        SpanStyle(Color.hsv(104f, 0.82f, 0.25f)),
        SpanStyle(Color.Blue),
        Color.White,
        Color.Gray,
        SpanStyle(color = Color.Red, textDecoration = TextDecoration.Underline)
    )
    val tempDir = File(TEMP_DIR_NAME)

    fun appendOutput(line: String) {
        outputText += line + "\n"
    }

    fun clearOutput() {
        outputText = ""
    }

    fun cleanupTempFiles() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
            appendOutput("Cleaned up temporary files.")
        }
    }

    fun runCode() {
        if (isRunning) return
        clearOutput()
        isRunning = true
        appendOutput("Starting execution (${if (isKotlinMode) "Kotlin" else "Java"})...")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1. Create temp directory
                if (!tempDir.exists()) tempDir.mkdirs()

                val scriptFileName = if (isKotlinMode) TEMP_KOTLIN_SCRIPT_NAME else TEMP_JAVA_SCRIPT_NAME
                val scriptFile = File(tempDir, scriptFileName)
                scriptFile.writeText(codeInput.text)
                appendOutput("Saved code to ${scriptFile.absolutePath}")

                if (isKotlinMode) {
                    appendOutput("Compiling Kotlin...")
                    val kotlincProcessBuilder = ProcessBuilder(
                        "kotlinc",
                        scriptFile.name,
                        "-d", TEMP_JAR_NAME,
                        "-include-runtime"
                    ).directory(tempDir).redirectErrorStream(true)

                    val compileProcess = kotlincProcessBuilder.start()
                    currentProcess = compileProcess
                    BufferedReader(InputStreamReader(compileProcess.inputStream)).useLines { lines ->
                        lines.forEach { appendOutput("KOTLINC: $it") }
                    }
                    val compileExitCode = compileProcess.waitFor()
                    currentProcess = null

                    if (compileExitCode != 0) {
                        appendOutput("Kotlin compilation failed with exit code $compileExitCode.")
                        return@launch
                    }
                    appendOutput("Kotlin compilation successful: ${File(tempDir, TEMP_JAR_NAME).absolutePath}")

                    appendOutput("Running Kotlin JAR...")
                    val javaProcessBuilder = ProcessBuilder(
                        "java", "-jar", TEMP_JAR_NAME
                    ).directory(tempDir).redirectErrorStream(true)

                    val runProcess = javaProcessBuilder.start()
                    currentProcess = runProcess
                    BufferedReader(InputStreamReader(runProcess.inputStream)).useLines { lines ->
                        lines.forEach { appendOutput(it) }
                    }
                    val runExitCode = runProcess.waitFor()
                    appendOutput("Kotlin execution finished with exit code $runExitCode.")

                } else {
                    appendOutput("Compiling Java...")
                    val javacProcessBuilder = ProcessBuilder(
                        "javac",
                        scriptFile.name
                    ).directory(tempDir).redirectErrorStream(true)

                    val compileProcess = javacProcessBuilder.start()
                    currentProcess = compileProcess
                    BufferedReader(InputStreamReader(compileProcess.inputStream)).useLines { lines ->
                        lines.forEach { appendOutput("JAVAC: $it") }
                    }
                    val compileExitCode = compileProcess.waitFor()
                    currentProcess = null

                    if (compileExitCode != 0) {
                        appendOutput("Java compilation failed with exit code $compileExitCode.")
                        return@launch
                    }
                    // Check if .class file was created (UserScript.class)
                    val classFile = File(tempDir, "$TEMP_CLASS_NAME.class")
                    if (!classFile.exists()) {
                        appendOutput("Java compilation seemed to succeed, but $TEMP_CLASS_NAME.class not found. ensure your Java code has a public class $TEMP_CLASS_NAME.")
                        return@launch
                    }
                    appendOutput("Java compilation successful: ${classFile.absolutePath}")


                    appendOutput("Running Java Class...")
                    val javaProcessBuilder = ProcessBuilder(
                        "java",
                        TEMP_CLASS_NAME
                    ).directory(tempDir).redirectErrorStream(true)


                    val runProcess = javaProcessBuilder.start()
                    currentProcess = runProcess
                    BufferedReader(InputStreamReader(runProcess.inputStream)).useLines { lines ->
                        lines.forEach { appendOutput(it) }
                    }
                    val runExitCode = runProcess.waitFor()
                    appendOutput("Java execution finished with exit code $runExitCode.")
                }

            } catch (e: Exception) {
                appendOutput("ERROR: ${e.message}")
                e.printStackTrace()
            } finally {
                currentProcess = null
                isRunning = false
                cleanupTempFiles()
            }
        }
    }

    fun stopCode() {
        if (!isRunning || currentProcess == null) return
        appendOutput("Attempting to stop process...")
        coroutineScope.launch(Dispatchers.IO) {
            try {
                currentProcess?.destroyForcibly()
                val exitCode = currentProcess?.waitFor()
                appendOutput("Process stopped by user (exit code: ${exitCode ?: "unknown"}).")
            } catch (e: Exception) {
                appendOutput("Error stopping process: ${e.message}")
            } finally {
                currentProcess = null
                isRunning = false
                cleanupTempFiles()
            }
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Simple IDE") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Java", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = isKotlinMode,
                            onCheckedChange = {
                                isKotlinMode = it
                                appendOutput("Switched to ${if (it) "Kotlin" else "Java"} mode.")
                            },
                            enabled = !isRunning,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary,
                                uncheckedThumbColor = MaterialTheme.colors.primaryVariant,
                                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                uncheckedTrackColor = MaterialTheme.colors.primaryVariant.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Kotlin", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = { runCode() },
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Run", tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Run", color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { stopCode() },
                        enabled = isRunning,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Filled.Delete, "Stop", tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", color = Color.White)
                    }
                }
            )

            BasicTextField(
                visualTransformation = {
                    val transformedText = buildAnnotatedString {
                        val text = it.text
                        var currentIndex = 0

                        val keywordPattern = "\\b(public|private|protected|abstract|class|extends|final|implements|interface|native|new|static|strictfp|synchronized|transient|volatile|break|case|continue|default|do|else|for|if|return|switch|while|assert|finally|throw|throws|try|import|package|super|this|void|enum|instanceof|var|yield|sealed|permits|record|module|requires|exports|opens|uses|provides|to|with|transitive)\\b"
                        val identifierPattern = "\\b[A-Za-z][A-Za-z0-9_]*\\b"
                        val booleanPattern = "\\b(true|false)\\b"
                        val typePattern = "\\b(boolean|byte|char|double|float|int|long|short|void|null)\\b"
                        val numberPattern = "\\b\\d+\\b"
                        val symbolsPattern = "[!@#$%^&*()\\[\\]{}\\-+=|:;/?.,<>~]"
                        val commentPattern = "(/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/|//[^\\n]*)"
                        val stringPattern = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""
                        val charPattern = "'([^'\\\\]|\\\\[btnfr'\"\\\\])'"

                        val combinedPattern = Pattern.compile(
                            "$keywordPattern|$identifierPattern|$booleanPattern|$typePattern|$numberPattern|$commentPattern|$stringPattern|$charPattern|$symbolsPattern"
                        )

                        val matcher = combinedPattern.matcher(text)

                        while (currentIndex < text.length) {
                            if (matcher.find(currentIndex)) {
                                if (matcher.start() > currentIndex) {
                                    append(text.substring(currentIndex, matcher.start()))
                                }
                                
                                val matchedText = matcher.group()
                                
                                when {
                                    matchedText.matches(Regex(keywordPattern)) -> {
                                        withStyle(defaultColorTheme.keywords) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(identifierPattern)) -> {
                                        withStyle(defaultColorTheme.identifiers) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(booleanPattern)) -> {
                                        withStyle(defaultColorTheme.literals) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(typePattern)) -> {
                                        withStyle(defaultColorTheme.types) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(numberPattern)) -> {
                                        withStyle(defaultColorTheme.numbers) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(commentPattern)) -> {
                                        withStyle(defaultColorTheme.comments) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(stringPattern)) -> {
                                        withStyle(defaultColorTheme.strings) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(charPattern)) -> {
                                        withStyle(defaultColorTheme.strings) {
                                            append(matchedText)
                                        }
                                    }
                                    matchedText.matches(Regex(symbolsPattern)) -> {
                                        withStyle(defaultColorTheme.scopes) {
                                            append(matchedText)
                                        }
                                    }
                                    else -> {
                                        withStyle(defaultColorTheme.error) {
                                            append(matchedText)
                                        }
                                    }
                                }
                                currentIndex = matcher.end()
                            } else {
                                append(text.substring(currentIndex))
                                break
                            }
                        }
                    }
                    TransformedText(transformedText, offsetMapping = OffsetMapping.Identity)
                },
                value = codeInput,
                onValueChange = { codeInput = it },
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.Gray)
                    .onPreviewKeyEvent { keyEvent: KeyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                            val text = codeInput.text
                            val start = codeInput.selection.start
                            val end = codeInput.selection.end
                            val newText = buildString {
                                append(text.substring(0, start))
                                append("    ")
                                append(text.substring(end))
                            }
                            val newCursor = start + 4
                            codeInput = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursor)
                            )
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = LocalContentColor.current),
                singleLine = false,
            )

            Divider()

            val scrollState = rememberScrollState()
            LaunchedEffect(outputText) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                Text(
                    text = outputText,
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentProcess?.destroyForcibly()
            cleanupTempFiles()
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    Window(
        onCloseRequest = {
            val tempDir = File(TEMP_DIR_NAME)
            if (tempDir.exists()) {
                println("Cleaning up temporary files on exit...")
                tempDir.deleteRecursively()
            }
            exitApplication()
        },
        title = "Simple Kotlin/Java IDE",
        state = windowState
    ) {
        Ide()
    }
}