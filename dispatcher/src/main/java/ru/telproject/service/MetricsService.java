package ru.telproject.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final MeterRegistry meterRegistry;

    public Counter createCounter(String name, String description){
        return Counter.builder(name)
                .description(description)
                .register(meterRegistry);
    }

    public Timer createTimer(String name, String description){
        return Timer.builder(name)
                .description(description)
                .register(meterRegistry);
    }

    public void incrementCounter(String name, String... tags){
        meterRegistry.counter(name, tags).increment();
    }

    public <T> T recordingTime(String name, Supplier<T> operation){
        return Timer.builder(name)
                .register(meterRegistry)
                .record(operation);
    }

    public void recordingTimeVoid(String name, Runnable operation){
         Timer.builder(name)
                .register(meterRegistry)
                .record(operation);
    }
}
