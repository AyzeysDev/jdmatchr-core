package com.jdmatchr.core.dto;

record RoleFitAndAlignmentMetricsDto(
        PredictionDto prediction,
        RadarDataDto radarData,
        AlignmentBreakdownDto alignmentBreakdown
) {
}
