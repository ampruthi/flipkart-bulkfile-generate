package com.amit.flipkart.repository;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    Optional<Listing> findBySellerSkuId(String sellerSkuId);
    List<Listing> findAllByStatusOrderByCreatedAtAsc(ListingStatus status);
}
