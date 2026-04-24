package com.eodigaljido.backend.dto.weather;

public record CurrentWeatherDto(
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        String windDirection,
        double precipitation1h,
        String precipitationType,
        String weatherIcon,
        String weatherDesc,
        int pop
) {}
