package com.ronda.backend.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {
    @GetMapping
    public StatusResponse status() {
        return new StatusResponse("ronda-backend", "ok");
    }

    public record StatusResponse(String service, String status) { }
}
