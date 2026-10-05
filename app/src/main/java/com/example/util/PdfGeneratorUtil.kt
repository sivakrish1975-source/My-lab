package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfGeneratorUtil {

    private const val TAG = "PdfGeneratorUtil"
    private const val PAGE_WIDTH = 595 // Standard A4 width in postscript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 40f

    /**
     * Generates a multi-page authentic Sri Lankan A/L Exam PDF file and saves it in app storage.
     */
    fun createExamPdf(
        context: Context,
        fileName: String,
        title: String,
        subject: String,
        year: Int,
        paperType: String,
        content: String,
        isMarkingScheme: Boolean = false
    ): File {
        val dir = File(context.filesDir, "downloaded_papers")
        if (!dir.exists()) dir.mkdirs()

        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val finalName = if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
        val targetFile = File(dir, finalName)

        val document = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerBoxPaint = Paint().apply {
            color = if (isMarkingScheme) Color.rgb(220, 240, 230) else Color.rgb(235, 240, 250)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val lines = content.lines()
        val linesPerPage = 42
        val totalPages = ((lines.size + linesPerPage - 1) / linesPerPage).coerceAtLeast(1)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Ensure solid white background to prevent anti-aliasing transparency gaps on older Android versions
            canvas.drawColor(Color.WHITE)

            var currentY = MARGIN

            // Header Banner
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 70f, headerBoxPaint)
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 70f, borderPaint)

            val deptTitle = "DEPARTMENT OF EXAMINATIONS, SRI LANKA"
            canvas.drawText(deptTitle, MARGIN + 12f, currentY + 20f, titlePaint)

            val examTitle = if (isMarkingScheme) {
                "G.C.E. (ADVANCED LEVEL) EXAMINATION - $year • OFFICIAL MARKING SCHEME"
            } else {
                "GENERAL CERTIFICATE OF EDUCATION (ADVANCED LEVEL) EXAMINATION - $year"
            }
            canvas.drawText(examTitle, MARGIN + 12f, currentY + 38f, subTitlePaint)

            val specInfo = "Subject: $subject | Type: $paperType | Medium: English | Time: 3 Hours"
            canvas.drawText(specInfo, MARGIN + 12f, currentY + 56f, bodyPaint)

            currentY += 85f

            // Index Number Box on page 1
            if (pageIndex == 0) {
                val indexLabel = "Index No:"
                canvas.drawText(indexLabel, MARGIN + 10f, currentY + 14f, subTitlePaint)
                var boxX = MARGIN + 80f
                for (b in 0 until 8) {
                    canvas.drawRect(boxX, currentY, boxX + 20f, currentY + 20f, borderPaint)
                    boxX += 24f
                }
                currentY += 35f
            }

            // Body Lines for this page
            val startLine = pageIndex * linesPerPage
            val endLine = (startLine + linesPerPage).coerceAtMost(lines.size)

            for (i in startLine until endLine) {
                val line = lines[i]
                // Wrap text if too wide
                val maxChars = 75
                if (line.length > maxChars) {
                    val part1 = line.substring(0, maxChars)
                    val part2 = "    " + line.substring(maxChars)
                    canvas.drawText(part1, MARGIN + 10f, currentY, bodyPaint)
                    currentY += 13f
                    canvas.drawText(part2, MARGIN + 10f, currentY, bodyPaint)
                } else {
                    canvas.drawText(line, MARGIN + 10f, currentY, bodyPaint)
                }
                currentY += 14f
            }

            // Footer
            val footerY = PAGE_HEIGHT - 25f
            canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, borderPaint)
            val footerText = "Page ${pageIndex + 1} of $totalPages  •  Confidential - Sri Lanka Examination Syndicate"
            canvas.drawText(footerText, MARGIN + 10f, footerY, subTitlePaint)

            document.finishPage(page)
        }

        try {
            FileOutputStream(targetFile).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF: ${e.message}", e)
        } finally {
            document.close()
        }

        return targetFile
    }

    /**
     * Checks if a file is a valid PDF by inspecting its header bytes (%PDF-).
     */
    fun isValidPdf(file: File): Boolean {
        if (!file.exists() || file.length() < 10) return false
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(5)
                val read = input.read(header)
                read == 5 && header[0] == '%'.code.toByte() &&
                        header[1] == 'P'.code.toByte() &&
                        header[2] == 'D'.code.toByte() &&
                        header[3] == 'F'.code.toByte() &&
                        header[4] == '-'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }
}
