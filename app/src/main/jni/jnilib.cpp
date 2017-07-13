#include <jni.h>
#include "jnilib.h"

#include <cstdint>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <iostream>
#include <vector>


#include <opencv2/imgproc/types_c.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/background_segm.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/highgui/highgui.hpp>

#include "AdaptiveSelectiveBackgroundLearning.h"
#include "AdaptiveBackgroundLearning.h"
#include "Blob.h"

using namespace cv;
using namespace std;


extern "C" {


AdaptiveBackgroundLearning asbl;
bool first = true;
Mat lastFgMask;
vector<Blob> blobs;

JNIEXPORT jstring JNICALL
Java_com_studi_timesyncwifi_Utility_OpenCVNative_function(JNIEnv *jenv, jobject instance) {

    return jenv->NewStringUTF("txt");
}

JNIEXPORT jboolean JNICALL
Java_com_studi_timesyncwifi_Utility_OpenCVNative_cameraFrame(JNIEnv *, jobject
              , jlong addrRgbIn, jlong addrFgMask, jlong addrBgModel, jlong addrCalc) {

    Mat &input = *(Mat *) addrRgbIn;
    Mat &fgMask = *(Mat *) addrFgMask;
    Mat &bgModel = *(Mat *) addrBgModel;
    Mat &calc = *(Mat *) addrCalc;

    bool traversing = false;

    asbl.process(input, fgMask, bgModel);
    if (first) {
        first = false;
    } else {
        bitwise_not(fgMask, calc);
        bitwise_or(lastFgMask, calc, calc);
        bitwise_not(calc, calc);

        blur(calc, calc, Size(3,3));
        threshold(calc, calc, 200, 255, THRESH_BINARY);

        cvtColor(calc, calc, COLOR_BGR2GRAY, 1);

        vector<vector<Point>> contours;
        findContours(calc, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        vector<vector<Point>> convexHulls(contours.size());

        for (unsigned int i=0; i<contours.size(); i++) {
            convexHull(contours[i], convexHulls[i]);
        }


        blobs.clear();
        for (auto &convexHull : convexHulls) {
            Blob pBlob(convexHull);

            if (pBlob.boundingRect.area()>input.rows*input.cols/280) {
                blobs.push_back(pBlob);
            }
        }

        //Mat imgConvexHulls = bgModel.clone();
        //cvtColor(imgConvexHulls, imgConvexHulls, COLOR_BGR2BGRA, 4);
        convexHulls.clear();

        for (auto &blob : blobs) {
            convexHulls.push_back(blob.contour);
            if (blob.boundingRect.x < input.cols/2 &&
                    blob.boundingRect.x+blob.boundingRect.width > input.cols/2) {
                traversing = true;
                break;
            }
        }
        drawContours(calc, convexHulls, -1, Scalar(255), -1);
        //input = imgConvexHulls.clone();
    }
    lastFgMask = fgMask.clone();
    return jboolean(traversing);
}



}


