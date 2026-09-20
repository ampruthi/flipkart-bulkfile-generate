package com.amit.flipkart.service;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ListingStatus;
import com.amit.flipkart.model.ListingCategory;
import com.amit.flipkart.repository.ListingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
public class ExcelService {

    private final ListingRepository repository;
    private final Resource template;
    private final Path outputDir;

    public ExcelService(
            ListingRepository repository,
            @Value("${app.excel.template}") Resource template,
            @Value("${app.excel.output-dir:./output}") String outputDir) {
        this.repository = repository;
        this.template = template;
        this.outputDir = Path.of(outputDir);
    }

    @Transactional
    public Path generatePending() {
        Path temporaryOutput = null;
        try {
            Files.createDirectories(outputDir);

            List<Listing> listings =
                    repository.findPendingEarringsOrLegacy(ListingStatus.EXCEL_PENDING, ListingCategory.EARRING);

            if (listings.isEmpty()) {
                throw new IllegalStateException("No EXCEL_PENDING listings found");
            }

            String filename = "flipkart-earring-upload-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                    ".xls";

            Path output = outputDir.resolve(filename);
            temporaryOutput = Files.createTempFile(outputDir, filename + ".", ".part");

            try (InputStream in = template.getInputStream();
                 HSSFWorkbook workbook = new HSSFWorkbook(in)) {

                Sheet sheet = workbook.getSheet("earring");
                if (sheet == null) {
                    throw new IllegalStateException("earring sheet not found");
                }

                Map<String, Integer> columns = headerMap(sheet);

                int rowIndex = firstEmptyDataRow(sheet);

                for (Listing listing : listings) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        row = sheet.createRow(rowIndex);
                    }
                    rowIndex++;
                    Map<String, Object> data = listing.getListingData();

                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        Integer col = columns.get(entry.getKey());
                        if (col != null) {
                            Cell cell = row.getCell(col);
                            if (cell == null) {
                                cell = row.createCell(col);
                            }
                            setCell(cell, entry.getValue());
                        }
                    }
                }

                try (OutputStream out = Files.newOutputStream(temporaryOutput)) {
                    workbook.write(out);
                }
            }

            validateGeneratedWorkbook(temporaryOutput);
            publish(temporaryOutput, output);
            temporaryOutput = null;

            // Do not mark a listing generated until its validated workbook is available.
            for (Listing listing : listings) {
                listing.setStatus(ListingStatus.EXCEL_GENERATED);
                listing.setExcelFile(output.toString());
                repository.save(listing);
            }

            return output;
        } catch (Exception e) {
            if (temporaryOutput != null) {
                try {
                    Files.deleteIfExists(temporaryOutput);
                } catch (IOException ignored) {
                    // Preserve the original export failure; the temporary file is harmless.
                }
            }
            throw new IllegalStateException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    private void validateGeneratedWorkbook(Path output) throws IOException {
        try (InputStream in = Files.newInputStream(output);
             HSSFWorkbook workbook = new HSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet("earring");
            if (sheet == null || headerMap(sheet).isEmpty()) {
                throw new IllegalStateException("Generated workbook is missing the earring template headers");
            }
        }
    }

    private void publish(Path temporaryOutput, Path output) throws IOException {
        try {
            Files.move(temporaryOutput, output,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryOutput, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Map<String, Integer> headerMap(Sheet sheet) {
        Map<String, Integer> map = new LinkedHashMap<>();
        Row header = sheet.getRow(0);
        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = new DataFormatter().formatCellValue(cell).trim();
                if (!value.isBlank()) map.put(value, c);
            }
        }
        return map;
    }

    private int firstEmptyDataRow(Sheet sheet) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) return r;

            boolean empty = true;
            for (int c = 0; c < Math.min(row.getLastCellNum(), 139); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && cell.getCellType() != CellType.BLANK &&
                        !new DataFormatter().formatCellValue(cell).isBlank()) {
                    empty = false;
                    break;
                }
            }
            if (empty) return r;
        }
        return sheet.getLastRowNum() + 1;
    }

    private void setCell(Cell cell, Object value) {
        if (value == null) return;
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }
}
