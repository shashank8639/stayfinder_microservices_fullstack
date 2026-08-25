package com.stayfinder.food.service;

import com.stayfinder.food.dto.MenuItemRequest;
import com.stayfinder.food.dto.MenuItemResponse;
import com.stayfinder.food.entity.MenuItem;
import com.stayfinder.food.exception.ResourceNotFoundException;
import com.stayfinder.food.mapper.FoodMapper;
import com.stayfinder.food.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final MenuItemRepository menuItemRepository;

    public MenuService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listMenu(Long hotelId) {
        return menuItemRepository.findByHotelIdOrderByNameAsc(hotelId).stream()
                .map(FoodMapper::toMenuResponse)
                .toList();
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        apply(item, request);
        menuItemRepository.save(item);
        log.info("Menu item created id={} hotelId={}", item.getId(), item.getHotelId());
        return FoodMapper.toMenuResponse(item);
    }

    @Transactional
    public MenuItemResponse update(Long itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
        apply(item, request);
        log.info("Menu item updated id={}", itemId);
        return FoodMapper.toMenuResponse(item);
    }

    @Transactional
    public void delete(Long itemId) {
        if (!menuItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Menu item not found: " + itemId);
        }
        menuItemRepository.deleteById(itemId);
        log.info("Menu item deleted id={}", itemId);
    }

    private void apply(MenuItem item, MenuItemRequest request) {
        item.setHotelId(request.hotelId());
        item.setName(request.name().trim());
        item.setPrice(request.price());
        item.setAvailable(request.available() == null || request.available());
    }
}
