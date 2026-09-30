package com.example.texttopdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.MetricAffectingSpan
import android.text.style.UnderlineSpan
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF2676E8)
private val Navy = Color(0xFF10295B)

private data class FontChoice(val name: String, val family: FontFamily, val typeface: Int, val group: String)
private val fonts = listOf(
    FontChoice("Roboto", FontFamily.SansSerif, Typeface.SANS_SERIF, "Documents"),
    FontChoice("Times New Roman", FontFamily.Serif, Typeface.SERIF, "Documents"),
    FontChoice("Georgia", FontFamily.Serif, Typeface.SERIF, "Documents"),
    FontChoice("Open Sans", FontFamily.SansSerif, Typeface.SANS_SERIF, "Documents"),
    FontChoice("Lato", FontFamily.SansSerif, Typeface.SANS_SERIF, "Documents"),
    FontChoice("JetBrains Mono", FontFamily.Monospace, Typeface.MONOSPACE, "Coding"),
    FontChoice("Fira Code", FontFamily.Monospace, Typeface.MONOSPACE, "Coding"),
    FontChoice("Courier New", FontFamily.Monospace, Typeface.MONOSPACE, "Coding"),
    FontChoice("Source Code Pro", FontFamily.Monospace, Typeface.MONOSPACE, "Coding"),
    FontChoice("Pacifico", FontFamily.Cursive, Typeface.SANS_SERIF, "Stylish"),
    FontChoice("Playfair Display", FontFamily.Serif, Typeface.SERIF, "Stylish"),
    FontChoice("Dancing Script", FontFamily.Cursive, Typeface.SANS_SERIF, "Stylish")
)

enum class Screen { HOME, EDITOR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    var value by remember { mutableStateOf(loadDraft(context)) }
    var font by remember { mutableStateOf(fonts.first()) }
    var size by remember { mutableStateOf(18) }
    var color by remember { mutableStateOf(Color(0xFF111827)) }
    var highlight by remember { mutableStateOf(Color.Transparent) }
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underline by remember { mutableStateOf(false) }
    var strike by remember { mutableStateOf(false) }
    var applyAll by remember { mutableStateOf(false) }
    var align by remember { mutableStateOf(TextAlign.Left) }
    var listMode by remember { mutableStateOf(0) }
    var dark by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var snack by remember { mutableStateOf(false) }

