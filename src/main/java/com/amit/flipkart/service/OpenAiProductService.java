package com.amit.flipkart.service;

import com.amit.flipkart.model.ProductUploadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.Base64;

@Service
public class OpenAiProductService {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;
    private final String reasoningEffort;
    private final MasterDataService masterData;
    private static final List<String> AI_FIELDS = List.of(
            "Type", "Ideal For", "Model Name", "Base Material", "Gemstone", "Diamond Clarity",
            "Pearl Type", "Certification", "Collection", "Plating", "Color", "Occasion",
            "Piercing Required", "Earring Back Type", "Finish", "Setting", "Silver Purity",
            "Metal Purity", "Natural/Synthetic Diamond", "Natural/Synthetic Ruby", "Ruby Shape",
            "Ruby Clarity", "Ruby Weight (carat)", "Natural/Synthetic Emerald", "Emerald Shape",
            "Emerald Clarity", "Natural/Synthetic Sapphire", "Sapphire Shape", "Sapphire Clarity",
            "Natural/Synthetic Amethyst", "Amethyst Shape", "Amethyst Clarity",
            "Artificial Pearl Material", "Pearl Shape", "Pearl Grade",
            "Natural/Synthetic Semi-precious Stone", "Semi-precious Stone Type",
            "Semi-precious Stone Shape", "Items Included", "Closure Type", "Sub Type",
            "Earring Shape", "With Ear Chain", "Earring Set Type", "Number of Pairs",
            "Number of Gemstones", "Design", "Metal Color", "Other Dimensions", "Other Features",
            "Description", "Search Keywords", "Key Features", "Ornamentation Type", "Net Quantity",
            "Brand Color");

    public OpenAiProductService(
            ObjectMapper mapper,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5.4}") String model,
            @Value("${app.openai.reasoning-effort:medium}") String reasoningEffort,
            MasterDataService masterData) {

        this.mapper = mapper;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.masterData = masterData;

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AiResult analyze(ProductUploadRequest input, MultipartFile image) {
        try {
            String mime = Optional.ofNullable(image.getContentType()).orElse("image/jpeg");
            String dataUrl = "data:" + mime + ";base64," +
                    Base64.getEncoder().encodeToString(image.getBytes());

            Map<String, Object> userText = new LinkedHashMap<>();
            userText.put("providedProduct", input);
            userText.put("allowedValues", masterData.allowedValues());

            String instruction = """
                    Analyze the supplied product image and product information to generate a Flipkart earring listing for Indian women.
=== GROUND RULES ===
1. IMITATION JEWELRY: Always treat jewelry as artificial/imitation unless explicitly stated otherwise.
   - Base Material → Alloy, Brass, or Copper (never precious metals)
   - Gemstone → Cubic Zirconia, CZ, Artificial Stones, or Beads
2. PLATING: Inspect the surface carefully. Identify: Gold Plated, Silver Plated, Rose Gold Plated, Oxidized/Black, Two-Tone, or Antique.
3. INDIAN MARKET: Apply Indian market vocabulary — Jhumka, Chandbali, Studs, Drops, Hoop, Meenakari, Kundan, Temple, Oxidized, Festive, Bridal, Ethnic — wherever contextually accurate.
4. TITLE ("aiTitle"): Generate a Flipkart-optimized title (50–70 characters).
   Format: [Plating/Color] [Design/Motif] [Earring Type] for Women
   Example: "Gold Plated Kundan Jhumka Earrings for Women"
   Do NOT invent a brand name.

=== OUTPUT RULES ===
- Return ONLY a valid, minified JSON object — no explanation, no markdown.
- Never invent commercial values: price, HSN, SKU, brand, manufacturer, country of origin, certifications, or tax codes.
- For fields with an allowedValues list → pick exactly one value from the list. Never invent a value outside it.
- For fields WITHOUT an allowedValues list:
  - Numeric/decimal/measurement fields (weight, dimensions in mm/cm/g) → use "" (empty string)
  - Non-determinable or irrelevant text fields → use "NA"
- For gemstone-specific fields (diamond, ruby, emerald, sapphire, amethyst, pearl properties) that clearly don't apply to this product → use "NA"

=== FIELDS TO POPULATE ===
Type, Ideal For, Model Name, Base Material, Gemstone, Diamond Clarity, Pearl Type, Certification, Collection, Plating, Color, Occasion, Piercing Required, 
Earring Back Type, Finish, Setting, Silver Purity, Metal Purity, Natural/Synthetic Diamond, Natural/Synthetic Ruby, Ruby Shape, Ruby Clarity, Ruby Weight (carat), 
Natural/Synthetic Emerald, Emerald Shape, Emerald Clarity, Natural/Synthetic Sapphire, Sapphire Shape, Sapphire Clarity, Natural/Synthetic Amethyst, Amethyst Shape, 
Amethyst Clarity, Artificial Pearl Material, Pearl Shape, Pearl Grade, Natural/Synthetic Semi-precious Stone, Semi-precious Stone Type, 
Semi-precious Stone Shape, Items Included, Closure Type, Sub Type, Earring Shape, With Ear Chain, Earring Set Type, Number of Pairs, Number of Gemstones, Design,
 Metal Color, Other Dimensions, Other Features, Description, Search Keywords, Key Features, 
 Ornamentation Type, Net Quantity, Brand Color, aiTitle                  
                """;



            Map<String, Object> contentText = Map.of(
                    "type", "input_text",
                    "text", instruction + "\n\nInput:\n" +
                            mapper.writeValueAsString(userText));

            Map<String, Object> contentImage = Map.of(
                    "type", "input_image",
                    "image_url", dataUrl);

            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", List.of(contentText, contentImage));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("reasoning", Map.of("effort", reasoningEffort));
            body.put("input", List.of(message));
            body.put("text", Map.of("format", responseFormat()));

            String raw = client.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(raw);
            String text = extractOutputText(root);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain output text");
            }

            String json = cleanJson(text);
            Map<String, Object> fields = mapper.readValue(json, LinkedHashMap.class);

            validateAllowedValues(fields);
            return new AiResult(fields, raw);

        } catch (Exception e) {
            throw new IllegalStateException("OpenAI product analysis failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : AI_FIELDS) {
            properties.put(field, fieldSchema(field));
        }
        properties.put("aiTitle", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("additionalProperties", false);

        return Map.of(
                "type", "json_schema",
                "name", "flipkart_earring_attributes",
                "strict", true,
                "schema", schema);
    }

    private Map<String, Object> fieldSchema(String field) {
        List<String> allowed = masterData.allowedValues(field);
        if (allowed.isEmpty()) {
            return Map.of("type", "string");
        }
        return Map.of("type", "string", "enum", allowed);
    }

    private void validateAllowedValues(Map<String, Object> fields) {
        List<String> invalidFields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            List<String> allowed = masterData.allowedValues(key);
            if (allowed.isEmpty()) continue;

            String candidate = value == null ? "" : String.valueOf(value).trim();
            if (!allowed.contains(candidate)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI returned values outside Flipkart allowed values for: " + invalidFields);
        }
    }

    private String extractOutputText(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray()) return null;

        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) continue;
            for (JsonNode c : content) {
                if ("output_text".equals(c.path("type").asText())) {
                    return c.path("text").asText();
                }
            }
        }
        return null;
    }

    private String cleanJson(String text) {
        String value = text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value.trim();
    }

    public record AiResult(Map<String, Object> fields, String rawResponse) {}
}
