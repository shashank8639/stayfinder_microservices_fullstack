package com.stayfinder.food.controller;

import com.stayfinder.food.dto.MenuItemRequest;
import com.stayfinder.food.dto.MenuItemResponse;
import com.stayfinder.food.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/hotels/{hotelId}/menu")
    public List<MenuItemResponse> menu(@PathVariable Long hotelId) {
        return menuService.listMenu(hotelId);
    }

    @PostMapping("/menu")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest request) {
        return menuService.create(request);
    }

    @PutMapping("/menu/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public MenuItemResponse update(@PathVariable Long itemId, @Valid @RequestBody MenuItemRequest request) {
        return menuService.update(itemId, request);
    }

    @DeleteMapping("/menu/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public void delete(@PathVariable Long itemId) {
        menuService.delete(itemId);
    }
}
