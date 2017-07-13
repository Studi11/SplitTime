//
// Created by studi on 1/3/17.
//

#ifndef TIMESYNCWIFI_ADAPTIVESELECTIVEBACKGROUNDLEARNING_H
#define TIMESYNCWIFI_ADAPTIVESELECTIVEBACKGROUNDLEARNING_H

#pragma once

#include <iostream>
#include <opencv2/opencv.hpp>


#include "IBGS.h"

class AdaptiveSelectiveBackgroundLearning : public IBGS
{
private:
  bool firstTime;
  cv::Mat img_background;
  double alphaLearn;
  double alphaDetection;
  long learningFrames;
  long counter;
  double minVal;
  double maxVal;
  int threshold;
  bool showOutput;

public:
  AdaptiveSelectiveBackgroundLearning();
  ~AdaptiveSelectiveBackgroundLearning();

  void process(const cv::Mat &img_input, cv::Mat &img_output, cv::Mat &img_bgmodel);

  private:
      void saveConfig();
      void loadConfig();

};

#endif //TIMESYNCWIFI_ADAPTIVESELECTIVEBACKGROUNDLEARNING_H
