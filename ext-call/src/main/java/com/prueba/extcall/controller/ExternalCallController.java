package com.prueba.extcall.controller;

import com.prueba.extcall.model.ExternalRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/ext-call")
public class ExternalCallController {

    @Async
    @PostMapping("/simulate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> simulate(@RequestBody ExternalRequest request) throws InterruptedException {
        
        //simulación de latencia entre 300ms y 1000ms
        int delay = ThreadLocalRandom.current().nextInt(300, 1001);
        Thread.sleep(delay);

         //simulación
        int randomValue = ThreadLocalRandom.current().nextInt(101);
        if (randomValue < 33) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "message", "Simulación exitosa: Cod. 200",
                    "eventType", request.getEventType(),
                    "source", request.getSource()
            )));
        }

        if (randomValue >=33 && randomValue < 66) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "TOO_MANY_REQUESTS",
                    "message", "Error transitorio simulado: Cod. 429",
                    "eventType", request.getEventType(),
                    "source", request.getSource()
            )));
        }

        if (randomValue >= 66 && randomValue <= 90) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Error transitorio simulado: Cod. 5.x.x",
                    "eventType", request.getEventType(),
                    "source", request.getSource()
            )));
        }

        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "BAD_REQUEST",
                "message", "Error definitivo simulado: Cod. 400",
                "eventType", request.getEventType(),
                "source", request.getSource()
        )));
    }
}
