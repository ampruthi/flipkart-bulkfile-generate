package com.amit.flipkart.api;

import com.amit.flipkart.service.NecklaceExcelService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/necklaces/excel")
public class NecklaceExcelController {
    private final NecklaceExcelService service;
    public NecklaceExcelController(NecklaceExcelService service) { this.service = service; }

    @PostMapping("/generate")
    public Map<String, String> generate() {
        Path path = service.generatePending();
        return Map.of("status", "EXCEL_GENERATED", "file", path.toAbsolutePath().toString());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String file) {
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(resource);
    }
}
