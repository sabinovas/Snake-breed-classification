package com.example.venomvision.mainPageActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.venomvision.Classifier;
import com.example.venomvision.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SnakeDetection extends AppCompatActivity {

    private Classifier mClassifier;
    private Bitmap mBitmap;
    private AssetManager mAssetManager;
    private final int mCameraRequestCode = 0;
    private final int mGalleryRequestCode = 2;

    private final int mInputSize = 224;
    private final String mModelPath = "model_snake.tflite";
    private final String mLabelPath = "snakes_labels.txt";
    private final String mSamplePath = "diagram.png";

    ImageView mGalleryButton, mCameraButton, mPhotoImageView;
    Button mDetectButton;
    TextView mResultTextView, detectAs, demoTxt;

    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake);

        mAssetManager = getApplicationContext().getAssets(); // Retrieve AssetManager


        mPhotoImageView = findViewById(R.id.mPhotoImageView);
        mResultTextView = findViewById(R.id.mResultTextView);
        detectAs = findViewById(R.id.detectAs);
        demoTxt = findViewById(R.id.demoTxt);
        mClassifier = new Classifier(mAssetManager, mModelPath, mLabelPath, mInputSize);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("snake_detection_data");
        // Initialize Firebase Storage
        mStorageRef = FirebaseStorage.getInstance().getReference();


        try {
            InputStream inputStream = getAssets().open(mSamplePath);
            mBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true);
            mPhotoImageView.setImageBitmap(mBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCameraButton = findViewById(R.id.mCameraButton);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, mCameraRequestCode);
                } else {
                    Toast.makeText(SnakeDetection.this, "Camera is not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mGalleryButton = findViewById(R.id.mGalleryButton);
        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callGalleryIntent = new Intent(Intent.ACTION_PICK);
                callGalleryIntent.setType("image/*");
                startActivityForResult(callGalleryIntent, mGalleryRequestCode);
            }
        });

        mDetectButton = findViewById(R.id.mDetectButton);
        mDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Classifier.Recognition results = mClassifier.recognizeImage(mBitmap).isEmpty() ?
                        null : mClassifier.recognizeImage(mBitmap).get(0);

                if (results == null || results.getConfidence() < 0.40) {
                    mResultTextView.setText("Snake not detected!");
                } else {
                    mResultTextView.setText(results.getTitle() + '\n' + results.getConfidence());
                    mResultTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showSnakeData(results.getTitle());
                        }
                    });
                    storeDataInFirebase(results);

                }

                mResultTextView.setAlpha(1.0f);
                detectAs.setAlpha(1.0f);
                demoTxt.setAlpha(0.0f);
                mDetectButton.setVisibility(View.GONE);
            }
        });
    }

    private void storeDataInFirebase(Classifier.Recognition results) {
        String key = mDatabase.push().getKey();

        if (key != null) {
            // Create a map to store the data
            Map<String, Object> snakeData = new HashMap<>();
            snakeData.put("title", results.getTitle());
            snakeData.put("confidence", String.valueOf(results.getConfidence()));

            // Upload image to Firebase Storage and store data
            uploadImageToStorageAndStoreData(key, snakeData);
        }
    }

    private void uploadImageToStorageAndStoreData(final String key, final Map<String, Object> snakeData) {
        // Convert bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Create a reference to the location where you want to save the image in Firebase Storage
        StorageReference imageRef = mStorageRef.child("images/" + key + ".jpg");

        // Upload the image to Firebase Storage
        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        snakeData.put("imageUrl", uri.toString());
                        mDatabase.child(key).setValue(snakeData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Data stored successfully
                                        Toast.makeText(SnakeDetection.this, "Data stored successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle errors
                                        Toast.makeText(SnakeDetection.this, "Error storing data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SnakeDetection.this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSnakeData(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (title) {
            case "Western Ribbon Snake":
                View westernRibbonSnakeView = View.inflate(this, R.layout.western_ribbon_snake_layout, null);
                builder.setView(westernRibbonSnakeView);
                break;
            case "Western Rat Snake":
                View westernRatSnakeView = View.inflate(this, R.layout.western_rat_snake_layout, null);
                builder.setView(westernRatSnakeView);
                break;
            case "Western Diamondback Rattlesnake":
                View westernDiamondbackRattlesnakeView = View.inflate(this, R.layout.western_diamondback_rattlesnake_layout, null);
                builder.setView(westernDiamondbackRattlesnakeView);
                break;
            case "Water Moccasin":
                View waterMoccasinView = View.inflate(this, R.layout.water_moccasin_layout, null);
                builder.setView(waterMoccasinView);
                break;
            case "Timber Rattlesnake":
                View timberRattlesnakeView = View.inflate(this, R.layout.timber_rattlesnake_layout, null);
                builder.setView(timberRattlesnakeView);
                break;
            case "Terrestrial Garter Snake":
                View terrestrialGarterSnakeView = View.inflate(this, R.layout.terrestrial_garter_snake_layout, null);
                builder.setView(terrestrialGarterSnakeView);
                break;
            case "Rough Green Snake":
                View roughGreenSnakeView = View.inflate(this, R.layout.rough_green_snake_layout, null);
                builder.setView(roughGreenSnakeView);
                break;
            case "Rough Earth Snake":
                View roughEarthSnakeView = View.inflate(this, R.layout.rough_earth_snake_layout, null);
                builder.setView(roughEarthSnakeView);
                break;
            case "Ring-necked Snake":
                View ringNeckedSnakeView = View.inflate(this, R.layout.ring_necked_snake_layout, null);
                builder.setView(ringNeckedSnakeView);
                break;
            case "Red-bellied Snake":
                View redBelliedSnakeView = View.inflate(this, R.layout.red_bellied_snake_layout, null);
                builder.setView(redBelliedSnakeView);
                break;
            case "Red Diamond Rattlesnake":
                View redDiamondRattlesnakeView = View.inflate(this, R.layout.red_diamond_rattlesnake_layout, null);
                builder.setView(redDiamondRattlesnakeView);
                break;
            case "Python Non Venomous":
                View pythonNonVenomousView = View.inflate(this, R.layout.python_non_venomous_layout, null);
                builder.setView(pythonNonVenomousView);
                break;
            case "Prairie Rattlesnake":
                View prairieRattlesnakeView = View.inflate(this, R.layout.prairie_rattlesnake_layout, null);
                builder.setView(prairieRattlesnakeView);
                break;
            case "Plains Garter Snake":
                View plainsGarterSnakeView = View.inflate(this, R.layout.plains_garter_snake_layout, null);
                builder.setView(plainsGarterSnakeView);
                break;
            case "Plain-bellied Water Snake":
                View plainBelliedWaterSnakeView = View.inflate(this, R.layout.plain_bellied_water_snake_layout, null);
                builder.setView(plainBelliedWaterSnakeView);
                break;
            case "Pit Vipper Venomous":
                View pitVipperVenomousView = View.inflate(this, R.layout.pit_vipper_venomous_layout, null);
                builder.setView(pitVipperVenomousView);
                break;
            case "Northern Water Snake":
                View northernWaterSnakeView = View.inflate(this, R.layout.northern_water_snake_layout, null);
                builder.setView(northernWaterSnakeView);
                break;
            case "Mojave Rattlesnake":
                View mojaveRattlesnakeView = View.inflate(this, R.layout.mojave_rattlesnake_layout, null);
                builder.setView(mojaveRattlesnakeView);
                break;
            case "Long-nosed Snake":
                View longNosedSnakeView = View.inflate(this, R.layout.long_nosed_snake_layout, null);
                builder.setView(longNosedSnakeView);
                break;
            case "Kukri Non-Venomous":
                View kukriNonVenomousView = View.inflate(this, R.layout.kukri_non_venomous_layout, null);
                builder.setView(kukriNonVenomousView);
                break;
            case "Keelback Non-Venomous":
                View keelbackNonVenomousView = View.inflate(this, R.layout.keelback_non_venomous_layout, null);
                builder.setView(keelbackNonVenomousView);
                break;
            case "Indian Cat-Ven":
                View indianCatVenView = View.inflate(this, R.layout.indian_cat_ven_layout, null);
                builder.setView(indianCatVenView);
                break;
            case "Great Plains Rat Snake":
                View greatPlainsRatSnakeView = View.inflate(this, R.layout.great_plains_rat_snake_layout, null);
                builder.setView(greatPlainsRatSnakeView);
                break;
            case "Gray Rat Snake":
                View grayRatSnakeView = View.inflate(this, R.layout.gray_rat_snake_layout, null);
                builder.setView(grayRatSnakeView);
                break;
            case "Grass Snake":
                View grassSnakeView = View.inflate(this, R.layout.grass_snake_layout, null);
                builder.setView(grassSnakeView);
                break;
            case "Gopher Snake":
                View gopherSnakeView = View.inflate(this, R.layout.gopher_snake_layout, null);
                builder.setView(gopherSnakeView);
                break;
            case "Fox Snake":
                View foxSnakeView = View.inflate(this, R.layout.fox_snake_layout, null);
                builder.setView(foxSnakeView);
                break;
            case "Eastern Rat Snake":
                View easternRatSnakeView = View.inflate(this, R.layout.eastern_rat_snake_layout, null);
                builder.setView(easternRatSnakeView);
                break;
            case "Eastern Racer":
                View easternRacerView = View.inflate(this, R.layout.eastern_racer_layout, null);
                builder.setView(easternRacerView);
                break;
            case "Eastern Milk Snake":
                View easternMilkSnakeView = View.inflate(this, R.layout.eastern_milk_snake_layout, null);
                builder.setView(easternMilkSnakeView);
                break;
            case "Eastern Hognose Snake":
                View easternHognoseSnakeView = View.inflate(this, R.layout.eastern_hognose_snake_layout, null);
                builder.setView(easternHognoseSnakeView);
                break;
            case "Diamond-backed Water Snake":
                View diamondBackedWaterSnakeView = View.inflate(this, R.layout.diamond_backed_water_snake_layout, null);
                builder.setView(diamondBackedWaterSnakeView);
                break;
            case "Dekay's Brown Snake":
                View dekaysBrownSnakeView = View.inflate(this, R.layout.dekays_brown_snake_layout, null);
                builder.setView(dekaysBrownSnakeView);
                break;
            case "Corn Snake":
                View cornSnakeView = View.inflate(this, R.layout.corn_snake_layout, null);
                builder.setView(cornSnakeView);
                break;
            case "Copperhead":
                View copperheadView = View.inflate(this, R.layout.copperhead_layout, null);
                builder.setView(copperheadView);
                break;
            case "Common Garter Snake":
                View commonGarterSnakeView = View.inflate(this, R.layout.common_garter_snake_layout, null);
                builder.setView(commonGarterSnakeView);
                break;
            case "COBRA_VENOUMOUS":
                View cobraVenomousView = View.inflate(this, R.layout.cobra_venomous_layout, null);
                builder.setView(cobraVenomousView);
                break;
            case "Coachwhip":
                View coachwhipView = View.inflate(this, R.layout.coachwhip_layout, null);
                builder.setView(coachwhipView);
                break;
            case "Checkered Garter Snake":
                View checkeredGarterSnakeView = View.inflate(this, R.layout.checkered_garter_snake_layout, null);
                builder.setView(checkeredGarterSnakeView);
                break;
            case "California Kingsnake":
                View californiaKingsnakeView = View.inflate(this, R.layout.california_kingsnake_layout, null);
                builder.setView(californiaKingsnakeView);
                break;
            case "BLACK_HEADED_ROYAL_SNAKE_NON-VENOUMOUS":
                View blackHeadedRoyalSnakeView = View.inflate(this, R.layout.black_headed_royal_snake_layout, null);
                builder.setView(blackHeadedRoyalSnakeView);
                break;
            case "Banded Water Snake":
                View bandedWaterSnakeView = View.inflate(this, R.layout.banded_water_snake_layout, null);
                builder.setView(bandedWaterSnakeView);
                break;
            default:
                View defaultView = View.inflate(this, R.layout.custom_dialog1, null);
                builder.setView(defaultView);
                break;
        }


        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set click listener for the "Got it" button in the dialog
        Button gotItButton = dialog.findViewById(R.id.got_it_btn);
        if (gotItButton != null) {
            gotItButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == mCameraRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                mBitmap = (Bitmap) data.getExtras().get("data");
                mBitmap = scaleImage(mBitmap);
                Toast toast = Toast.makeText(this, "Image crop to: w= " + mBitmap.getWidth() + " h= " + mBitmap.getHeight(), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 20);
                toast.show();
                mPhotoImageView.setImageBitmap(mBitmap);
                mResultTextView.setText("Your photo image set now.");
                mDetectButton.setAlpha(1.0f);
                demoTxt.setAlpha(0.0f);
                mResultTextView.setAlpha(1.0f);
            } else {
                Toast.makeText(this, "Camera cancel..", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == mGalleryRequestCode) {
            if (data != null) {
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mBitmap = scaleImage(mBitmap);
                mPhotoImageView.setImageBitmap(mBitmap);
                mDetectButton.setAlpha(1.0f);
                demoTxt.setAlpha(0.0f);
                mResultTextView.setAlpha(1.0f);
            }
        } else {
            Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap scaleImage(Bitmap bitmap) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float scaleWidth = (float) mInputSize / originalWidth;
        float scaleHeight = (float) mInputSize / originalHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true);
    }
}
