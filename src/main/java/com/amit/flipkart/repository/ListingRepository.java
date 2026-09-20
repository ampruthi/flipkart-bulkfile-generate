package com.amit.flipkart.repository;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ListingStatus;
import com.amit.flipkart.model.ListingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    Optional<Listing> findBySellerSkuId(String sellerSkuId);
    List<Listing> findAllByStatusOrderByCreatedAtAsc(ListingStatus status);
    List<Listing> findAllByStatusAndCategoryOrderByCreatedAtAsc(ListingStatus status, ListingCategory category);

    @Query("select l from Listing l where l.status = :status and (l.category = :category or l.category is null) order by l.createdAt asc")
    List<Listing> findPendingEarringsOrLegacy(@Param("status") ListingStatus status,
                                              @Param("category") ListingCategory category);
}
