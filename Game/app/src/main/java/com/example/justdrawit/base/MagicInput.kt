package com.example.justdrawit.base

import android.gesture.Gesture
import android.gesture.GesturePoint
import android.gesture.GestureStroke
import android.gesture.Prediction
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.util.Log
import android.util.Xml
import com.example.justdrawit.Test
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MagicInput(
    private val gctx: GameContext,
    private val gestureManager: GestureManager,
    private val test: Test,
    private val onGestureRecognized: (String) -> Unit
) : IGameObject {
    // 저장할 제스처 이름 변수 (여기서 수정 가능)
    private var saveGestureName = "triangle"
    private var isRecordingMode = false

    private val rectSize = 400f
    private val padding = 50f
    private val touchRect = RectF()

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#F5F5DC") // Beige
        style = Paint.Style.FILL
        alpha = 200
    }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#5D4037") // Brown
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    private val strokePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val previewPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val recordButtonPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val saveButtonPaint = Paint().apply {
        color = Color.parseColor("#4CAF50") // Green
        style = Paint.Style.FILL
    }
    private val recordButtonRect = RectF()
    private val saveButtonRect = RectF()
    private val textBoxRect = RectF()

    private var lastResultText = "Gesture: None"
    private var lastResultScore = "Score: 0.0"
    private var isDrawing = false
    private var isSavingFeedback = false
    private var savingFeedbackTimer = 0f

    private val currentPoints = mutableListOf<GesturePoint>()
    private val lastPoints = mutableListOf<GesturePoint>() // 페이드 아웃용 저장소
    private var fadeAlpha = 0
    private val fadeSpeed = 500f // 초당 감소할 알파값 (255면 약 0.5초)
    private var touchId = -1

    init {
        val right = gctx.metrics.width - padding
        val bottom = gctx.metrics.height - padding
        touchRect.set(right - rectSize, bottom - rectSize, right, bottom)

        // 텍스트 상자 위치 (입력창 바로 위)
        val textBoxHeight = 100f
        textBoxRect.set(touchRect.left, touchRect.top - textBoxHeight - 10f, touchRect.right, touchRect.top - 10f)

        // 버튼 위치 (텍스트 상자 위)
        val btnWidth = 150f
        val btnHeight = 80f
        recordButtonRect.set(textBoxRect.left, textBoxRect.top - btnHeight - 10f, textBoxRect.left + btnWidth, textBoxRect.top - 10f)
        saveButtonRect.set(recordButtonRect.right + 20f, recordButtonRect.top, recordButtonRect.right + 20f + btnWidth, recordButtonRect.bottom)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)

        // 현재 터치된 위치 (가상 좌표)
        val rawX = event.getX(actionIndex)
        val rawY = event.getY(actionIndex)
        val pt = gctx.metrics.fromScreen(rawX, rawY)
        val tx = pt.x
        val ty = pt.y

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // 새 입력 시작 시 기존 페이드 아웃 중인 선은 즉시 제거
                fadeAlpha = 0
                lastPoints.clear()

                // 버튼 클릭 체크 (magicInputMode가 true일 때만 작동)
                if (test.magicInputMode) {
                    if (recordButtonRect.contains(tx, ty)) {
                        isRecordingMode = !isRecordingMode
                        Log.d("MagicInput", "Recording Mode Toggle: $isRecordingMode")
                        return true
                    }
                    if (saveButtonRect.contains(tx, ty)) {
                        saveLibraryToFile()
                        return true
                    }
                }

                // 입력창 터치 체크
                if (touchId == -1 && touchRect.contains(tx, ty)) {
                    touchId = pointerId
                    isDrawing = true
                    currentPoints.clear()
                    // 상대 좌표로 저장 (사각형 왼쪽 위가 0,0)
                    currentPoints.add(GesturePoint(tx - touchRect.left, ty - touchRect.top, System.currentTimeMillis()))
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchId != -1) {
                    // 추적 중인 손가락의 인덱스를 찾음
                    val index = event.findPointerIndex(touchId)
                    if (index != -1) {
                        val movePt = gctx.metrics.fromScreen(event.getX(index), event.getY(index))
                        // 상대 좌표로 저장
                        currentPoints.add(GesturePoint(movePt.x - touchRect.left, movePt.y - touchRect.top, System.currentTimeMillis()))
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (pointerId == touchId) {
                    recognizeGesture()
                    
                    // 그린 선을 페이드용으로 복사
                    lastPoints.clear()
                    lastPoints.addAll(currentPoints)
                    fadeAlpha = 255
                    
                    touchId = -1
                    isDrawing = false
                    currentPoints.clear()
                    return true
                }
            }
        }
        return false
    }

    private fun recognizeGesture() {
        if (currentPoints.size < 5) {
            lastResultText = "Too Short"
            lastResultScore = "Points: ${currentPoints.size}"
            return
        }

        val gesture = Gesture()
        gesture.addStroke(GestureStroke(ArrayList(currentPoints)))
        
        val library = gestureManager.getLibrary()
        if (library == null) {
            lastResultText = "Library Error"
            lastResultScore = "Not Loaded"
            return
        }

        // 먼저 인식을 시도하여 결과 저장
        val predictions = library.recognize(gesture)
        if (predictions != null && predictions.isNotEmpty()) {
            val best = predictions[0] as Prediction
            lastResultText = "Gesture: ${best.name}"
            lastResultScore = "Score: ${String.format(Locale.US, "%.2f", best.score)}"
            
            // 인식 성공 시 마법 발동 (REC 모드가 아닐 때만)
            if (!isRecordingMode && best.score > 1.0) {
                onGestureRecognized(best.name)
            }
        } else {
            lastResultText = "Gesture: Not Found"
            lastResultScore = "Score: 0.0"
        }

        // REC 모드라면 인식 결과와 상관없이 라이브러리에 추가
        if (isRecordingMode) {
            library.addGesture(saveGestureName, gesture)
            // 상단 텍스트 앞에 [REC] 표시를 붙여 녹화 중임을 알림
            lastResultText = "[REC] $saveGestureName"
            lastResultScore = "Count: ${currentPoints.size} (Points)"
            
            // XML은 기록용으로 즉시 저장
            saveGestureAsXml(saveGestureName, gesture)
        }
    }

    private fun saveLibraryToFile() {
        val library = gestureManager.getLibrary() ?: return
        
        isSavingFeedback = true
        savingFeedbackTimer = 0.5f // 0.5초 동안 표시

        if (library.save()) {
            lastResultText = "SUCCESS!"
            lastResultScore = "All Data Saved"
            Log.d("MagicInput", "Gesture library saved to internal storage.")
        } else {
            lastResultText = "SAVE FAILED"
            lastResultScore = "Error Occurred"
        }
    }

    private fun saveGestureAsXml(name: String, gesture: Gesture) {
        try {
            val baos = ByteArrayOutputStream()
            val serializer = Xml.newSerializer()
            
            serializer.setOutput(baos, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            
            serializer.startTag("", "gesture")
            serializer.attribute("", "name", name)
            
            for ((sIndex, stroke) in gesture.strokes.withIndex()) {
                serializer.startTag("", "stroke")
                serializer.attribute("", "index", sIndex.toString())
                
                val points = stroke.points
                for (i in 0 until points.size / 2) {
                    serializer.startTag("", "point")
                    serializer.attribute("", "x", points[i * 2].toString())
                    serializer.attribute("", "y", points[i * 2 + 1].toString())
                    serializer.endTag("", "point")
                }
                serializer.endTag("", "stroke")
            }
            
            serializer.endTag("", "gesture")
            serializer.endDocument()
            
            val xmlContent = baos.toString("UTF-8")
            
            // 1. 파일로 저장
            val file = File(gctx.view.context.filesDir, "gesture_$name.xml")
            FileOutputStream(file).use { it.write(xmlContent.toByteArray()) }
            
            // 2. 로그로 전체 내용 출력 (추출용)
            Log.i("GestureXML", "\n--- START GESTURE XML: $name ---\n$xmlContent\n--- END GESTURE XML ---")
            Log.d("MagicInput", "XML format saved and logged: ${file.absolutePath}")
            
        } catch (e: Exception) {
            Log.e("MagicInput", "Failed to save/log XML", e)
        }
    }

    override fun update(gctx: GameContext) {
        if (isSavingFeedback) {
            savingFeedbackTimer -= gctx.frameTime
            if (savingFeedbackTimer <= 0) {
                isSavingFeedback = false
            }
        }
        
        // 페이드 아웃 업데이트
        if (fadeAlpha > 0) {
            fadeAlpha = (fadeAlpha - fadeSpeed * gctx.frameTime).toInt().coerceAtLeast(0)
            if (fadeAlpha == 0) {
                lastPoints.clear()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // 배경 및 테두리
        canvas.drawRect(touchRect, bgPaint)
        canvas.drawRect(touchRect, borderPaint)

        // magicInputMode가 true일 때만 버튼 그리기
        if (test.magicInputMode) {
            // REC 버튼 그리기
            recordButtonPaint.color = if (isRecordingMode) Color.RED else Color.DKGRAY
            canvas.drawRect(recordButtonRect, recordButtonPaint)
            canvas.drawRect(recordButtonRect, borderPaint)
            val recText = if (isRecordingMode) "REC" else "OFF"
            canvas.drawText(recText, recordButtonRect.centerX(), recordButtonRect.centerY() + 15f, textPaint)

            // SAVE 버튼 그리기
            canvas.drawRect(saveButtonRect, saveButtonPaint)
            canvas.drawRect(saveButtonRect, borderPaint)
            canvas.drawText("SAVE", saveButtonRect.centerX(), saveButtonRect.centerY() + 15f, textPaint)
        }

        // 제스처 결과 박스 그리기 (gestureCheck가 true일 때만)
        if (test.gestureCheck) {
            // 상단 텍스트 표시 박스 (베이지색 반투명)
            canvas.drawRect(textBoxRect, bgPaint)
            canvas.drawRect(textBoxRect, borderPaint)

            // 텍스트 출력
            if (isSavingFeedback) {
                canvas.drawText("Saving...", textBoxRect.centerX(), textBoxRect.centerY() + 15f, textPaint)
            } else if (isDrawing) {
                canvas.drawText("Drawing...", textBoxRect.centerX(), textBoxRect.centerY() + 15f, textPaint)
            } else {
                canvas.drawText(lastResultText, textBoxRect.centerX(), textBoxRect.top + 40f, textPaint)
                canvas.drawText(lastResultScore, textBoxRect.centerX(), textBoxRect.top + 85f, textPaint)
                
                // 저장된 제스처 샘플 미리보기 그리기 (텍스트 박스 안쪽 오른쪽 상단에 작게)
                drawGesturePreview(canvas, textBoxRect)
            }
        }

        // 현재 그리고 있는 선 시각화 (사각형 내부로 제한)
        if (currentPoints.size > 1) {
            drawPoints(canvas, currentPoints, 255)
        }
        
        // 다 그린 후 점점 투명해지는 선 시각화
        if (fadeAlpha > 0 && lastPoints.size > 1) {
            drawPoints(canvas, lastPoints, fadeAlpha)
        }
    }

    private fun drawPoints(canvas: Canvas, points: List<GesturePoint>, alpha: Int) {
        val saveCount = canvas.save()
        canvas.clipRect(touchRect)
        
        strokePaint.alpha = alpha
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            canvas.drawLine(
                p1.x + touchRect.left, p1.y + touchRect.top,
                p2.x + touchRect.left, p2.y + touchRect.top,
                strokePaint
            )
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawGesturePreview(canvas: Canvas, textBoxRect: RectF) {
        val library = gestureManager.getLibrary() ?: return
        val gestures = library.getGestures(saveGestureName) ?: return
        if (gestures.isEmpty()) {
            // 샘플이 없을 경우 로그 출력 (디버깅용)
            return
        }

        // 가장 최근에 등록된 샘플 표시 (마지막 인덱스)
        val gesture = gestures.last()
        val strokes = gesture.strokes
        if (strokes.isEmpty()) return

        // 미리보기 박스 설정 (텍스트 박스 왼쪽 영역)
        val previewSize = 80f
        val previewMargin = 10f
        val px = textBoxRect.left + previewMargin
        val py = textBoxRect.top + previewMargin
        
        // 제스처의 경계 사각형 구하기
        val bounds = gesture.boundingBox
        val bWidth = maxOf(bounds.width(), 1f)
        val bHeight = maxOf(bounds.height(), 1f)
        val scale = (previewSize - 20f) / maxOf(bWidth, bHeight)

        val saveCount = canvas.save()
        // 미리보기 위치로 이동
        canvas.translate(px, py)
        // 박스 중앙에 맞추기 위한 추가 이동
        canvas.translate((previewSize - bWidth * scale) / 2f, (previewSize - bHeight * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-bounds.left, -bounds.top)

        for (stroke in strokes) {
            val points = stroke.points
            for (i in 0 until points.size / 2 - 1) {
                canvas.drawLine(
                    points[i * 2], points[i * 2 + 1],
                    points[(i + 1) * 2], points[(i + 1) * 2 + 1],
                    previewPaint
                )
            }
        }
        canvas.restoreToCount(saveCount)
    }
}
