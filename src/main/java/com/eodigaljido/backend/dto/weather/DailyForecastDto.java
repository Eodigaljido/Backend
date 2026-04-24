package com.eodigaljido.backend.dto.weather;

public record DailyForecastDto(
        String date,
        String dayOfWeek,
        String weatherIcon,
        String weatherDesc,
        Double tempMax,
        Double tempMin,
        int pop
) {}
