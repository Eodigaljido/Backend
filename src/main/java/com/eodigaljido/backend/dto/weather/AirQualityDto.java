package com.eodigaljido.backend.dto.weather;

public record AirQualityDto(
        int pm10,
        String pm10Grade,
        int pm25,
        String pm25Grade,
        int aqi,
        String aqiGrade
) {}
