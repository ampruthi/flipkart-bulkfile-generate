package com.amit.flipkart.service;

import com.amit.flipkart.exception.ResourceNotFoundException;
import com.amit.flipkart.model.*;
import com.amit.flipkart.repository.ListingRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NecklaceExcelService {
    private static final String SHEET = "necklace_chain";
    private static final int FIRST_DATA_ROW = 4;
    private final ListingRepository repository;
    private final Resource template;
    private final Path outputDir;

    public NecklaceExcelService(ListingRepository repository,
                                @Value("${app.excel.necklace-template}") Resource template,
                                @Value("${app.excel.output-dir:./output}") String outputDir) {
        this.repository = repository;
        this.template = template;
        this.outputDir = Path.of(outputDir);
    }

    @Transactional
    public Path generatePending() {
        Path temporary = null;
        try {
            Files.createDirectories(outputDir);
            List<Listing> listings = repository.findAllByStatusAndCategoryOrderByCreatedAtAsc(
                    ListingStatus.EXCEL_PENDING, ListingCategory.NECKLACE_CHAIN);
            if (listings.isEmpty()) {
                throw new ResourceNotFoundException(
                        "No pending neckalce listings found"
                );
            }
            String filename = "flipkart-necklace-chain-upload-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xls";
            Path output = outputDir.resolve(filename);
            temporary = Files.createTempFile(outputDir, filename + ".", ".part");
            try (InputStream in = template.getInputStream(); HSSFWorkbook workbook = new HSSFWorkbook(in)) {
                Sheet sheet = workbook.getSheet(SHEET);
                if (sheet == null) {
                    throw new ResourceNotFoundException(
                            "File Not Found!"
                    );
                }
                Map<String, Integer> columns = headerMap(sheet);
                int rowIndex = FIRST_DATA_ROW;
                for (Listing listing : listings) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) row = sheet.createRow(rowIndex);
                    for (Map.Entry<String, Object> entry : listing.getListingData().entrySet()) {
                        Integer column = columns.get(entry.getKey());
                        if (column != null && entry.getValue() != null) setCell(existingOrNew(row, column), entry.getValue());
                    }
                    rowIndex++;
                }
                try (OutputStream out = Files.newOutputStream(temporary)) { workbook.write(out); }
            }
            validate(temporary);
            move(temporary, output);
            temporary = null;
            for (Listing listing : listings) {
                listing.setStatus(ListingStatus.EXCEL_GENERATED);
                listing.setExcelFile(output.toString());
                repository.save(listing);
            }
            return output;
        } catch (Exception e) {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new IllegalStateException("Necklace Excel generation failed: " + e.getMessage(), e);
        }
    }

    private Cell existingOrNew(Row row, int column) { Cell cell = row.getCell(column); return cell == null ? row.createCell(column) : cell; }
    private Map<String, Integer> headerMap(Sheet sheet) {
        Map<String, Integer> map = new LinkedHashMap<>(); Row header = sheet.getRow(0);
        for (int c = 0; header != null && c < header.getLastCellNum(); c++) {
            String value = new DataFormatter().formatCellValue(header.getCell(c)).trim();
            if (!value.isBlank()) map.put(value, c);
        }
        return map;
    }
    private void validate(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file); HSSFWorkbook workbook = new HSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null || !headerMap(sheet).containsKey("Seller SKU ID")) throw new IllegalStateException("Invalid necklace workbook");
        }
    }
    private void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    private void setCell(Cell cell, Object value) {
        if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else if (value instanceof Boolean b) cell.setCellValue(b);
        else cell.setCellValue(String.valueOf(value));
    }
}
