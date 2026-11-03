package com.liangji.bodymeasure.ui.settings

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.liangji.bodymeasure.R

internal enum class RewardQrSaveResult {
    SAVED,
    UNSUPPORTED,
    FAILED,
}

internal fun saveRewardQr(context: Context): RewardQrSaveResult =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveRewardQrToMediaStore(context)
    } else {
        RewardQrSaveResult.UNSUPPORTED
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun saveRewardQrToMediaStore(context: Context): RewardQrSaveResult {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "身语赞赏二维码.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/身语",
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = runCatching {
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull() ?: return RewardQrSaveResult.FAILED

    return runCatching {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.wechat_reward)
            ?: error("无法读取赞赏二维码")
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: error("无法写入相册文件")
        bitmap.recycle()
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null,
        )
        RewardQrSaveResult.SAVED
    }.getOrElse {
        runCatching { resolver.delete(uri, null, null) }
        RewardQrSaveResult.FAILED
    }
}
