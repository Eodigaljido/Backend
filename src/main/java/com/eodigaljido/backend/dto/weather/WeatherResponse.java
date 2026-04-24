package com.eodigaljido.backend.dto.weather;

import java.util.List;

public record WeatherResponse(
        String location,
        String fetchedAt,
        CurrentWeatherDto current,
        AirQualityDto air,
        List<DailyForecastDto> weekly,
        Boolean stale
) {}
