package com.globalisosecurity.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record RpmMlPredictionResponse(
        @JsonProperty("model_version") String modelVersion,
        List<Prediction> predictions) {

    public record Prediction(
            @JsonProperty("analysis_id") Long analysisId,
            @JsonProperty("priority") String priority,
            Double confidence,
            Map<String, Double> probabilities,
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("requires_human_review") Boolean requiresHumanReview,
            @JsonProperty("confidence_note") String confidenceNote) {
    }
}
