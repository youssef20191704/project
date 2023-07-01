package com.example.shopngo.facerecognition;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.shopngo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FaceRecognitionActivity extends CameraActivity implements MainInterface {
    private FaceRecognitionViewModel viewModel;
    private AlertDialog dialog = null;
    private static final Logger LOGGER = new Logger();
    private AddImageParam addImageParam = new AddImageParam();
    private FloatingActionButton fabAdd;
    private FaceDetector faceDetector;
    private SimilarityClassifier detector;
    //Used Integer.MAX_VALUE To make it full screen
    private static final Size DESIRED_PREVIEW_SIZE = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);
    // MobileFaceNet
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private Bitmap rgbFrameBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Bitmap croppedBitmap = null;
    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;
    private Integer sensorOrientation;


    private boolean computingDetection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FaceRecognitionViewModel.class);
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(view -> {
            fabAdd.setClickable(false);
            if (addImageParam.getFace() != null) {
                // Note this can be done only once
                int sourceW = rgbFrameBitmap.getWidth();
                int sourceH = rgbFrameBitmap.getHeight();
                int targetW = portraitBmp.getWidth();
                int targetH = portraitBmp.getHeight();
                Matrix transform = createTransform(sourceW, sourceH, targetW, targetH, sensorOrientation);
                final Canvas cv = new Canvas(portraitBmp);

                // draws the original image in portrait mode.
                cv.drawBitmap(rgbFrameBitmap, transform, null);

                final Canvas cvFace = new Canvas(faceBmp);

                LOGGER.i("FACE" + addImageParam.getFace().toString());
                //results = detector.recognizeImage(croppedBitmap);

                final RectF boundingBox = new RectF(addImageParam.getFace().getBoundingBox());

                // maps crop coordinates to original
                cropToFrameTransform.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(portraitBmp, matrix, null);

                //canvas.drawRect(faceBB, paint);

                String label = "";
                float confidence = -1f;
                float[][] extra = null;

                Bitmap crop = null;
                if ((faceBB.left + faceBB.width()) <= portraitBmp.getWidth() && faceBB.left >= 0)
                    crop = Bitmap.createBitmap(portraitBmp, (int) faceBB.left, (int) faceBB.top, (int) faceBB.width(), (int) faceBB.height());


                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, true);

                if (resultsAux.size() > 0) {

                    SimilarityClassifier.Recognition result = resultsAux.get(0);

                    extra = result.getExtra();

                    float conf = result.getDistance();
                    if (conf < 1.0f) {

                        confidence = conf;
                        label = result.getTitle();
                    }

                }

                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }

                final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", label, confidence, boundingBox);

                result.setLocation(boundingBox);
                result.setExtra(extra);
                result.setCrop(crop);


                if (crop != null)
                    updateResults(result);
            }

            fabAdd.setClickable(true);
        });
        initFaceDetector();

    }

    private void initFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).build();
        FaceDetector detector = FaceDetection.getClient(options);

        faceDetector = detector;
    }

    @Override
    protected void processImage() {
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;


        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector.process(image).addOnSuccessListener(faces -> {
            if (faces.size() == 0) {
                addImageParam.setFace(null);
                updateResults(null);
                return;
            }
            runInBackground(() -> {
                onFacesDetected(faces);
            });
        }).addOnFailureListener((fail) -> addImageParam.setFace(null));
    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
            detector.setMainInterface(this);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast = Toast.makeText(getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight, cropW, cropH, sensorOrientation);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_camera_connection;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private void updateResults(final SimilarityClassifier.Recognition mappedRecognition) {
        computingDetection = false;
        if (mappedRecognition != null) {
            LOGGER.i("Adding results");
            if (mappedRecognition.getExtra() != null) {
                showAddFaceDialog(mappedRecognition);
            }

        }
    }

    private void onFacesDetected(List<Face> faces) {
        final List<SimilarityClassifier.Recognition> mappedRecognitions = new LinkedList<>();


        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(sourceW, sourceH, targetW, targetH, sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        for (Face face : faces) {
            addImageParam.setFace(face);

            LOGGER.i("FACE" + face.toString());
            //results = detector.recognizeImage(croppedBitmap);

            final RectF boundingBox = new RectF(face.getBoundingBox());

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
            final boolean goodConfidence = true; //face.get;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                cropToFrameTransform.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(portraitBmp, matrix, null);

                //canvas.drawRect(faceBB, paint);

                String label = "";
                float confidence = -1f;
                float[][] extra = null;


                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, false);

                if (resultsAux.size() > 0) {

                    SimilarityClassifier.Recognition result = resultsAux.get(0);

                    extra = result.getExtra();

                    float conf = result.getDistance();
                    if (conf < 1.0f) {

                        confidence = conf;
                        label = result.getTitle();
                    }

                }

                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }

                final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", label, confidence, boundingBox);

                result.setLocation(boundingBox);
                result.setExtra(extra);
                mappedRecognitions.add(result);

            }


        }
        runOnUiThread(() -> {
            updateResults(mappedRecognitions.get(0));
        });
    }

    private void showAddFaceDialog(SimilarityClassifier.Recognition rec) {
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogLayout = inflater.inflate(R.layout.dialog_face_recognition_image, null);
            ImageView ivFace = dialogLayout.findViewById(R.id.dlg_image);
            EditText etName = dialogLayout.findViewById(R.id.dlg_input);

            ivFace.setImageBitmap(rec.getCrop());
            etName.setHint("Input name");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dlg, int i) {

                    String name = etName.getText().toString();
                    if (name.isEmpty()) {
                        return;
                    }
                    viewModel.saveRecognitionToSharedPref(name, rec);
                    detector.setShouldGetRegistered(true);

                    RecognitionObject.recognition = rec;
                    setResult(Activity.RESULT_OK);
                    //knownFaces.put(name, rec);
                    dlg.dismiss();
                    finish();

                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
//                    fragment.enablingCamera();
                }
            });
            builder.setView(dialogLayout);
            dialog = builder.create();
        } else {
            ImageView ivFace = dialog.findViewById(R.id.dlg_image);
            ivFace.setImageBitmap(rec.getCrop());
        }
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    // Face Processing
    private Matrix createTransform(final int srcWidth, final int srcHeight, final int dstWidth, final int dstHeight, final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    @Override
    public HashMap<String, SimilarityClassifier.Recognition> getRegistered() {
        return viewModel.getRecognitionHashMap();
    }
}