    LaunchedEffect(value.text) { saveDraft(context, value.text) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Box(Modifier.fillMaxSize()) {
            if (screen == Screen.HOME) {
                HomeScreen(onCreate = { screen = Screen.EDITOR }, onSettings = { dark = !dark })
            } else {
                EditorScreen(
                    value = value,
                    onValue = { value = it },
                    font = font,
                    onFont = { font = it },
                    size = size,
                    onSize = { size = it.coerceIn(8, 72) },
                    color = color,
                    highlight = highlight,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    strike = strike,
                    applyAll = applyAll,
                    align = align,
                    listMode = listMode,
                    onBack = { screen = Screen.HOME },
                    onDownload = { nameDialog = true },
                    onPreview = { preview = true },
                    onApplyAll = { applyAll = it },
                    onStyle = { kind ->
                        when (kind) { 1 -> bold = !bold; 2 -> italic = !italic; 3 -> underline = !underline; 4 -> strike = !strike }
                        value = applySpan(value, font, size, color, highlight, bold, italic, underline, strike, applyAll)
                    },
                    onColor = { color = it; value = applySpan(value, font, size, it, highlight, bold, italic, underline, strike, applyAll) },
                    onHighlight = { highlight = it; value = applySpan(value, font, size, color, it, bold, italic, underline, strike, applyAll) },
                    onAlign = { align = it; value = applyParagraph(value, it, applyAll) },
                    onList = { listMode = it; value = insertList(value, it) }
                )
            }
            if (nameDialog) {
                FileNameDialog(onDismiss = { nameDialog = false }) { name ->
                    nameDialog = false
                    savedUri = exportPdf(context, value.annotatedString, name)
                    snack = true
                }
            }
            if (preview) PreviewDialog(value.annotatedString) { preview = false }
            if (snack) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); snack = false }
                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp), shape = MaterialTheme.shapes.large, color = Color(0xFF252525)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("PDF saved", color = Color.White)
                        savedUri?.let { uri ->
                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) }) { Text("Open") }
                            TextButton(onClick = { share(context, uri) }) { Text("Share") }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun HomeScreen(onCreate: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Blue).padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(20.dp))
            Text("Text to PDF Maker", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(58.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 58.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(150.dp).background(Color(0xFFF0F4FA), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Column { repeat(4) { Box(Modifier.padding(vertical = 6.dp).width(if (it == 3) 55.dp else 100.dp).height(10.dp).background(Blue, MaterialTheme.shapes.small)) } }
            }
            Icon(Icons.Default.ArrowForward, null, tint = Blue, modifier = Modifier.size(56.dp).padding(horizontal = 8.dp))
            Box(Modifier.size(150.dp).background(Color(0xFFEF3038), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) { Text("PDF", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(46.dp))
        Text("Text to PDF", color = Navy, fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Maker", color = Blue, fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(18.dp))
        Text("Turn your text into a PDF file\nquickly and easily.", color = Color(0xFF71809B), fontSize = 21.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(52.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp).height(82.dp), shape = MaterialTheme.shapes.large, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(18.dp)); Text("Create PDF", fontSize = 27.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(34.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 50.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HomeCard("My PDFs", Icons.Default.Description, Color(0xFFEAF3FF), Blue, onCreate, Modifier.weight(1f))
            HomeCard("Settings", Icons.Default.Settings, Color(0xFFEAFBF6), Color(0xFF0A9F89), onSettings, Modifier.weight(1f))
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable private fun HomeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, tint: Color, click: () -> Unit, modifier: Modifier) {
    Surface(onClick = click, modifier = modifier.height(180.dp), shape = MaterialTheme.shapes.large, color = bg) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(54.dp)); Spacer(Modifier.height(16.dp)); Text(title, color = Navy, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Icon(Icons.Default.ArrowForward, null, tint = tint)
        }
    }
}

@Composable private fun EditorScreen(value: TextFieldValue, onValue: (TextFieldValue) -> Unit, font: FontChoice, onFont: (FontChoice) -> Unit, size: Int, onSize: (Int) -> Unit, color: Color, highlight: Color, bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean, applyAll: Boolean, align: TextAlign, listMode: Int, onBack: () -> Unit, onDownload: () -> Unit, onPreview: () -> Unit, onApplyAll: (Boolean) -> Unit, onStyle: (Int) -> Unit, onColor: (Color) -> Unit, onHighlight: (Color) -> Unit, onAlign: (TextAlign) -> Unit, onList: (Int) -> Unit) {
    var fontMenu by remember { mutableStateOf(false) }
    var colorMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Blue).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Text to PDF", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onPreview) { Text("Preview", color = Color.White) }
            Button(onClick = onDownload, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Blue)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(5.dp)); Text("Download PDF") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { fontMenu = true }) { Text(font.name, fontFamily = font.family); Icon(Icons.Default.ArrowDropDown, null) }
                DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                    fonts.groupBy { it.group }.forEach { (group, groupFonts) ->
                        Text(group, color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                        groupFonts.forEach { f -> DropdownMenuItem(text = { Text(f.name, fontFamily = f.family) }, onClick = { onFont(f); fontMenu = false }) }
                    }
                }
            }
            IconButton(onClick = { onSize(size - 1) }) { Icon(Icons.Default.Remove, null) }
            Text(size.toString(), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { onSize(size + 1) }) { Icon(Icons.Default.Add, null) }
            StyleButton(Icons.Default.FormatBold, bold) { onStyle(1) }
            StyleButton(Icons.Default.FormatItalic, italic) { onStyle(2) }
            StyleButton(Icons.Default.FormatUnderlined, underline) { onStyle(3) }
            StyleButton(Icons.Default.StrikethroughS, strike) { onStyle(4) }
            Box {
                IconButton(onClick = { colorMenu = true }) { Icon(Icons.Default.FormatColorText, null, tint = color) }
                DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                    listOf(Color.Black, Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFF8F00), Color(0xFF8E24AA)).forEach { c ->
                        DropdownMenuItem(text = { Box(Modifier.size(25.dp).background(c, MaterialTheme.shapes.small)) }, onClick = { onColor(c); colorMenu = false })
                    }
                }
            }
            IconButton(onClick = { onHighlight(Color(0xFFFFF59D)) }) { Icon(Icons.Default.BorderColor, null, tint = Color(0xFFFFB300)) }
            listOf(TextAlign.Left, TextAlign.Center, TextAlign.Right, TextAlign.Justify).forEach { a ->
                IconButton(onClick = { onAlign(a) }) { Icon(if (a == TextAlign.Left) Icons.Default.FormatAlignLeft else if (a == TextAlign.Center) Icons.Default.FormatAlignCenter else if (a == TextAlign.Right) Icons.Default.FormatAlignRight else Icons.Default.FormatAlignJustify, null, tint = if (align == a) Blue else LocalContentColor.current) }
            }
            StyleButton(Icons.Default.FormatListBulleted, listMode == 1) { onList(1) }
            StyleButton(Icons.Default.FormatListNumbered, listMode == 2) { onList(2) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("All", fontSize = 12.sp); Switch(checked = applyAll, onCheckedChange = onApplyAll) }
            IconButton(onClick = {}) { Icon(Icons.Default.Undo, null) }
            IconButton(onClick = {}) { Icon(Icons.Default.Redo, null) }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            val words = value.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
            Text("$words words  •  ${value.text.length} characters", color = Color(0xFF71809B), fontSize = 13.sp)
            Spacer(Modifier.weight(1f)); Text("A4 • 20 mm margins", color = Color(0xFF71809B), fontSize = 13.sp)
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp).border(1.dp, Color(0xFFE4E8EF), MaterialTheme.shapes.small).padding(16.dp)) {
            BasicTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxSize(), textStyle = TextStyle(fontSize = size.sp, fontFamily = font.family, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, textDecoration = if (underline && strike) TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)) else if (underline) TextDecoration.Underline else if (strike) TextDecoration.LineThrough else TextDecoration.None, textAlign = align, lineHeight = (size + 7).sp), decorationBox = { inner -> if (value.text.isEmpty()) Text("Start typing or paste your text here…", color = Color(0xFF9AA6BA), fontSize = size.sp); inner() })
        }
    }
}

