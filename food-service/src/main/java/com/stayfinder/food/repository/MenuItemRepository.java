package com.stayfinder.food.repository;

import com.stayfinder.food.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByHotelIdOrderByNameAsc(Long hotelId);
}
