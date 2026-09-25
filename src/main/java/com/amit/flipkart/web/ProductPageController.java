package com.amit.flipkart.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductPageController {

    @GetMapping("/products/upload")
    public String uploadPage() {
        return "product-upload";
    }

    @GetMapping("/products/listing")
    public String listing() {
        return "listings";
    }

    @GetMapping("/files/status")
    public String fileOperations() {
        return "file-operations";
    }
}