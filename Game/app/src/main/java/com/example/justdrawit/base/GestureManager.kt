package com.example.justdrawit.base

import android.content.Context
import android.gesture.Gesture
import android.gesture.GestureLibraries
import android.gesture.GestureLibrary
import android.util.Log
import com.example.justdrawit.R
import java.io.File

class GestureManager(context: Context) {
    private var library: GestureLibrary? = null
    private val storeFile = File(context.filesDir, "gestures")

    fun getLibrary() = library

    init {
        // 1. 내부 저장소 파일 로드 시도
        if (storeFile.exists()) {
            try {
                val fileLib = GestureLibraries.fromFile(storeFile)
                if (fileLib.load()) {
                    library = fileLib
                    Log.d("GestureManager", "Loaded gestures from internal storage.")
                }
            } catch (e: Exception) {
                Log.e("GestureManager", "Failed to load internal storage file. It might be corrupted.", e)
                // 파일이 깨졌을 가능성이 높으므로 삭제하여 다음 실행 시 복구되게 함
                storeFile.delete()
            }
        }
        
        // 2. 내부 저장소 로드 실패 시 res/raw/gestures에서 불러옴
        if (library == null) {
            try {
                val rawLib = GestureLibraries.fromRawResource(context, R.raw.gestures)
                if (rawLib.load()) {
                    library = rawLib
                    Log.d("GestureManager", "Loaded gestures from raw resource.")
                }
            } catch (e: Exception) {
                Log.e("GestureManager", "Failed to load raw resource.", e)
            }
        }

        // 3. 만약 모든 로드에 실패했다면, 빈 라이브러리라도 생성하여 에러를 방지합니다.
        if (library == null) {
            library = GestureLibraries.fromFile(storeFile)
            Log.d("GestureManager", "Created a new empty gesture library in memory.")
        }
    }

    /**
     * 사용자가 그린 제스처를 분석하여 가장 일치하는 결과의 이름을 반환합니다.
     */
    fun recognize(gesture: Gesture): String? {
        val lib = library ?: return null
        
        try {
            // 라이브러리에 등록된 제스처가 하나도 없으면 엔진 오류가 날 수 있음
            if (lib.gestureEntries.isEmpty()) return null
            
            val predictions = lib.recognize(gesture) ?: return null
            
            if (predictions.isNotEmpty()) {
                val best = predictions[0]
                // 점수(Score)가 높을수록 정확도가 높습니다. (보통 1.0 이상이면 신뢰할 만함)
                if (best.score > 1.0) {
                    return best.name
                }
            }
        } catch (e: Exception) {
            Log.e("GestureManager", "Error during recognition. Library might be invalid.", e)
        }
        return null
    }
}
