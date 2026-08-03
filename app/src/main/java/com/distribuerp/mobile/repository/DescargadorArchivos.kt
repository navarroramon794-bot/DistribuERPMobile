package com.distribuerp.mobile.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

object DescargadorArchivos {

    fun guardarEnDescargas(
        contexto: Context,
        cuerpo: ResponseBody,
        nombreArchivo: String
    ): Uri {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            guardarEnMediaStore(
                contexto,
                cuerpo,
                nombreArchivo
            )
        } else {
            guardarEnCarpetaPublica(
                contexto,
                cuerpo,
                nombreArchivo
            )
        }
    }

    private fun guardarEnMediaStore(
        contexto: Context,
        cuerpo: ResponseBody,
        nombreArchivo: String
    ): Uri {
        val resolver = contexto.contentResolver

        val valores = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                nombreArchivo
            )

            put(
                MediaStore.MediaColumns.MIME_TYPE,
                cuerpo.contentType()?.toString()
                    ?: "application/octet-stream"
            )

            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            valores
        ) ?: throw IllegalStateException(
            "No se pudo crear el archivo en Descargas"
        )

        try {
            resolver.openOutputStream(uri)?.use {
                salida ->

                cuerpo.byteStream().use { entrada ->
                    entrada.copyTo(salida)
                }
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        return uri
    }

    private fun guardarEnCarpetaPublica(
        contexto: Context,
        cuerpo: ResponseBody,
        nombreArchivo: String
    ): Uri {
        val carpeta = Environment
            .getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }

        val archivo = File(
            carpeta,
            nombreArchivo
        )

        cuerpo.byteStream().use { entrada ->
            FileOutputStream(archivo).use { salida ->
                entrada.copyTo(salida)
            }
        }

        return FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            archivo
        )
    }
}
