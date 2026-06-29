package com.javan.smart.water.view.service;

import com.javan.smart.water.view.model.WaterAnalysisModel;

/**
 * @author FengJ
 * @description 分析记录
 */
public interface WaterAnalysisService {

    void save(WaterAnalysisModel waterAnalysis);

    WaterAnalysisModel getByKey(String uniqueKey);


    String buildKey(WaterAnalysisModel waterAnalysis);

    String buildKey(String stationCode, String day, String hour);
}
