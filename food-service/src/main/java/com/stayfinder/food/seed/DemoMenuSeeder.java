package com.stayfinder.food.seed;

import com.stayfinder.food.entity.MenuItem;
import com.stayfinder.food.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * One menu per hotel (hotelId copied from Booking — Food has no hotel table).
 * Demo ids 1/2/3 match the Booking catalog seeder order on an empty database.
 */
@Component
@ConditionalOnProperty(name = "stayfinder.food.seed-demo-data", havingValue = "true")
public class DemoMenuSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoMenuSeeder.class);

    private final MenuItemRepository menuItemRepository;

    public DemoMenuSeeder(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedMenu(1L, "Hyderabadi Biryani", "420.00", "Masala Chai", "80.00");
        seedMenu(2L, "Chicken Shawarma", "35.00", "Karak Chai", "8.00");
        seedMenu(3L, "Dal Baati", "280.00", "Lassi", "90.00");
    }

    private void seedMenu(Long hotelId, String main, String mainPrice, String drink, String drinkPrice) {
        if (!menuItemRepository.findByHotelIdOrderByNameAsc(hotelId).isEmpty()) {
            return;
        }
        seed(hotelId, main, new BigDecimal(mainPrice));
        seed(hotelId, drink, new BigDecimal(drinkPrice));
        log.info("Seeded demo menu for hotelId={}", hotelId);
    }

    private void seed(Long hotelId, String name, BigDecimal price) {
        MenuItem item = new MenuItem();
        item.setHotelId(hotelId);
        item.setName(name);
        item.setPrice(price);
        item.setAvailable(true);
        menuItemRepository.save(item);
    }
}
