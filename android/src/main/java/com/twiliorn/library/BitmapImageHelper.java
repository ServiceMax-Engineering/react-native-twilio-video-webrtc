package com.twiliorn.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class BitmapImageHelper {

    public static String saveImage(Bitmap bitmap, Context context, String deviceCurrentOrientation) {
        String filename = generateCacheFilePath("jpeg", context);

        try {
            Bitmap rotatedBitmap = BitmapImageHelper.rotateImageIfRequired(bitmap, deviceCurrentOrientation);
            FileOutputStream out = new FileOutputStream(filename);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            generateCacheFilePath("jpeg", context);
            out.flush();
            out.close();
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateCacheFilePath(String extension, Context context) {
        File outputDir = context.getCacheDir();

        String outputUri = String.format("%s/%s." + extension, outputDir.getPath(), UUID.randomUUID().toString());
        return outputUri;
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, String deviceCurrentOrientation) throws IOException {
        switch (deviceCurrentOrientation) {

            case "LANDSCAPE_RIGHT":
                return rotateImage(img, 90);
            case "LANDSCAPE_LEFT":
                return rotateImage(img, -90);
            case "PORTRAIT_UPSIDE_DOWN":
                return rotateImage(img, 180);
            case "PORTRAIT":
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
}

