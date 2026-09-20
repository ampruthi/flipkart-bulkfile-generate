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
    private final MasterDataService masterData;
    private static final String earringAllowedValues = """
            Type: Chandbali Earring, Clip-on Earring, Cuff Earring, Drops & Danglers, Ear Thread, Earring Set, Hoop Earring, Huggie Earring, Jhumki Earring, Magnetic Earring, Plug Earring, Rhinestone Studs, Stud Earring, Tassel Earring, Tunnel Earring
            
            Ideal For: Baby Boys, Baby Girls, Boys, Girls, Men, Women
            
            Base Material: Acrylic, Alloy, Aluminum, Bone, Brass, Bronze, Ceramic, Cobalt, Copper, Cotton Dori, Crystal, Enamel, Fabric, German Silver, Glass, Gold, Ivory, Jute, Lac, Leather, Metal, Mother of Pearl, Nickel, Paper, Plastic, Porcelain, Resin, Ribbon, Rubber, Shell, Silicone, Silk Dori, Silver, Stainless Steel, Steel, Stone, Terracotta, Tungsten, White Metal, Wood, Zinc
            
            Gemstone: Agate, Alexandrite, Amber, Amethyst, Andalusite, Aquamarine, Beads, Beryl, Black Diamond, Blue Sapphire, Carnelian, Cat's Eye, Chalcedony, Citrine, Coral, Crystal, Cubic Zirconia, Danburite, Diamond, Diopside, Emerald, Garnet, Iolite, Jade, Kyanite, Labradorite, Lapis Lazuli, Malachite, Moissanite, Moonstone, Mother of Pearl, NA, Onyx, Opal, Orange Sapphire, Pearl, Peridot, Quartz, Ruby, Sapphire, Spinel, Swarovski Crystal, Swarovski Zirconia, Tanzanite, Tiger Eye, Titanium Drusy, Topaz, Tourmaline, Tsavorite, Turquoise, White Zircon, Zircon
            
            Pearl Type: Cultured, Freshwater, NA, Plastic, South Sea, Tahitian
            
            Certification: BIS Hallmark, Brand Certification, EGL, GIA, GSL, HKD, IDI, IGI, IGL, NA, SGL, Swarovski Authenticity
            
            Collection: Contemporary, Ethnic
            
            Plating: 800 Silver, 830 Silver, 900 Silver, 958 Silver, 999 Silver, Black Silver, Brass, Copper, Enamel, Gold-plated, NA, Palladium, Platinum, Rhodium, Silver, Sterling Silver, Titanium
            
            Color: Aqua, Beige, Black, Blue, Bronze, Brown, Copper, Gold, Green, Grey, Maroon, Multicolor, Orange, Pink, Platinum, Purple, Red, Rose Gold, Sea Green, Silver, Turquoise, White, Yellow
            
            Diamond Clarity: FL, I1, I2, I3, IF, NA, SI, SI-I, SI1, SI2, VS, VS-SI, VS1, VS2, VVS, VVS-VS, VVS1, VVS2
            
            Occasion: Everyday, Love, Party, Religious, Wedding & Engagement, Workwear
            
            Closure Type: Clip-on, Hooks, Hoopwire, Magnetic, Push Plugs, Screw
            
            Sub Type: Bar Danglers, Basic Stud, Bead Tassels, Behind the Ear, Chain Cuffs, Chain Link Earring, Chandbali Jhumkis, Chandelier Earring, Classic Jumki, Closed Hoop, Cuffs with Danglers, Dangle Earring, Dangler Hoop, Dangler Tunnels, Dreamcatchers, Drop Earring, Drop Tassels, Ear Spike, Fan Tassels, Fringe Danglers, Half Moon Chandbalis, Hoop Chandbali, Hoop Jhumkis, Hoop Tassels, Hoop Tunnels, Huggie Cuff, Huggie Studs, Jacket Earring, Jhalar Jhumki, Layered Chandbalis, Layered Hoop, Layered Jhumki, Mesh Danglers, Multi Tassels, NA, Needle Thread, Open Hoop, Over the Ear, Pom Pom Tassels, Regular Chandbalis, Round Tassels, Stick-on Earring, Stud Tunnels, Tiered Tassels
            
            Earring Shape: Abstract, Animals, Ball, Bell, Birds, Bows, Butterfly, Elephant, Feather, Floral, Geometric, God Symbols, Heart, Leaf, Moon, Oval, Owl, Paisley, Parrot, Peacock, Round, Square, Star, Sun, Teardrop
            
            Earring Set Type: Chandbali Earring, Cuff Earring, Drops & Danglers, Hoop Earring, Jhumki Earring, Stud Earring, Tunnel Earring
            
            Design: Heavy, Minimal, Statement
            
            Ornamentation Type: Beads, Coins, Cutwork/Filigree, Dried Flowers, Enamel Decoartions, Feather, Gemstones, Ghungroo, Glitter, Hand-painted, Kundan, Mirror Work, None, Pearl, Pom Poms, Stones, Tassel""";

    public OpenAiProductService(
            ObjectMapper mapper,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5-mini}") String model,
            MasterDataService masterData) {

        this.mapper = mapper;
        this.model = model;
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
                    Analyze the supplied product image and the supplied product information.
                    This is a Flipkart earring listing targeted at Indian women and adults.
                    
                    CRITICAL INSTRUCTIONS FOR ACCURACY & CONTEXT:
                    1. IMITATION JEWELRY RULE: Always assume the jewelry is artificial/imitation. Reflect this in "Base Material" (e.g., Alloy, Brass, Copper) and "Gemstone" fields (e.g., Cubic Zirconia, CZ, Artificial Stones, Beads) unless the provided text explicitly proves otherwise. Do not use real precious metal purities or natural diamond certifications.
                    2. PLATING & METAL COLOR: Carefully inspect the surface. Identify if it is Gold Plated, Silver Plated, Rose Gold Plated, Black/Oxidized (very common for Indian jhumkas/chandbalis), or Two-Tone.\s
                    3. INDIAN MARKET RELEVANCE: When analyzing design, motifs, occasion, and "aiTitle", explicitly consider Indian festive, wedding, ethnic, and daily wear trends. Use relevant vocabulary where appropriate (e.g., Jhumka, Chandbali, Studs, Drops, Hoop, Meenakari, Kundan, Temple Jewelry, Oxidized, Festive, Wedding).
                    4. TITLE GENERATION ("aiTitle"): Create a concise, high-converting, Flipkart-optimized marketplace title (approx. 50-70 characters). Follow this format: [Brand/Generic] [Plating/Color] [Design/Style Style] [Type of Earring] for Women. (Example: "Gold Plated Pearl Drop Jhumka Earrings for Women"). Do not invent a brand name; use "Artificial" or a generic identifier if the brand is blank.
                    
                    Return ONLY a valid, minified JSON object. Fill only product/catalog attributes that can reasonably be inferred from the image or supplied information. Never invent factual commercial values such as price, HSN, brand, SKU, country of origin, manufacturer, packer, tax code, or certification.
                    
                    If a field cannot be determined, return an NA string ("NA"). Where an allowed-values list is supplied, choose exactly one value from that list. Do not invent a value outside the list.
                    
                    Include these fields strictly, and consider value from allowedValues (provided based on field name, else keep empty. If nothing match with prediction then Seelct NA / Other from list:
                    Type, Ideal For, Model Name, Base Material, Gemstone, Diamond Clarity, Pearl Type, Certification, Collection, Plating, Color, Occasion, Piercing Required, Earring Back Type, Finish, Setting, Silver Purity, Metal Purity, Natural/Synthetic Diamond, Natural/Synthetic Ruby, Ruby Shape, Ruby Clarity, Ruby Weight (carat), Natural/Synthetic Emerald, Emerald Shape, Emerald Clarity, Natural/Synthetic Sapphire, Sapphire Shape, Sapphire Clarity, Natural/Synthetic Amethyst, Amethyst Shape, Amethyst Clarity, Artificial Pearl Material, Pearl Shape, Pearl Grade, Pearl Diameter (mm), Natural/Synthetic Semi-precious Stone, Semi-precious Stone Type, Semi-precious Stone Shape, Items Included, Closure Type, Sub Type, Earring Shape, With Ear Chain, Earring Set Type, Number of Pairs, Number of Gemstones, Design, Metal Color, Metal Weight, Width (mm), Height (mm), Diameter (mm), Weight (g), Other Dimensions, Other Features, Description, Search Keywords, Key Features, Ornamentation Type, Net Quantity, Brand Color.
                    
                    Also generate the concise marketplace title in "aiTitle".
                    
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
            body.put("input", List.of(message));

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

    private void validateAllowedValues(Map<String, Object> fields) {
        fields.replaceAll((key, value) -> {
            List<String> allowed = masterData.allowedValues(key);
            if (allowed.isEmpty() || value == null) return value;

            String candidate = String.valueOf(value).trim();
            return allowed.contains(candidate) ? candidate : "";
        });
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
