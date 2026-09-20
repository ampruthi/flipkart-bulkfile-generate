package com.amit.flipkart.api;

import com.amit.flipkart.service.MasterDataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/master-data")
public class MasterDataController {

    private final MasterDataService service;

    public MasterDataController(MasterDataService service) {
        this.service = service;
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() {
        service.reload();
        return Map.of(
                "status", "OK",
                "fields", service.allowedValues().keySet()
        );
    }
}
