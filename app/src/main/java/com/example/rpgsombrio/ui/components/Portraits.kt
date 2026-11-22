package com.example.rpgsombrio.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PORTRAIT_MAX_PX = 512

/** Decodifica a imagem já reduzida (sem carregar o original inteiro na memória). */
private fun decodeScaled(context: Context, uri: Uri, maxPx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

/** Copia o retrato escolhido para o armazenamento do app, reduzido; o URI da galeria deixa de ser necessário. */
internal fun importPortrait(context: Context, source: Uri): String? {
    val bitmap = decodeScaled(context, source, PORTRAIT_MAX_PX) ?: return null
    val dir = File(context.filesDir, "portraits").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

/** Remove o arquivo do retrato somente se ele foi criado pelo app (nunca arquivos da galeria). */
internal fun deletePortraitFile(context: Context, uriString: String) {
    if (uriString.isBlank()) return
    runCatching {
        val path = Uri.parse(uriString).takeIf { it.scheme == "file" }?.path ?: return
        val file = File(path)
        if (file.parentFile == File(context.filesDir, "portraits")) file.delete()
    }
}

@Composable
internal fun Portrait(uriString: String, name: String, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uriString) {
        value = withContext(Dispatchers.IO) { decodeScaled(context, Uri.parse(uriString), 256) }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "Retrato de $name",
            modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    }
}
