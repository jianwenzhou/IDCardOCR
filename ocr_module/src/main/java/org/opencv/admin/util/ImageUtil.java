/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package org.opencv.admin.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.text.Html;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil {
    private static final String TAG = "CameraExif";


    /**
     * 高亮图片变暗
     * @param src
     * @param dst
     */
    public static void highlightRemove(Mat src, Mat dst) {

        for (int i = 0; i < src.rows(); i++) {

            for (int j = 0; j < src.cols(); j++) {
                double B = src.get(i, j)[0];
                double G = src.get(i, j)[1];
                double R = src.get(i, j)[2];

                double alpha_r = R / (R + G + B);
                double alpha_g = G / (R + G + B);
                double alpha_b = B / (R + G + B);

                double alpha = Math.max(Math.max(alpha_r, alpha_g), alpha_b);
                double MaxC = Math.max(Math.max(R, G), B);
                double minalpha = Math.min(Math.min(alpha_r, alpha_g), alpha_b);
                double beta_r = 1 - (alpha - alpha_r) / (3 * alpha - 1);
                double beta_g = 1 - (alpha - alpha_g) / (3 * alpha - 1);
                double beta_b = 1 - (alpha - alpha_b) / (3 * alpha - 1);
                double beta = Math.max(Math.max(beta_r, beta_g), beta_b);
                double gama_r = (alpha_r - minalpha) / (1 - 3 * minalpha);
                double gama_g = (alpha_g - minalpha) / (1 - 3 * minalpha);
                double gama_b = (alpha_b - minalpha) / (1 - 3 * minalpha);
                double gama = Math.max(Math.max(gama_r, gama_g), gama_b);

                double temp = (gama * (R + G + B) - MaxC) / (3 * gama - 1);

                double[] data = new double[3];
                data[0] = B - (temp + 0.5);
                data[1] = G - (temp + 0.5);
                data[2] = R - (temp + 0.5);
                dst.put(i, j, data);
            }
        }
    }


    public static Rect findRectangle(Bitmap image) {
        try {
            Mat tempor = new Mat();
            Mat src = new Mat();
            Utils.bitmapToMat(image, tempor);

            Imgproc.cvtColor(tempor, src, Imgproc.COLOR_BGR2RGB);

            Mat blurred = src.clone();
            Imgproc.medianBlur(src, blurred, 9);

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            List<Mat> blurredChannel = new ArrayList<Mat>();
            blurredChannel.add(blurred);
            List<Mat> gray0Channel = new ArrayList<Mat>();
            gray0Channel.add(gray0);

            MatOfPoint2f approxCurve;

            double maxArea = 0;
            int maxId = -1;

            for (int c = 0; c < 3; c++) {
                int ch[] = {c, 0};
                Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

                int thresholdLevel = 1;
                for (int t = 0; t < thresholdLevel; t++) {
                    if (t == 0) {
                        Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1);
                    } else {
                        Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                                Imgproc.THRESH_BINARY,
                                (src.width() + src.height()) / 200, t);
                    }

                    Imgproc.findContours(gray, contours, new Mat(),
                            Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    for (MatOfPoint contour : contours) {
                        MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                        double area = Imgproc.contourArea(contour);
                        approxCurve = new MatOfPoint2f();
                        Imgproc.approxPolyDP(temp, approxCurve,
                                Imgproc.arcLength(temp, true) * 0.02, true);

                        if (approxCurve.total() == 4 && area >= maxArea) {
                            double maxCosine = 0;

                            List<Point> curves = approxCurve.toList();
                            for (int j = 2; j < 5; j++) {

                                double cosine = Math.abs(angle(curves.get(j % 4),
                                        curves.get(j - 2), curves.get(j - 1)));
                                maxCosine = Math.max(maxCosine, cosine);
                            }

                            if (maxCosine < 0.3) {
                                maxArea = area;
                                maxId = contours.indexOf(contour);
                            }
                        }
                    }
                }
            }

            if (maxId >= 0) {
                Rect rect = Imgproc.boundingRect(contours.get(maxId));

                Imgproc.rectangle(src, rect.tl(), rect.br(), new Scalar(255, 0, 0, .8), 4);

                int mDetectedWidth = rect.width;
                int mDetectedHeight = rect.height;

                Log.e("OCR", "Rectangle width :" + mDetectedWidth + " Rectangle height :" + mDetectedHeight);
                return rect;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

    //获取图片清晰度
    public static double getLaplacian(Mat mat) {
        Mat imageGrey = new Mat();
        Imgproc.cvtColor(mat, imageGrey, Imgproc.COLOR_RGB2GRAY);
        Mat imageLaplacian = new Mat();
        Imgproc.Laplacian(imageGrey, imageLaplacian, CvType.CV_16U);    //拉普拉斯梯度
        imageGrey.release();
        //图像的平均灰度
        return Core.mean(imageLaplacian).val[0];
    }


    public static boolean isBlurByOpenCV(Bitmap image) {
        int l = CvType.CV_8UC1; //8-bit grey scale image
        Mat matImage = new Mat();
        Utils.bitmapToMat(image, matImage);
        Mat matImageGrey = new Mat();
        Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY); // 图像灰度化

        Bitmap destImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
        Canvas canvas = new Canvas(destImage);
        Paint paint = new Paint();
        Matrix m = new Matrix();
        canvas.drawBitmap(image, m, paint);

        Mat dst2 = new Mat();
        Utils.bitmapToMat(destImage, dst2);
        Mat laplacianImage = new Mat();
        dst2.convertTo(laplacianImage, l);
        Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U); // 拉普拉斯变换
        Mat laplacianImage8bit = new Mat();
        laplacianImage.convertTo(laplacianImage8bit, l);

        Log.e("OCR", "bitmap1=" + image.isRecycled());
        Bitmap bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(laplacianImage8bit, bmp);
        int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight()); // bmp为轮廓图

        int maxLap = -16777216; // 16m
        for (int pixel : pixels) {
            if (pixel > maxLap)
                maxLap = pixel;
        }
        int userOffset = -3881250; // 界线（严格性）降低一点
        int soglia = -6118750 + userOffset; // -6118750为广泛使用的经验值
        Log.e("OCR", "maxLap=" + maxLap);

        soglia += 6118750 + userOffset;
        maxLap += 6118750 + userOffset;
        Log.e("OCR", "bitmap2=" + image.isRecycled());

        matImage.release();
        matImageGrey.release();
        dst2.release();
        laplacianImage.release();
        destImage.recycle();
        bmp.recycle();

        Log.e("OCR", "opencvanswers..result：image.w=" + image.getWidth() + ", image.h=" + image.getHeight()
                + "\nmaxLap= " + maxLap + "(清晰范围:0~" + (6118750 + userOffset) + ")"
                + "\n" + Html.fromHtml("<font color='#eb5151'><b>" + (maxLap <= soglia ? "模糊" : "清晰") + "</b></font>"));
        return maxLap <= soglia;
    }

    public static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    // Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
    public static int getOrientation(byte[] jpeg) {
        if (jpeg == null) {
            return 0;
        }

        int offset = 0;
        int length = 0;

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.length && (jpeg[offset++] & 0xFF) == 0xFF) {
            int marker = jpeg[offset] & 0xFF;

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue;
            }
            offset++;

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue;
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false);
            if (length < 2 || offset + length > jpeg.length) {
                Log.e(TAG, "Invalid length");
                return 0;
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8
                    && pack(jpeg, offset + 2, 4, false) == 0x45786966
                    && pack(jpeg, offset + 6, 2, false) == 0) {
                offset += 8;
                length -= 8;
                break;
            }

            // Skip other markers.
            offset += length;
            length = 0;
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            int tag = pack(jpeg, offset, 4, false);
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.e(TAG, "Invalid byte order");
                return 0;
            }
            boolean littleEndian = (tag == 0x49492A00);

            // Get the offset and check if it is reasonable.
            int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
            if (count < 10 || count > length) {
                Log.e(TAG, "Invalid offset");
                return 0;
            }
            offset += count;
            length -= count;

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian);
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian);
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    int orientation = pack(jpeg, offset + 8, 2, littleEndian);
                    switch (orientation) {
                        case 1:
                            return 0;
                        case 3:
                            return 180;
                        case 6:
                            return 90;
                        case 8:
                            return 270;
                        default:
                            return 0;
                    }
                }
                offset += 12;
                length -= 12;
            }
        }

        Log.i(TAG, "Orientation not found");
        return 0;
    }

    private static int pack(byte[] bytes, int offset, int length,
                            boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }

        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 保存图片
     *
     * @param src
     *         源图片
     * @return {@code true}: 成功<br>{@code false}: 失败
     */
    public static boolean save(Bitmap src, File file) {
        OutputStream os = null;
        boolean ret = false;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            ret = src.compress(Bitmap.CompressFormat.JPEG, 100, os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(os);
        }
        return ret;
    }


    /**
     * 关闭IO
     *
     * @param closeables
     *         closeables
     */
    public static void closeIO(final Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
