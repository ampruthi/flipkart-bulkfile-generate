package com.amit.flipkart.api;

import com.amit.flipkart.service.ExcelService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/excel")
public class ExcelController {

    private final ExcelService service;

    public ExcelController(ExcelService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public Map<String, String> generate() {
        Path path = service.generatePending();
        return Map.of(
                "status", "EXCEL_GENERATED",
                "file", path.toAbsolutePath().toString()
        );
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String file) {
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
