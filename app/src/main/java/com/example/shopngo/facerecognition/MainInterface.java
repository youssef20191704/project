package com.example.shopngo.facerecognition;

import java.util.HashMap;

public interface MainInterface {
    HashMap<String, SimilarityClassifier.Recognition> getRegistered();
}
