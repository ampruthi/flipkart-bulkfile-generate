package com.amit.flipkart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class MasterDataService {

    private final ObjectMapper mapper;
    private final Resource template;

    private volatile Map<String, List<String>> allowedValues = Map.of();

    public MasterDataService(ObjectMapper mapper,
                             @Value("${app.excel.template}") Resource template) {
        this.mapper = mapper;
        this.template = template;
        reload();
    }

    public synchronized void reload() {
        try (InputStream in = template.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet index = workbook.getSheet("Index");
            if (index == null) {
                throw new IllegalStateException("Index sheet not found");
            }

            Map<String, List<String>> result = new LinkedHashMap<>();
            Row header = index.getRow(1); // Excel row 2
            if (header == null) {
                allowedValues = result;
                return;
            }

            for (int c = 0; c < header.getLastCellNum(); c++) {
                String field = text(header.getCell(c));
                if (field == null || field.isBlank()) continue;

                List<String> values = new ArrayList<>();
                for (int r = 2; r <= index.getLastRowNum(); r++) {
                    String value = text(index.getRow(r) == null ? null : index.getRow(r).getCell(c));
                    if (value != null && !value.isBlank() && !values.contains(value)) {
                        values.add(value);
                    }
                }
                if (!values.isEmpty()) result.put(field, values);
            }
            allowedValues = result;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load Flipkart Index sheet", e);
        }
    }

    public Map<String, List<String>> allowedValues() {
        return allowedValues;
    }

    public List<String> allowedValues(String field) {
        return allowedValues.getOrDefault(field, List.of());
    }

    private String text(Cell cell) {
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
