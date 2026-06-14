package com.example.justdrawit

import android.gesture.Gesture
import android.gesture.GesturePoint
import android.gesture.GestureStroke
import android.gesture.Prediction
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.util.Log
import android.util.Xml
import com.example.justdrawit.base.GestureManager
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Locale

class GestureTestScene(gctx: GameContext) : Scene(gctx) {
    private val gestureManager = GestureManager(gctx.view.context)
    
    private var isRecordingMode = false
    private var selectedSpellName: String? = null
    
    private val rectSize = 500f
    private val touchRect = RectF()
    private val textBoxRect = RectF()
    private val recordButtonRect = RectF()
    private val saveButtonRect = RectF()
    private val exitButtonRect = RectF()
    
    private val spellButtons = listOf(
        SpellButton("arrowRight", RectF()),
        SpellButton("circle", RectF()),
        SpellButton("triangle", RectF())
    )
    
    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val inputBgPaint = Paint().apply { color = Color.parseColor("#F5F5DC"); alpha = 200 }
    private val borderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER }
    private val strokePaint = Paint().apply { color = Color.BLUE; style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true }
    private val iconPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true }
    
    private var lastResultText = "Gesture: None"
    private var lastResultScore = "Score: 0.0"
    private var isDrawing = false
    private val currentPoints = mutableListOf<GesturePoint>()
    private var touchId = -1

    init {
        val cx = gctx.metrics.width / 2
        val cy = gctx.metrics.height / 2
        
        // 입력창 (중앙)
        touchRect.set(cx - rectSize/2, cy - 100f, cx + rectSize/2, cy + rectSize - 100f)
        
        // 결과창 (입력창 위)
        textBoxRect.set(touchRect.left, touchRect.top - 120f, touchRect.right, touchRect.top - 20f)
        
        // 버튼 배치 (상단)
        val btnW = 180f
        val btnH = 80f
        recordButtonRect.set(cx - btnW - 10f, 150f, cx - 10f, 150f + btnH)
        saveButtonRect.set(cx + 10f, 150f, cx + btnW + 10f, 150f + btnH)
        exitButtonRect.set(gctx.metrics.width - 150f, 50f, gctx.metrics.width - 30f, 130f)
        
        // 스펠 버튼 배치 (입력창 왼쪽)
        var startY = touchRect.top
        for (btn in spellButtons) {
            btn.rect.set(touchRect.left - 150f, startY, touchRect.left - 30f, startY + 120f)
            startY += 150f
        }
    }

    private class SpellButton(val name: String, val rect: RectF)

    override fun update(gctx: GameContext) {}

    override fun draw(canvas: Canvas) {
        canvas.drawRect(gctx.metrics.borderRect, bgPaint)
        
        // Exit 버튼
        canvas.drawRect(exitButtonRect, borderPaint)
        canvas.drawText("Back", exitButtonRect.centerX(), exitButtonRect.centerY() + 15f, textPaint)

        // REC, SAVE 버튼
        val recColor = if (isRecordingMode) Color.RED else Color.DKGRAY
        drawStyledButton(canvas, recordButtonRect, if (isRecordingMode) "REC ON" else "REC OFF", recColor)
        drawStyledButton(canvas, saveButtonRect, "SAVE FILE", Color.parseColor("#4CAF50"))

        // 스펠 UI
        for (btn in spellButtons) {
            val isSelected = selectedSpellName == btn.name
            val bgColor = if (isSelected) Color.BLUE else Color.BLACK
            canvas.drawRect(btn.rect, Paint().apply { color = bgColor })
            canvas.drawRect(btn.rect, borderPaint)
            
            // 아이콘 그리기
            val iconRect = RectF(btn.rect.left + 20f, btn.rect.top + 20f, btn.rect.right - 20f, btn.rect.bottom - 20f)
            when (btn.name) {
                "arrowRight" -> drawArrowIcon(canvas, iconRect)
                "circle" -> canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), iconRect.width()/2, iconPaint)
                "triangle" -> drawTriangleIcon(canvas, iconRect)
            }
        }

        // 결과 박스
        canvas.drawRect(textBoxRect, Paint().apply { color = Color.DKGRAY })
        canvas.drawRect(textBoxRect, borderPaint)
        if (isDrawing) {
            canvas.drawText("Drawing...", textBoxRect.centerX(), textBoxRect.centerY() + 15f, textPaint)
        } else {
            canvas.drawText(lastResultText, textBoxRect.centerX(), textBoxRect.top + 45f, textPaint)
            canvas.drawText(lastResultScore, textBoxRect.centerX(), textBoxRect.top + 95f, textPaint)
        }

        // 입력창
        canvas.drawRect(touchRect, inputBgPaint)
        canvas.drawRect(touchRect, borderPaint)
        
        // 그리기 선
        if (currentPoints.size > 1) {
            val saveCount = canvas.save()
            canvas.clipRect(touchRect)
            for (i in 0 until currentPoints.size - 1) {
                val p1 = currentPoints[i]
                val p2 = currentPoints[i+1]
                canvas.drawLine(p1.x + touchRect.left, p1.y + touchRect.top, p2.x + touchRect.left, p2.y + touchRect.top, strokePaint)
            }
            canvas.restoreToCount(saveCount)
        }
    }

    private fun drawStyledButton(canvas: Canvas, rect: RectF, text: String, color: Int) {
        canvas.drawRect(rect, Paint().apply { this.color = color })
        canvas.drawRect(rect, borderPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 15f, textPaint)
    }

    private fun drawArrowIcon(canvas: Canvas, rect: RectF) {
        val path = Path()
        path.moveTo(rect.left + 10f, rect.top)
        path.lineTo(rect.right - 10f, rect.centerY())
        path.lineTo(rect.left + 10f, rect.bottom)
        canvas.drawPath(path, iconPaint)
    }

    private fun drawTriangleIcon(canvas: Canvas, rect: RectF) {
        val path = Path()
        path.moveTo(rect.centerX(), rect.top)
        path.lineTo(rect.left, rect.bottom)
        path.lineTo(rect.right, rect.bottom)
        path.close()
        canvas.drawPath(path, iconPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pt = gctx.metrics.fromScreen(event.getX(actionIndex), event.getY(actionIndex))
        val tx = pt.x
        val ty = pt.y

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (exitButtonRect.contains(tx, ty)) {
                    TitleScene(gctx).change()
                    return true
                }
                if (recordButtonRect.contains(tx, ty)) {
                    isRecordingMode = !isRecordingMode
                    if (!isRecordingMode) selectedSpellName = null
                    return true
                }
                if (saveButtonRect.contains(tx, ty)) {
                    gestureManager.getLibrary()?.save()
                    lastResultText = "FILE SAVED!"
                    return true
                }
                
                // 스펠 선택 (REC 모드일 때만)
                if (isRecordingMode) {
                    for (btn in spellButtons) {
                        if (btn.rect.contains(tx, ty)) {
                            selectedSpellName = btn.name
                            return true
                        }
                    }
                }

                // 입력창
                if (touchId == -1 && touchRect.contains(tx, ty)) {
                    touchId = event.getPointerId(actionIndex)
                    isDrawing = true
                    currentPoints.clear()
                    currentPoints.add(GesturePoint(tx - touchRect.left, ty - touchRect.top, System.currentTimeMillis()))
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchId != -1) {
                    val index = event.findPointerIndex(touchId)
                    if (index != -1) {
                        val mPt = gctx.metrics.fromScreen(event.getX(index), event.getY(index))
                        currentPoints.add(GesturePoint(mPt.x - touchRect.left, mPt.y - touchRect.top, System.currentTimeMillis()))
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.getPointerId(actionIndex) == touchId) {
                    processGesture()
                    touchId = -1
                    isDrawing = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun processGesture() {
        if (currentPoints.size < 5) return
        val gesture = Gesture()
        gesture.addStroke(GestureStroke(ArrayList(currentPoints)))
        val library = gestureManager.getLibrary() ?: return

        if (isRecordingMode && selectedSpellName != null) {
            library.addGesture(selectedSpellName, gesture)
            lastResultText = "Recorded: $selectedSpellName"
            lastResultScore = "Count: ${currentPoints.size}"
            
            // XML 로그 출력
            saveGestureAsXml(selectedSpellName!!, gesture)
        } else {
            val predictions = library.recognize(gesture)
            if (!predictions.isNullOrEmpty()) {
                val best = predictions[0] as Prediction
                lastResultText = "Recognized: ${best.name}"
                lastResultScore = "Score: ${String.format(Locale.US, "%.2f", best.score)}"
            } else {
                lastResultText = "Not Found"
                lastResultScore = "Score: 0.0"
            }
        }
    }

    private fun saveGestureAsXml(name: String, gesture: Gesture) {
        try {
            val baos = ByteArrayOutputStream()
            val serializer = Xml.newSerializer()
            serializer.setOutput(baos, "UTF-8")
            serializer.startDocument("UTF-8", true)
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
            Log.i("GestureXML", "\n--- TEST SCENE GESTURE: $name ---\n${baos.toString("UTF-8")}\n--- END ---")
        } catch (e: Exception) { Log.e("GestureTest", "XML Error", e) }
    }
}