@Composable private fun StyleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, click: () -> Unit) { IconButton(onClick = click, modifier = Modifier.clip(MaterialTheme.shapes.small).background(if (active) Blue.copy(alpha = .12f) else Color.Transparent)) { Icon(icon, null, tint = if (active) Blue else LocalContentColor.current) } }

private fun rangeFor(v: TextFieldValue, all: Boolean): IntRange { val s = if (all) 0 else v.selection.min; val e = if (all) v.text.length else v.selection.max; return s until e }

private fun applySpan(v: TextFieldValue, font: FontChoice, size: Int, color: Color, highlight: Color, bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean, all: Boolean): TextFieldValue {
    val range = rangeFor(v, all); if (range.isEmpty()) return v
    val b = AnnotatedString.Builder(v.annotatedString)
    b.addStyle(SpanStyle(fontFamily = font.family, fontSize = size.sp, color = color, background = highlight, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, textDecoration = if (underline && strike) TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)) else if (underline) TextDecoration.Underline else if (strike) TextDecoration.LineThrough else TextDecoration.None), range.first, range.last + 1)
    return v.copy(annotatedString = b.toAnnotatedString())
}

private fun applyParagraph(v: TextFieldValue, align: TextAlign, all: Boolean): TextFieldValue {
    val range = rangeFor(v, all); if (range.isEmpty()) return v
    val b = AnnotatedString.Builder(v.annotatedString); b.addStyle(ParagraphStyle(textAlign = align), range.first, range.last + 1); return v.copy(annotatedString = b.toAnnotatedString())
}

private fun insertList(v: TextFieldValue, mode: Int): TextFieldValue {
    val cursor = v.selection.min; val start = v.text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }; val prefix = if (mode == 1) "• " else "1. "; val text = v.text.substring(0, start) + prefix + v.text.substring(start); return TextFieldValue(text, TextRange(cursor + prefix.length))
}

