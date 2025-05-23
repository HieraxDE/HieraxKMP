import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle


class IDETheme(
    keywords: SpanStyle,
    identifiers: SpanStyle,
    comments: SpanStyle,
    literals: SpanStyle,
    strings: SpanStyle,
    numbers: SpanStyle,
    scopes: SpanStyle,
    types: SpanStyle,
    functions: SpanStyle,
    variables: SpanStyle,
    constants: SpanStyle,
    background: Color,
    foreground: Color,
    error: SpanStyle
) {
    var keywords = keywords
    var identifiers = identifiers
    var comments = comments
    var literals = literals
    var strings = strings
    var numbers = numbers
    var scopes = scopes
    var types = types
    var functions = functions
    var variables = variables
    var constants = constants
    var background = background
    var foreground = foreground
    var error = error
}