package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.fmtQty
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.todayLabel
import com.example.model.AccountBookEntry
import com.example.model.HomeDebt
import com.example.model.SewingOrder
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfExportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = (PAGE_WIDTH - MARGIN * 2).toInt()
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 48f

    private val COLOR_EMERALD = Color.parseColor("#047857")
    private val COLOR_VIOLET = Color.parseColor("#6D28D9")
    private val COLOR_ROSE = Color.parseColor("#BE123C")
    private val COLOR_AMBER = Color.parseColor("#B45309")
    private val COLOR_DARK = Color.parseColor("#111827")
    private val COLOR_MUTED = Color.parseColor("#6B7280")
    private val COLOR_BORDER = Color.parseColor("#E5E7EB")
    private val COLOR_CARD_BG = Color.parseColor("#F9FAFB")
    private val COLOR_EMERALD_SOFT = Color.parseColor("#ECFDF5")
    private val COLOR_ROSE_SOFT = Color.parseColor("#FFF1F2")
    private val COLOR_VIOLET_SOFT = Color.parseColor("#F5F3FF")

    private class PdfWriter(private val context: Context) {
        val doc = PdfDocument()
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
            private set
        var y: Float = MARGIN

        private val baseTypeface: Typeface =
            runCatching { ResourcesCompat.getFont(context, R.font.cairo) }.getOrNull()
                ?: Typeface.DEFAULT

        private val boldTypeface: Typeface =
            Typeface.create(baseTypeface, Typeface.BOLD)

        fun startPage(subHeader: String? = null) {
            finishCurrentPage()
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = doc.startPage(info)
            page = newPage
            canvas = newPage.canvas
            canvas.drawColor(Color.WHITE)
            y = MARGIN
            if (pageNumber > 1 && !subHeader.isNullOrBlank()) {
                drawRtlText(subHeader, size = 10f, color = COLOR_MUTED, bold = true)
                y += 4f
                drawDivider()
            }
        }

        fun ensureSpace(neededHeight: Float, subHeader: String? = null) {
            if (y + neededHeight > BOTTOM_LIMIT) {
                startPage(subHeader)
            }
        }

        private fun finishCurrentPage() {
            page?.let { current ->
                // Page footer
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = baseTypeface
                    textSize = 9f
                    color = COLOR_MUTED
                }
                val footerLayout = StaticLayout.Builder
                    .obtain("صندوقي · إدارة مالية ذكية — صفحة $pageNumber", 0, "صندوقي · إدارة مالية ذكية — صفحة $pageNumber".length, footerPaint, CONTENT_WIDTH)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setTextDirection(TextDirectionHeuristics.RTL)
                    .build()
                current.canvas.save()
                current.canvas.translate(MARGIN, PAGE_HEIGHT - 28f)
                footerLayout.draw(current.canvas)
                current.canvas.restore()
                doc.finishPage(current)
            }
            page = null
        }

        fun makeTextPaint(size: Float, color: Int, bold: Boolean = false): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = if (bold) boldTypeface else baseTypeface
                textSize = size
                this.color = color
            }

        fun measureRtlLayout(
            text: String,
            width: Int = CONTENT_WIDTH,
            size: Float = 11.5f,
            color: Int = COLOR_DARK,
            bold: Boolean = false,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        ): StaticLayout {
            val paint = makeTextPaint(size, color, bold)
            return StaticLayout.Builder
                .obtain(text, 0, text.length, paint, max(1, width))
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(2f, 1.08f)
                .build()
        }

        fun drawHeaderBanner(
            title: String,
            subtitle: String,
            badge: String,
            accentColor: Int,
        ) {
            val bannerHeight = 84f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            val rect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + bannerHeight)
            canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

            val titleLayout = measureRtlLayout(
                title,
                width = CONTENT_WIDTH - 130,
                size = 18f,
                color = Color.WHITE,
                bold = true,
            )
            val subLayout = measureRtlLayout(
                subtitle,
                width = CONTENT_WIDTH - 130,
                size = 10.5f,
                color = Color.argb(230, 255, 255, 255),
                bold = false,
            )

            // RTL title placed on right side inside banner
            canvas.save()
            canvas.translate(MARGIN + 114f, y + 16f)
            titleLayout.draw(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(MARGIN + 114f, y + 16f + titleLayout.height + 4f)
            subLayout.draw(canvas)
            canvas.restore()

            // Badge pill on left side of banner
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(46, 255, 255, 255)
            }
            val pillRect = RectF(MARGIN + 12f, y + 24f, MARGIN + 104f, y + 58f)
            canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)
            val badgeLayout = measureRtlLayout(
                badge,
                width = 88,
                size = 10.5f,
                color = Color.WHITE,
                bold = true,
                align = Layout.Alignment.ALIGN_CENTER,
            )
            canvas.save()
            canvas.translate(MARGIN + 14f, y + 32f)
            badgeLayout.draw(canvas)
            canvas.restore()

            y += bannerHeight + 14f
        }

        fun drawRtlText(
            text: String,
            size: Float = 11.5f,
            color: Int = COLOR_DARK,
            bold: Boolean = false,
            xOffset: Float = MARGIN,
            width: Int = CONTENT_WIDTH,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            advance: Boolean = true,
        ): Int {
            val layout = measureRtlLayout(text, width, size, color, bold, align)
            canvas.save()
            canvas.translate(xOffset, y)
            layout.draw(canvas)
            canvas.restore()
            if (advance) {
                y += layout.height + 4f
            }
            return layout.height
        }

        fun drawSectionTitle(title: String, accentColor: Int = COLOR_EMERALD) {
            ensureSpace(34f)
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            canvas.drawRoundRect(
                RectF(PAGE_WIDTH - MARGIN - 4f, y + 3f, PAGE_WIDTH - MARGIN, y + 19f),
                2f,
                2f,
                barPaint,
            )
            val layout = measureRtlLayout(title, width = CONTENT_WIDTH - 12, size = 13f, color = COLOR_DARK, bold = true)
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 8f
        }

        fun drawInfoBox(rows: List<Pair<String, String>>, bgColor: Int = COLOR_CARD_BG, borderColor: Int = COLOR_BORDER) {
            if (rows.isEmpty()) return
            val measured = rows.map { (lbl, value) ->
                val lblL = measureRtlLayout(lbl, width = (CONTENT_WIDTH * 0.40f).toInt(), size = 10.5f, color = COLOR_MUTED, bold = true)
                val valL = measureRtlLayout(value, width = (CONTENT_WIDTH * 0.56f).toInt(), size = 11f, color = COLOR_DARK, bold = true, align = Layout.Alignment.ALIGN_OPPOSITE)
                Triple(lblL, valL, max(lblL.height, valL.height) + 8f)
            }
            val totalHeight = measured.sumOf { it.third.toDouble() }.toFloat() + 14f
            ensureSpace(totalHeight + 8f)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val boxRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + totalHeight)
            canvas.drawRoundRect(boxRect, 12f, 12f, bgPaint)
            canvas.drawRoundRect(boxRect, 12f, 12f, strokePaint)

            var curY = y + 10f
            measured.forEachIndexed { idx, (lblL, valL, rowH) ->
                // Label on right (RTL)
                canvas.save()
                canvas.translate(MARGIN + CONTENT_WIDTH * 0.58f, curY)
                lblL.draw(canvas)
                canvas.restore()

                // Value on left
                canvas.save()
                canvas.translate(MARGIN + 12f, curY)
                valL.draw(canvas)
                canvas.restore()

                curY += rowH
                if (idx < measured.lastIndex) {
                    val divPaint = Paint().apply {
                        color = borderColor
                        strokeWidth = 0.6f
                    }
                    canvas.drawLine(MARGIN + 12f, curY - 4f, PAGE_WIDTH - MARGIN - 12f, curY - 4f, divPaint)
                }
            }
            y += totalHeight + 12f
        }

        fun drawKpiRow(cards: List<Triple<String, String, Pair<Int, Int>>>) {
            if (cards.isEmpty()) return
            val gap = 10f
            val cardW = (CONTENT_WIDTH - gap * (cards.size - 1)) / cards.size
            val cardH = 58f
            ensureSpace(cardH + 10f)

            cards.forEachIndexed { idx, (label, value, colors) ->
                val (bgCol, valCol) = colors
                val rightX = PAGE_WIDTH - MARGIN - idx * (cardW + gap)
                val leftX = rightX - cardW
                val rect = RectF(leftX, y, rightX, y + cardH)
                canvas.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol })
                canvas.drawRoundRect(
                    rect,
                    10f,
                    10f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = COLOR_BORDER
                        style = Paint.Style.STROKE
                        strokeWidth = 0.9f
                    },
                )
                val lblL = measureRtlLayout(label, width = (cardW - 14f).toInt(), size = 9.5f, color = COLOR_MUTED, bold = true, align = Layout.Alignment.ALIGN_CENTER)
                val valL = measureRtlLayout(value, width = (cardW - 14f).toInt(), size = 12.5f, color = valCol, bold = true, align = Layout.Alignment.ALIGN_CENTER)
                canvas.save()
                canvas.translate(leftX + 7f, y + 9f)
                lblL.draw(canvas)
                canvas.restore()

                canvas.save()
                canvas.translate(leftX + 7f, y + 11f + lblL.height + 3f)
                valL.draw(canvas)
                canvas.restore()
            }
            y += cardH + 12f
        }

        fun drawTable(
            headers: List<String>,
            colWeights: List<Float>,
            rows: List<List<String>>,
            accentColor: Int = COLOR_EMERALD,
            subHeader: String? = null,
        ) {
            val colWidths = colWeights.map { (CONTENT_WIDTH * it).toInt() }
            fun drawHeaderRow() {
                ensureSpace(32f, subHeader)
                val rect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f)
                canvas.drawRoundRect(rect, 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor })
                var rightCursor = PAGE_WIDTH - MARGIN - 6f
                headers.forEachIndexed { i, h ->
                    val w = colWidths[i]
                    val leftX = rightCursor - w
                    val l = measureRtlLayout(
                        h,
                        width = max(1, w - 8),
                        size = 10f,
                        color = Color.WHITE,
                        bold = true,
                        align = if (i == 0) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER,
                    )
                    canvas.save()
                    canvas.translate(leftX + 4f, y + 5f)
                    l.draw(canvas)
                    canvas.restore()
                    rightCursor = leftX
                }
                y += 28f
            }

            drawHeaderRow()
            rows.forEachIndexed { rIdx, row ->
                val layouts = row.mapIndexed { cIdx, cell ->
                    measureRtlLayout(
                        cell,
                        width = max(1, colWidths[cIdx] - 8),
                        size = 10f,
                        color = COLOR_DARK,
                        bold = false,
                        align = if (cIdx == 0) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER,
                    )
                }
                val rowH = (layouts.maxOfOrNull { it.height } ?: 14) + 12f
                if (y + rowH > BOTTOM_LIMIT) {
                    startPage(subHeader)
                    drawHeaderRow()
                }
                if (rIdx % 2 == 1) {
                    canvas.drawRect(
                        RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH),
                        Paint().apply { color = COLOR_CARD_BG },
                    )
                }
                var rightCursor = PAGE_WIDTH - MARGIN - 6f
                layouts.forEachIndexed { i, l ->
                    val w = colWidths[i]
                    val leftX = rightCursor - w
                    canvas.save()
                    canvas.translate(leftX + 4f, y + 6f)
                    l.draw(canvas)
                    canvas.restore()
                    rightCursor = leftX
                }
                y += rowH
                canvas.drawLine(
                    MARGIN,
                    y,
                    PAGE_WIDTH - MARGIN,
                    y,
                    Paint().apply {
                        color = COLOR_BORDER
                        strokeWidth = 0.6f
                    },
                )
            }
            y += 10f
        }

        fun drawDivider() {
            canvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                Paint().apply {
                    color = COLOR_BORDER
                    strokeWidth = 0.8f
                },
            )
            y += 8f
        }

        fun writeToFile(fileName: String): File {
            finishCurrentPage()
            val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                doc.writeTo(fos)
            }
            doc.close()
            return outFile
        }
    }

    fun generateOrderReceiptPdf(context: Context, order: SewingOrder): File {
        val writer = PdfWriter(context)
        val rawRem = order.price - order.paid
        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
        val subHeader = "وصل طلبية خياطة #${order.id} — ${order.client}"

        writer.startPage(subHeader)
        writer.drawHeaderBanner(
            title = "وصل طلبية خياطة",
            subtitle = "الرقم #${order.id} · تاريخ الطلب: ${order.date.ifBlank { todayLabel() }}",
            badge = order.stage,
            accentColor = COLOR_VIOLET,
        )

        writer.drawKpiRow(
            listOf(
                Triple("السعر الإجمالي", fmt(order.price), COLOR_VIOLET_SOFT to COLOR_VIOLET),
                Triple("المدفوع", fmt(order.paid), COLOR_EMERALD_SOFT to COLOR_EMERALD),
                Triple(
                    if (rem <= 0) "الحالة" else "المتبقي",
                    if (rem <= 0) "خالص الدفع ✓" else fmt(rem),
                    (if (rem <= 0) COLOR_EMERALD_SOFT else COLOR_ROSE_SOFT) to (if (rem <= 0) COLOR_EMERALD else COLOR_ROSE),
                ),
            )
        )

        writer.drawSectionTitle("بيانات الزبونة والطلبية", COLOR_VIOLET)
        val meterRateVal = order.meterRate ?: 0.0
        val flatLaborVal = order.flatLabor ?: 0.0
        val adjVal = order.adjustment ?: 0.0
        val clientRows = buildList {
            add("اسم الزبونة" to order.client)
            if (!order.phone.isNullOrBlank()) add("رقم الهاتف" to order.phone)
            add("الموديل / نوع اللباس" to order.model)
            if (!order.fittingDate.isNullOrBlank()) add("موعد التجربة" to order.fittingDate)
            if (!order.dueDate.isNullOrBlank()) add("موعد التسليم" to order.dueDate)
            if (order.meters > 0) add("إجمالي الأمتار" to "${fmtQty(order.meters)} م × ${fmt(meterRateVal)}")
        }
        writer.drawInfoBox(clientRows)

        val breakdownRows = buildList {
            val labor = if (order.meters > 0) FinanceUtils.round2(order.meters * meterRateVal) else flatLaborVal
            if (labor > 0) {
                add(listOf("أتعاب التفصيل والخياطة", if (order.meters > 0) "${fmtQty(order.meters)} م" else "مقطوعة", fmt(labor)))
            }
            order.fabrics?.forEach { f ->
                val desc = "${f.name} (${if (f.source == "customer") "قماش الزبونة" else "قماش الورشة"})"
                add(listOf(desc, "${fmtQty(f.meters)} م", if (f.charge > 0) fmt(f.charge) else "من الزبونة"))
            }
            order.supplies?.forEach { s ->
                val qtyTxt = if (s.qty != null && !s.unit.isNullOrBlank()) "${fmtQty(s.qty)} ${s.unit}" else "—"
                add(listOf("لوازم: ${s.name}", qtyTxt, fmt(s.cost)))
            }
            if (adjVal != 0.0) {
                add(listOf("تعديل / خصم أو إضافة", "—", fmt(adjVal)))
            }
        }
        if (breakdownRows.isNotEmpty()) {
            writer.drawSectionTitle("تفاصيل التسعيرة والمواد", COLOR_VIOLET)
            writer.drawTable(
                headers = listOf("البند", "الكمية", "القيمة"),
                colWeights = listOf(0.54f, 0.20f, 0.26f),
                rows = breakdownRows,
                accentColor = COLOR_VIOLET,
                subHeader = subHeader,
            )
        }

        order.measurements?.let { m ->
            val measPairs = listOfNotNull(
                m.len?.takeIf { it.isNotBlank() }?.let { "الطول" to it },
                m.sh?.takeIf { it.isNotBlank() }?.let { "الكتف" to it },
                m.chest?.takeIf { it.isNotBlank() }?.let { "الصدر" to it },
                m.waist?.takeIf { it.isNotBlank() }?.let { "الخصر" to it },
                m.hip?.takeIf { it.isNotBlank() }?.let { "الأرداف" to it },
                m.sleeve?.takeIf { it.isNotBlank() }?.let { "الكم" to it },
                m.note?.takeIf { it.isNotBlank() }?.let { "ملاحظة المقاس" to it },
            )
            if (measPairs.isNotEmpty()) {
                writer.drawSectionTitle("المقاسات المعتمدة", COLOR_VIOLET)
                writer.drawInfoBox(measPairs)
            }
        }

        if (!order.notes.isNullOrBlank()) {
            writer.drawSectionTitle("ملاحظات إضافية", COLOR_VIOLET)
            writer.drawInfoBox(listOf("ملاحظة" to order.notes))
        }

        writer.ensureSpace(40f)
        writer.drawRtlText(
            "شكراً لثقتكم — تم إصدار الوصل بتاريخ ${todayLabel()}",
            size = 10f,
            color = COLOR_MUTED,
            align = Layout.Alignment.ALIGN_CENTER,
        )

        return writer.writeToFile("order_receipt_${order.id}.pdf")
    }

    fun generateMonthlyWorkshopReportPdf(
        context: Context,
        ym: String,
        orders: List<SewingOrder>,
        cashIn: Double,
        cashOut: Double,
        cashNet: Double,
    ): File {
        val writer = PdfWriter(context)
        val monthLabel = formatYM(ym)
        val subHeader = "تقرير ورشة الخياطة — $monthLabel"
        val sales = FinanceUtils.round2(orders.sumOf { it.price })
        val remaining = FinanceUtils.round2(orders.sumOf { max(0.0, it.price - it.paid) })

        writer.startPage(subHeader)
        writer.drawHeaderBanner(
            title = "التقرير الشهري لورشة الخياطة",
            subtitle = "$monthLabel · تاريخ الإصدار: ${todayLabel()}",
            badge = "${orders.size} طلبية",
            accentColor = COLOR_VIOLET,
        )

        writer.drawKpiRow(
            listOf(
                Triple("مبيعات الطلبيات", fmt(sales), COLOR_VIOLET_SOFT to COLOR_VIOLET),
                Triple("المحصّل بالصندوق", fmt(cashIn), COLOR_EMERALD_SOFT to COLOR_EMERALD),
                Triple("مصاريف الصندوق", fmt(cashOut), COLOR_ROSE_SOFT to COLOR_ROSE),
            )
        )
        writer.drawKpiRow(
            listOf(
                Triple(
                    "صافي الصندوق الفعلي",
                    fmt(cashNet),
                    (if (cashNet >= 0) COLOR_EMERALD_SOFT else COLOR_ROSE_SOFT) to (if (cashNet >= 0) COLOR_EMERALD else COLOR_ROSE),
                ),
                Triple("متبقي عند الزبونات", fmt(remaining), COLOR_CARD_BG to COLOR_AMBER),
            )
        )

        writer.drawSectionTitle("سجل طلبيات الشهر", COLOR_VIOLET)
        if (orders.isEmpty()) {
            writer.drawRtlText("لا توجد طلبيات مسجلة في هذا الشهر.", color = COLOR_MUTED)
        } else {
            val tableRows = orders.map { o ->
                val rem = FinanceUtils.round2(max(0.0, o.price - o.paid))
                listOf(
                    "${o.client} — ${o.model}",
                    o.stage,
                    fmt(o.price),
                    fmt(o.paid),
                    if (rem <= 0) "خالص" else fmt(rem),
                )
            }
            writer.drawTable(
                headers = listOf("الزبونة والموديل", "المرحلة", "السعر", "المدفوع", "المتبقي"),
                colWeights = listOf(0.36f, 0.16f, 0.16f, 0.16f, 0.16f),
                rows = tableRows,
                accentColor = COLOR_VIOLET,
                subHeader = subHeader,
            )
        }

        return writer.writeToFile("workshop_report_$ym.pdf")
    }

    fun generateDebtStatementPdf(
        context: Context,
        creditor: String,
        direction: String,
        activity: String,
        items: List<HomeDebt>,
    ): File {
        val writer = PdfWriter(context)
        val isIn = direction == "in"
        val accent = if (isIn) COLOR_EMERALD else COLOR_ROSE
        val total = FinanceUtils.round2(items.sumOf { it.amount })
        val paid = FinanceUtils.round2(items.sumOf { it.paid })
        val remaining = FinanceUtils.round2(items.sumOf { debtRemaining(it) })
        val subHeader = "كشف حساب ${if (isIn) "مستحقات" else "ديون"} — $creditor"

        writer.startPage(subHeader)
        writer.drawHeaderBanner(
            title = if (isIn) "كشف مستحقات (لي)" else "كشف حساب دين (عليّ)",
            subtitle = "الطرف: $creditor · النشاط: $activity · ${todayLabel()}",
            badge = if (remaining <= 0) "خالص ✓" else "${items.size} سجلات",
            accentColor = accent,
        )

        writer.drawKpiRow(
            listOf(
                Triple("الإجمالي", fmt(total), COLOR_CARD_BG to COLOR_DARK),
                Triple(if (isIn) "المحصّل" else "المسدّد", fmt(paid), COLOR_EMERALD_SOFT to COLOR_EMERALD),
                Triple("المتبقي", fmt(remaining), (if (remaining <= 0) COLOR_EMERALD_SOFT else COLOR_ROSE_SOFT) to (if (remaining <= 0) COLOR_EMERALD else COLOR_ROSE)),
            )
        )

        writer.drawSectionTitle("تفاصيل المعاملات والدفعات", accent)
        val rows = items.map { d ->
            val rem = debtRemaining(d)
            listOf(
                d.item.ifBlank { d.activity },
                d.date,
                fmt(d.amount),
                fmt(d.paid),
                if (rem <= 0) "خالص ✓" else fmt(rem),
            )
        }
        writer.drawTable(
            headers = listOf("البيان / الغرض", "التاريخ", "المبلغ", "المدفوع", "المتبقي"),
            colWeights = listOf(0.36f, 0.16f, 0.16f, 0.16f, 0.16f),
            rows = rows,
            accentColor = accent,
            subHeader = subHeader,
        )

        val allPayments = items.flatMap { d ->
            d.payments.map { p -> listOf(d.item, p.date, fmt(p.amount)) }
        }
        if (allPayments.isNotEmpty()) {
            writer.drawSectionTitle("سجل الدفعات المستلمة / المسددة", accent)
            writer.drawTable(
                headers = listOf("البند", "تاريخ الدفعة", "المبلغ"),
                colWeights = listOf(0.50f, 0.25f, 0.25f),
                rows = allPayments,
                accentColor = accent,
                subHeader = subHeader,
            )
        }

        val safeName = creditor.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").take(24).ifBlank { "party" }
        return writer.writeToFile("debt_statement_$safeName.pdf")
    }

    fun generateAccountBookPdf(context: Context, entry: AccountBookEntry): File {
        val writer = PdfWriter(context)
        val isCustomer = entry.kind == "customer"
        val bal = FinanceUtils.round2(entry.entries.sumOf { it.delta })
        val accent = if (isCustomer) COLOR_EMERALD else COLOR_AMBER
        val subHeader = "كشف حساب — ${entry.name}"

        writer.startPage(subHeader)
        writer.drawHeaderBanner(
            title = if (isCustomer) "كشف حساب زبون" else "كشف حساب مورد / محل",
            subtitle = "${entry.name}${if (!entry.phone.isNullOrBlank()) " · ${entry.phone}" else ""} · ${todayLabel()}",
            badge = if (bal == 0.0) "مصفّر ✓" else "${entry.entries.size} حركة",
            accentColor = accent,
        )

        val infoRows = buildList {
            add("الاسم" to entry.name)
            if (!entry.phone.isNullOrBlank()) add("الهاتف" to entry.phone)
            if (!entry.note.isNullOrBlank()) add("ملاحظة" to entry.note)
            add("الرصيد النهائي" to fmt(bal))
        }
        writer.drawInfoBox(infoRows)

        writer.drawSectionTitle("سجل الحركات", accent)
        if (entry.entries.isEmpty()) {
            writer.drawRtlText("لا توجد حركات مسجلة في هذا الحساب بعد.", color = COLOR_MUTED)
        } else {
            // Compute running balance chronologically (entries are newest-first, so compute from oldest)
            val chronological = entry.entries.reversed()
            var running = 0.0
            val rows = chronological.map { item ->
                running = FinanceUtils.round2(running + item.delta)
                val sign = if (item.delta >= 0) "+" else "−"
                listOf(
                    item.note.ifBlank { if (item.delta >= 0) "إضافة" else "تسديد" },
                    item.date,
                    "$sign ${fmt(kotlin.math.abs(item.delta))}",
                    fmt(running),
                )
            }.reversed()

            writer.drawTable(
                headers = listOf("البيان", "التاريخ", "الحركة", "الرصيد"),
                colWeights = listOf(0.42f, 0.18f, 0.20f, 0.20f),
                rows = rows,
                accentColor = accent,
                subHeader = subHeader,
            )
        }

        val safeName = entry.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").take(24).ifBlank { "account" }
        return writer.writeToFile("account_statement_$safeName.pdf")
    }

    fun sharePdf(context: Context, file: File, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openPdf(context: Context, file: File, onFallbackShare: () -> Unit = { sharePdf(context, file, "مشاركة PDF") }) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(viewIntent, "عرض / طباعة PDF"))
        }.onFailure {
            onFallbackShare()
        }
    }
}
