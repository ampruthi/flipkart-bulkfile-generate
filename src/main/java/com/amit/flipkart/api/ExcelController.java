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

    @PostMapping("/earring/generate")
    public Map<String, String> generateEarringFile() {
        Path path = service.generatePending();
        return Map.of(
                "status", "EXCEL_GENERATED",
                "file", path.toAbsolutePath().toString()
        );
    }

    @GetMapping("/earring/download")
    public ResponseEntity<Resource> downloadEarringFile(@RequestParam String file) {
        Path path = service.generatePending();
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(resource);
    }

    @PostMapping("/necklace/generate")
    public Map<String, String> generateNecklaceFile() {
        Path path = service.generatePending();
        return Map.of("status", "EXCEL_GENERATED", "file", path.toAbsolutePath().toString());
    }

    @GetMapping("/necklace/download")
    public ResponseEntity<Resource> downloadNecklaceFile() {
        Path path = service.generatePending();
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(resource);
    }
}