private fun loadDraft(context: Context): TextFieldValue = TextFieldValue(context.getSharedPreferences("draft", Context.MODE_PRIVATE).getString("text", "") ?: "")
private fun saveDraft(context: Context, text: String) { context.getSharedPreferences("draft", Context.MODE_PRIVATE).edit().putString("text", text).apply() }

private class TypefaceSpanCompat(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) { tp.typeface = typeface }
    override fun updateMeasureState(tp: TextPaint) { tp.typeface = typeface }
}

private fun exportPdf(context: Context, source: AnnotatedString, name: String): Uri? {
    val safe = name.trim().ifEmpty { "TextToPDF" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val pdf = PdfDocument(); val w = 595; val h = 842; val margin = 57; val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG); paint.textSize = 18f
    val sp = SpannableString(source.text)
    source.spanStyles.forEach { r ->
        val s = r.item; val typeface = when (s.fontFamily) { FontFamily.Serif -> Typeface.SERIF; FontFamily.Monospace -> Typeface.MONOSPACE; else -> Typeface.SANS_SERIF }; var style = Typeface.NORMAL; if (s.fontWeight == FontWeight.Bold) style = style or Typeface.BOLD; if (s.fontStyle == FontStyle.Italic) style = style or Typeface.ITALIC
        sp.setSpan(TypefaceSpanCompat(typeface), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(StyleSpan(style), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(AbsoluteSizeSpan((s.fontSize.value.takeIf { it > 0 } ?: 18f).toInt(), true), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(ForegroundColorSpan(s.color.toArgb()), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); if (s.textDecoration.contains(TextDecoration.Underline)) sp.setSpan(UnderlineSpan(), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); if (s.textDecoration.contains(TextDecoration.LineThrough)) sp.setSpan(StrikethroughSpan(), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); if (s.background != Color.Transparent) sp.setSpan(BackgroundColorSpan(s.background.toArgb()), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    source.paragraphStyles.forEach { r -> val a = when (r.item.textAlign) { TextAlign.Center -> Layout.Alignment.ALIGN_CENTER; TextAlign.Right -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_NORMAL }; sp.setSpan(AlignmentSpan.Standard(a), r.start, r.end.coerceAtMost(sp.length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    var pos = 0; val contentHeight = h - margin * 2
    while (pos < sp.length || pos == 0) {
        val remaining = sp.subSequence(pos, sp.length); val layout = StaticLayout.Builder.obtain(remaining, 0, remaining.length, paint, w - margin * 2).setIncludePad(true).build(); val maxLines = (contentHeight / 24).coerceAtLeast(1); val takeLines = maxLines.coerceAtMost(layout.lineCount); val end = if (takeLines >= layout.lineCount) remaining.length else layout.getLineEnd(takeLines - 1)
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, pdf.pages.size + 1).create()); page.canvas.drawColor(Color.White.toArgb()); page.canvas.save(); page.canvas.translate(margin.toFloat(), margin.toFloat()); StaticLayout.Builder.obtain(SpannableString(remaining.subSequence(0, end)), 0, end, paint, w - margin * 2).setIncludePad(true).build().draw(page.canvas); page.canvas.restore(); pdf.finishPage(page); if (end <= 0) break; pos += end
    }
    val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, "$safe.pdf"); put(MediaStore.Downloads.MIME_TYPE, "application/pdf"); put(MediaStore.Downloads.RELATIVE_PATH, "Download") }; val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values); if (uri != null) context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }; pdf.close(); return uri
}

@Composable private fun FileNameDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) { var name by remember { mutableStateOf("TextToPDF_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("PDF file name") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("File name") }) }, confirmButton = { Button(onClick = { onSave(name) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

@Composable private fun PreviewDialog(source: AnnotatedString, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("PDF Preview") }, text = { Box(Modifier.heightIn(min = 300.dp, max = 520.dp).verticalScroll(rememberScrollState())) { Text(source.text, color = Color.Black, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }) }

private fun share(context: Context, uri: Uri) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share PDF")) }
