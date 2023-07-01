package com.example.shopngo.facerecognition

import com.example.shopngo.facerecognition.SimilarityClassifier.Recognition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecognitionObject {
     lateinit var recognition:Recognition
     fun recognitiontostring(): String? {
         return Gson().toJson(recognition)

     }
    fun stringtorecognition(facerecognitiontostring: String): Recognition? {
        val type = object : TypeToken< Recognition>() {}.type
        return Gson().fromJson< Recognition>(facerecognitiontostring, type)
    }
 }