package com.pavlov.MyShadowGallery.data.repository

import android.content.Context
import android.net.Uri
import com.pavlov.MyShadowGallery.data.utils.ImageUriHelper
import com.pavlov.MyShadowGallery.util.NamingStyleManager
import com.pavlov.MyShadowGallery.util.APK
import com.pavlov.MyShadowGallery.util.APK.RECEIVED_FROM_OUTSIDE
import com.pavlov.MyShadowGallery.util.APK.TEMP_IMAGES
import com.pavlov.MyShadowGallery.util.APK.UPLOADED_BY_ME
import com.pavlov.MyShadowGallery.util.APKM
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val imageUriHelper: ImageUriHelper
) {

    private val TAG = "ImageRepository"
    private val apkManager = APKM(context)

    private val _receivedFromOutside = MutableStateFlow<List<String>>(emptyList())
    val receivedFromOutside: StateFlow<List<String>> = _receivedFromOutside

    private val _tempImages = MutableStateFlow<List<String>>(emptyList())
    val tempImages: StateFlow<List<String>> = _tempImages

    private val _uploadedByMe = MutableStateFlow<List<String>>(emptyList())
    val uploadedByMe: StateFlow<List<String>> = _uploadedByMe

    suspend fun loadImages(directoryName: String, stateFlow: MutableStateFlow<List<String>>) {
        try {
            val directory = File(context.filesDir, directoryName)
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (created) {
                    Timber.d("Директория $directoryName создана.")
                } else {
                    Timber.e("Не удалось создать директорию $directoryName.")
                }
            } else {
                Timber.d("Директория $directoryName уже существует.")
            }

            val files = directory.listFiles()?.mapNotNull { it.name } ?: emptyList()
            stateFlow.value = files
            Timber.d("Загружено ${files.size} элементов из $directoryName.")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при загрузке файлов из $directoryName.")
        }
    }

    suspend fun loadAllImages() {
        loadImages(TEMP_IMAGES, _tempImages)
        loadImages(RECEIVED_FROM_OUTSIDE, _receivedFromOutside)
        loadImages(UPLOADED_BY_ME, _uploadedByMe)
    }


/** ловлю на добавлении пикчи: FileNotFoundException или open failed: ENOENT, полагаю contentResolver.openInputStream(uri) вызывается в тот момент,
 * когда файл ещё не готов или камера не успела завершить работу с ним. Делаю несколько повторных попыток открыть InputStream с ретрай.
 * */

suspend fun addImage(uri: Uri, directoryName: String): Uri? {
    try {
        val dir = File(context.filesDir, directoryName)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Timber.e("Не удалось создать директорию $directoryName")
                return null
            }
        }

        val fileName = getFileName(directoryName)
        val file = File(dir, fileName)

        var tries = 0
        val maxTries = 3
        var inputStream: InputStream? = null

        while (tries < maxTries) {
            tries++
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    break
                } else {
                    Timber.e("Не удалось открыть InputStream для URI: $uri, попытка $tries")
                }
            } catch (e: FileNotFoundException) {
                Timber.e(e, "FileNotFoundException при открытии URI: $uri, попытка $tries")
            }

            if (tries < maxTries) {
                kotlinx.coroutines.delay(400)
            }
        }

        if (inputStream == null) {
            Timber.e("Не удалось открыть InputStream для URI: $uri после $tries попыток.")
            return null
        }

        inputStream.use { ist ->
            file.outputStream().use { ost ->
                ist.copyTo(ost)
            }
        }

        Timber.d("Изображение сохранено: ${file.absolutePath}")
        loadAllImages()
        return getFileUri(fileName)
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при добавлении изображения: $uri")
        return null
    }
}

    suspend fun deleteImage(uri: Uri) {
        try {
            val file = uriToFile(uri)
            if (file != null && file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Timber.d("Файл удалён: ${file.absolutePath}")
                    loadAllImages()
                } else {
                    Timber.e("Не удалось удалить файл: ${file.absolutePath}")
                }
            } else {
                Timber.e("Файл не найден для удаления: $uri")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении файла: $uri")
        }
    }

    fun getFileUri(fileName: String): Uri? {
        return imageUriHelper.getFileUri(fileName)
    }

    fun uriToFile(uri: Uri): File? {
        return imageUriHelper.uriToFile(uri)
    }

    suspend fun clearTempImages() {
        try {
            val tempDir = File(context.filesDir, TEMP_IMAGES)
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    try {
                        if (file.delete()) {
                            Timber.d("Временное изображение удалено: ${file.absolutePath}")
                        } else {
                            Timber.e("Не удалось удалить временное изображение: ${file.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при удалении файла: ${file.absolutePath}")
                    }
                }
            }
            _tempImages.value = emptyList()
            Timber.d("Все временные изображения очищены")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при очистке временных изображений")
        }
    }

    fun getPhotoDate(fileName: String): String {
        Timber.d("Получение даты для файла: $fileName")
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

        // UPLOADED_BY_ME
        var directory = File(context.filesDir, UPLOADED_BY_ME)
        var file = File(directory, fileName)
        if (file.exists()) {
            val date = Date(file.lastModified())
            val formattedDate = dateFormat.format(date)
            Timber.d("Дата файла $fileName: $formattedDate")
            return formattedDate
        }

        // TEMP_IMAGES
        directory = File(context.filesDir, TEMP_IMAGES)
        file = File(directory, fileName)
        if (file.exists()) {
            val date = Date(file.lastModified())
            val formattedDate = dateFormat.format(date)
            Timber.d("Дата файла $fileName в $TEMP_IMAGES: $formattedDate")
            return formattedDate
        }

        // RECEIVED_FROM_OUTSIDE
        directory = File(context.filesDir, RECEIVED_FROM_OUTSIDE)
        file = File(directory, fileName)
        if (file.exists()) {
            val date = Date(file.lastModified())
            val formattedDate = dateFormat.format(date)
            Timber.d("Дата файла $fileName в $RECEIVED_FROM_OUTSIDE: $formattedDate")
            return formattedDate
        }

        Timber.e("Файл не найден для получения даты: ${file.absolutePath}")
        return "Неизвестно"
    }

    fun getFileName(directoryName: String): String {
        val folder = File(context.filesDir, directoryName)
        val isEncrypted = apkManager.getBoolean(APK.KEY_USE_THE_ENCRYPTION_K, false)
        return NamingStyleManager(context).generateFileName(isEncrypted, folder).let { name ->
            if (!name.contains(".")) {
                "$name.png" // если нет расширения
            } else {
                name
            }
        }
    }

    fun getFileNameWithoutExtension(fileName: String): String {
        val name = fileName.substringBeforeLast('.')
        Timber.d("Получено имя файла без расширения: $name из $fileName")
        return name
    }
}
