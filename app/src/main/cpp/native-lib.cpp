#include "com_example_testandroid_MainActivity.h"
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <vector>

/* classifier for detecting face */
cv::CascadeClassifier cascade;
bool hasClassifier = false;

/* detect face and draw a circle */
void detectAndDraw(cv::Mat& img, double scale);

JNIEXPORT void JNICALL Java_com_example_testandroid_MainActivity_initializeFaceDetector
        (JNIEnv *, jobject) {
    hasClassifier = cascade.load("/storage/emulated/0/facedetect/haarcascade_frontalface_alt.xml");
}

JNIEXPORT void JNICALL Java_com_example_testandroid_MainActivity_detectFace
        (JNIEnv *env, jobject, jbyteArray data, jint width, jint height, jobject bitmap) {
    if(!hasClassifier) return;
    jbyte * yuvData = env->GetByteArrayElements(data, NULL);
    cv::Mat yuvImage(height+height/2, width, CV_8U, yuvData);
    cv::Mat image(height, width, CV_8UC4);
    cv::cvtColor(yuvImage, image, cv::COLOR_YUV2BGRA_NV12);
    detectAndDraw(image, 5.0);
    env->ReleaseByteArrayElements(data, yuvData, JNI_ABORT);

    void * pixels;
    AndroidBitmap_lockPixels(env,bitmap,&pixels);
    cv::Mat tmp(height,width,CV_8UC4,pixels);
    image.copyTo(tmp);
    AndroidBitmap_unlockPixels(env,bitmap);
}

void detectAndDraw(cv::Mat& img, double scale)
{
    std::vector<cv::Rect> faces, faces2;
    const static cv::Scalar colors[] =
            {
                    cv::Scalar(255,0,0),
                    cv::Scalar(255,128,0),
                    cv::Scalar(255,255,0),
                    cv::Scalar(0,255,0),
                    cv::Scalar(0,128,255),
                    cv::Scalar(0,255,255),
                    cv::Scalar(0,0,255),
                    cv::Scalar(255,0,255)
            };
    cv::Mat gray, smallImg;

    cvtColor(img, gray, cv::COLOR_BGR2GRAY);
    double fx = 1 / scale;
    resize( gray, smallImg, cv::Size(), fx, fx, cv::INTER_LINEAR_EXACT );
    equalizeHist( smallImg, smallImg );


    cascade.detectMultiScale( smallImg, faces,
                              1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30) );

    for ( size_t i = 0; i < faces.size(); i++ )
    {
        cv::Rect r = faces[i];
        cv::Mat smallImgROI;
        std::vector<cv::Rect> nestedObjects;
        cv::Point center;
        cv::Scalar color = colors[i%8];
        int radius;

        double aspect_ratio = (double)r.width/r.height;
        if( 0.75 < aspect_ratio && aspect_ratio < 1.3 )
        {
            center.x = cvRound((r.x + r.width*0.5)*scale);
            center.y = cvRound((r.y + r.height*0.5)*scale);
            radius = cvRound((r.width + r.height)*0.25*scale);
            circle( img, center, radius, color, 3, 8, 0 );
        }
        else
            rectangle( img, cvPoint(cvRound(r.x*scale), cvRound(r.y*scale)),
                       cvPoint(cvRound((r.x + r.width-1)*scale), cvRound((r.y + r.height-1)*scale)),
                       color, 3, 8, 0);
    }
}