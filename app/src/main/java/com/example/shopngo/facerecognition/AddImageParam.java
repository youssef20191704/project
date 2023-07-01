package com.example.shopngo.facerecognition;

import com.google.mlkit.vision.face.Face;

public class AddImageParam {
    private Face face = null;

    public synchronized void setFace(Face face) {
        this.face = face;
    }

    public synchronized Face getFace() {
        return face;
    }
}
