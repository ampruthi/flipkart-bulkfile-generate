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

@Service
public class NecklaceOpenAiProductService {
    private static final List<String> FIELDS = List.of(
            "Base Material", "Type", "Gemstone", "Ideal For", "Pack of", "Plating", "Color",
            "Ornamentation Type", "Occasion", "Pearl Type", "Certification", "Collection",
            "Model Name", "Diamond Cut", "Finish", "Clasp Type", "Number of Diamonds",
            "Natural/Synthetic Diamond", "Diamond Shape", "Ruby Shape", "Ruby Clarity",
            "Natural/Synthetic Ruby", "Ruby Weight (carat)", "Emerald Clarity", "Emerald Shape",
            "Natural/Synthetic Emerald", "Natural/Synthetic Sapphire", "Sapphire Shape",
            "Sapphire Clarity", "Natural/Synthetic Amethyst", "Amethyst Shape", "Amethyst Clarity",
            "Pearl Grade", "Pearl Shape", "Natural/Synthetic Semi-precious Stone",
            "Description", "Search Keywords", "Key Features", "Brand Color");

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;
    private final String reasoningEffort;
    private final NecklaceMasterDataService masterData;

    public NecklaceOpenAiProductService(ObjectMapper mapper,
                                         @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${app.openai.api-key:}") String apiKey,
                                         @Value("${app.openai.model:gpt-5.4}") String model,
                                         @Value("${app.openai.reasoning-effort:medium}") String reasoningEffort,
                                         NecklaceMasterDataService masterData) {
        this.mapper = mapper;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.masterData = masterData;
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).build();
    }

    public AiResult analyze(ProductUploadRequest input, MultipartFile image) {
        try {
            String mime = Optional.ofNullable(image.getContentType()).orElse("image/jpeg");
            String imageUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image.getBytes());
            Map<String, Object> supplied = Map.of("providedProduct", input, "allowedValues", masterData.allowedValues());
            String instruction = """
                    Analyze this necklace-chain image for a Flipkart India listing. Treat it as imitation jewellery unless supplied information proves otherwise.
                    Choose categorical values only from the supplied allowed values. Use NA only when it is an allowed value; use an empty string for unknown measurements.
                    Never invent price, HSN, brand, SKU, origin, manufacturer, packer, importer, tax code, or image URLs.
                    Use Indian jewellery terminology accurately: necklace, chain, choker, layered, kundan, oxidised, festive, bridal, ethnic.
                    Return the required structured attributes only.
                    === FIELDS TO POPULATE ===
Type, Ideal For, Model Name, Base Material, Gemstone, Diamond Clarity, Pearl Type, Certification, Collection, Plating, Color, Occasion, Piercing Required, 
Earring Back Type, Finish, Setting, Silver Purity, Metal Purity, Natural/Synthetic Diamond, Natural/Synthetic Ruby, Ruby Shape, Ruby Clarity, Ruby Weight (carat), 
Natural/Synthetic Emerald, Emerald Shape, Emerald Clarity, Natural/Synthetic Sapphire, Sapphire Shape, Sapphire Clarity, Natural/Synthetic Amethyst, Amethyst Shape, 
Amethyst Clarity, Artificial Pearl Material, Pearl Shape, Pearl Grade, Natural/Synthetic Semi-precious Stone, Semi-precious Stone Type, 
Semi-precious Stone Shape, Items Included, Closure Type, Sub Type, Earring Shape, With Ear Chain, Earring Set Type, Number of Pairs, Number of Gemstones, Design,
 Metal Color, Other Dimensions, Other Features, Description, Search Keywords, Key Features, 
 Ornamentation Type, Net Quantity, Brand Color, aiTitle
                    """;
            Map<String, Object> message = Map.of("role", "user", "content", List.of(
                    Map.of("type", "input_text", "text", instruction + "\nInput:\n" + mapper.writeValueAsString(supplied)),
                    Map.of("type", "input_image", "image_url", imageUrl, "detail", "high")));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("reasoning", Map.of("effort", reasoningEffort));
            body.put("input", List.of(message));
            body.put("text", Map.of("format", responseFormat()));
            String raw = client.post().uri("/responses").contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);
            Map<String, Object> fields = mapper.readValue(outputText(raw), LinkedHashMap.class);
            validate(fields);
            return new AiResult(fields, raw);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI necklace analysis failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : FIELDS) {
            List<String> allowed = masterData.allowedValues(field);
            properties.put(field, allowed.isEmpty() ? Map.of("type", "string") : Map.of("type", "string", "enum", allowed));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object"); schema.put("properties", properties);
        schema.put("required", new ArrayList<>(properties.keySet())); schema.put("additionalProperties", false);
        return Map.of("type", "json_schema", "name", "flipkart_necklace_attributes", "strict", true, "schema", schema);
    }

    private String outputText(String raw) throws Exception {
        JsonNode output = mapper.readTree(raw).path("output");
        for (JsonNode item : output) for (JsonNode content : item.path("content")) {
            if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
        }
        throw new IllegalStateException("OpenAI response did not contain output text");
    }

    private void validate(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            List<String> allowed = masterData.allowedValues(entry.getKey());
            if (!allowed.isEmpty() && !allowed.contains(String.valueOf(entry.getValue()).trim())) {
                throw new IllegalStateException("Invalid necklace allowed value for " + entry.getKey());
            }
        }
    }

    public record AiResult(Map<String, Object> fields, String rawResponse) { }
}
