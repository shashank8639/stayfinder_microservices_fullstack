package com.stayfinder.food.repository;

import com.stayfinder.food.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    @Query("""
            SELECT DISTINCT o FROM FoodOrder o
            LEFT JOIN FETCH o.items
            WHERE o.userId = :userId
            ORDER BY o.createdAt DESC
            """)
    List<FoodOrder> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT o FROM FoodOrder o
            LEFT JOIN FETCH o.items
            WHERE o.hotelId = :hotelId
            ORDER BY o.createdAt DESC
            """)
    List<FoodOrder> findByHotelIdOrderByCreatedAtDesc(@Param("hotelId") Long hotelId);

    @Query("""
            SELECT o FROM FoodOrder o
            LEFT JOIN FETCH o.items
            WHERE o.id = :id
            """)
    Optional<FoodOrder> findDetailedById(@Param("id") Long id);
}
