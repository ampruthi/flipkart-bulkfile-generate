package com.amit.flipkart.api;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ListingStatus;
import com.amit.flipkart.repository.ListingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    private final ListingRepository repository;

    public ListingController(ListingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Listing> getListings(
            @RequestParam(defaultValue = "OTHER") ListingStatus status) {
        return repository.findAllByStatusOrderByCreatedAtAsc(status);
    }
}
