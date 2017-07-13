//
// Created by studi on 1/3/17.
//

#ifndef TIMESYNCWIFI_BLOB_H
#define TIMESYNCWIFI_BLOB_H

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

class Blob {
public:
    std::vector<cv::Point> contour;
    cv::Rect boundingRect;
    cv::Point centerPosition;
    double dblDiagonalSize;
    double dblAspectRatio;
    Blob(std::vector<cv::Point> _contour);
private:

};



#endif //TIMESYNCWIFI_BLOB_H
