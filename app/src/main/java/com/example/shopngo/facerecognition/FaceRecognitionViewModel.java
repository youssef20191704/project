package com.example.shopngo.facerecognition;

import static android.content.Context.MODE_PRIVATE;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

public class FaceRecognitionViewModel extends AndroidViewModel {
    public FaceRecognitionViewModel(@NonNull Application application) {
        super(application);
    }

    public void saveRecognitionToSharedPref(String name, SimilarityClassifier.Recognition recognition) {
        SharedPreferences sharePref = getApplication().getSharedPreferences("RecognitionSharedPref", MODE_PRIVATE);
        HashMap<String, SimilarityClassifier.Recognition> savedHashMap = getRecognitionHashMap();

        savedHashMap.put(name, recognition);
        //convert to string using gson
        Gson gson = new Gson();
        String hashMapString = gson.toJson(recognition);

        sharePref.edit().putString("hashString", hashMapString).apply();
    }

    public HashMap<String, SimilarityClassifier.Recognition> getRecognitionHashMap() {
        //get from shared prefs
        SharedPreferences sharePref = getApplication().getSharedPreferences("RecognitionSharedPref", MODE_PRIVATE);
        Gson gson = new Gson();
        String storedHashMapString = sharePref.getString("hashString", "oopsDintWork");
        Type type = new TypeToken<HashMap<String, SimilarityClassifier.Recognition>>() {}.getType();
        if (storedHashMapString.equals("oopsDintWork")) {
            return new HashMap<String, SimilarityClassifier.Recognition>();
        } else {
            return gson.fromJson(storedHashMapString, type);
        }
    }
}
