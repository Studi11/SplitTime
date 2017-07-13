//
// Created by studi on 3/23/17.
//

#ifndef TIMESYNCWIFI_ADAPTIVEBACKGROUNDLEARNING_H
#define TIMESYNCWIFI_ADAPTIVEBACKGROUNDLEARNING_H

#pragma once

#include <iostream>
#include <opencv2/opencv.hpp>


#include "IBGS.h"

class AdaptiveBackgroundLearning : public IBGS
{
private:
  bool firstTime;
  cv::Mat img_background;
  double alpha;
  long limit;
  long counter;
  double minVal;
  double maxVal;
  bool enableThreshold;
  int threshold;
  bool showForeground;
  bool showBackground;

public:
  AdaptiveBackgroundLearning();
  ~AdaptiveBackgroundLearning();

  void process(const cv::Mat &img_input, cv::Mat &img_output, cv::Mat &img_bgmodel);

private:
  void saveConfig();
  void loadConfig();
};

#endif /* ADAPTIVEBACKGROUNDLEARNING_H */

