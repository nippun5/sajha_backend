package com.sajha.backend.security;

import com.sajha.backend.api.ApiProblem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Single-instance safety valve; replace with shared Redis or gateway limiter before multi-replica deployment. */
@Service
public class RateLimitService {
    private record Window(Instant since,int count){}
    private final ConcurrentHashMap<String,Window> limits=new ConcurrentHashMap<>();
    public void check(String key,int max,Duration period){
        Instant now=Instant.now();
        Window w=limits.compute(key,(k,old)->old==null || old.since.plus(period).isBefore(now)
            ? new Window(now,1) : new Window(old.since,old.count+1));
        if(w.count>max) throw new ApiProblem(HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMIT","Too many attempts; try again later");
        if(limits.size()>20_000) limits.entrySet().removeIf(e->e.getValue().since.plus(Duration.ofHours(2)).isBefore(now));
    }
}
