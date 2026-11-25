package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.model.SandooqiDB
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class SandooqiRepository(
    private val dao: SandooqiDao,
    private val appContext: Context? = null,
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val dbAdapter = moshi.adapter(SandooqiDB::class.java).indent("  ")

    val dbFlow: Flow<SandooqiDB> = dao.observeState().map { entity ->
        if (entity == null) {
            FinanceUtils.createDefaultDB()
        } else {
            runCatching {
                val parsed = dbAdapter.fromJson(entity.json)
                FinanceUtils.normalizeSandooqiDB(parsed)
            }.getOrElse {
                FinanceUtils.createDefaultDB()
            }
        }
    }

    suspend fun ensureInitialized(): SandooqiDB = withContext(Dispatchers.IO) {
        val existing = dao.getStateOnce()
        if (existing == null) {
            val initial = FinanceUtils.createDefaultDB()
            dao.upsertState(SandooqiStateEntity(id = 1, json = dbAdapter.toJson(initial)))
            initial
        } else {
            val parsed = runCatching { dbAdapter.fromJson(existing.json) }.getOrNull()
            val normalized = FinanceUtils.normalizeSandooqiDB(parsed)
            if (parsed != normalized) {
                dao.upsertState(SandooqiStateEntity(id = 1, json = dbAdapter.toJson(normalized)))
            }
            normalized
        }
    }

    suspend fun saveDB(db: SandooqiDB) = withContext(Dispatchers.IO) {
        dao.upsertState(
            SandooqiStateEntity(
                id = 1,
                json = dbAdapter.toJson(db),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun exportJson(db: SandooqiDB): String = dbAdapter.toJson(db)

    fun parseImportedJson(json: String): SandooqiDB? {
        return runCatching {
            val parsed = dbAdapter.fromJson(json) ?: return null
            FinanceUtils.normalizeSandooqiDB(parsed)
        }.getOrNull()
    }

    suspend fun compressUriToDataUrl(uri: Uri, maxDim: Int = 960, quality: Int = 72): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                appContext?.contentResolver?.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                    val w = original.width
                    val h = original.height
                    val scaled = if (w > maxDim || h > maxDim) {
                        val (nw, nh) = if (w > h) {
                            maxDim to ((h.toFloat() * maxDim) / w).roundToInt().coerceAtLeast(1)
                        } else {
                            ((w.toFloat() * maxDim) / h).roundToInt().coerceAtLeast(1) to maxDim
                        }
                        Bitmap.createScaledBitmap(original, nw, nh, true).also {
                            if (it != original) original.recycle()
                        }
                    } else {
                        original
                    }
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    scaled.recycle()
                    val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    "data:image/jpeg;base64,$b64"
                }
            }.getOrNull()
        }

    fun decodeDataUrlBitmap(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return runCatching {
            val b64 = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun buildCsvReport(db: SandooqiDB, month: String): String {
        val report = FinanceUtils.computeCashReportForMonth(db, month)
        val rows = buildList {
            add(listOf("التاريخ", "الحساب", "النوع", "البند", "البيان", "المبلغ"))
            for (t in report.arr) {
                val accLabel = FinanceUtils.ACCOUNT_LABELS[FinanceUtils.transactionAccount(t)] ?: "الحساب العائلي"
                val typeLabel = if (t.type == "in") "وارد" else "صادر"
                val safeName = "\"${t.name.replace("\"", "\"\"")}\""
                add(listOf(t.dateISO.ifBlank { t.date }, accLabel, typeLabel, t.cat, safeName, t.amount.toString()))
            }
        }
        return "\uFEFF" + rows.joinToString("\n") { it.joinToString(",") }
    }
}
