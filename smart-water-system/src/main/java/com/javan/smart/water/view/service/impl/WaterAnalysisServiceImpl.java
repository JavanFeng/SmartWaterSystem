package com.javan.smart.water.view.service.impl;

import com.javan.smart.water.view.model.WaterAnalysisModel;
import com.javan.smart.water.view.service.WaterAnalysisService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author FengJ
 * @description service
 */
@Service
public class WaterAnalysisServiceImpl implements WaterAnalysisService {

    private Map<String, WaterAnalysisModel> CACHE = new LinkedHashMap<>(50) {
        @Override
        public boolean removeEldestEntry(Map.Entry<String, WaterAnalysisModel> eldest) {
            return CACHE.size() > 50;
        }
    };


    @Override
    public void save(WaterAnalysisModel waterAnalysis) {
        // logic...
        CACHE.put(buildKey(waterAnalysis), waterAnalysis);
    }

    @Override
    public WaterAnalysisModel getByKey(String uniqueKey) {
        return CACHE.get(uniqueKey);
    }

    @Override
    public String buildKey(WaterAnalysisModel waterAnalysis) {
        return buildKey(waterAnalysis.getStationCode(), waterAnalysis.getDay(), waterAnalysis.getHour());
    }

    @Override
    public String buildKey(String stationCode, String day, String hour) {
        return stationCode + day + hour;
    }
}
