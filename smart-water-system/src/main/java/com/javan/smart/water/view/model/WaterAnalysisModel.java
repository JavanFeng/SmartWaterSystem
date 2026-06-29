package com.javan.smart.water.view.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author FengJ
 * @description 水质分析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaterAnalysisModel {

    private String stationCode;

    private String day;

    private String hour;

    private String analysisResult;

    private String cancel;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String stationCode;
        private String day;
        private String hour;
        private String analysisResult;

        public Builder stationCode(String stationCode) {
            this.stationCode = stationCode;
            return this;
        }

        public Builder day(String day) {
            this.day = day;
            return this;
        }

        public Builder hour(String hour) {
            this.hour = hour;
            return this;
        }

        public Builder analysisResult(String analysisResult) {
            this.analysisResult = analysisResult;
            return this;
        }

        public WaterAnalysisModel build() {
            return new WaterAnalysisModel(stationCode, day, hour, analysisResult, null);
        }
    }
}
