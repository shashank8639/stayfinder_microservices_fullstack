package com.stayfinder.booking.seed;

import com.stayfinder.booking.entity.Hotel;
import com.stayfinder.booking.entity.Room;
import com.stayfinder.booking.repository.HotelRepository;
import com.stayfinder.booking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "stayfinder.booking.seed-demo-data", havingValue = "true")
public class DemoCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCatalogSeeder.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public DemoCatalogSeeder(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedHotel("Grand Horizon Hyderabad", "Hyderabad", "Lakeside business hotel", "3500.00", "5200.00");
        seedHotel("Creek Palace Dubai", "Dubai", "Creek-view stay near Deira", "650.00", "980.00");
        seedHotel("Fort View Jaipur", "Jaipur", "Heritage rooms near Amber Fort", "2800.00", "4100.00");
    }

    private void seedHotel(String name, String city, String description, String standardPrice, String deluxePrice) {
        if (hotelRepository.findByName(name).isPresent()) {
            return;
        }
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setDescription(description);
        hotelRepository.save(hotel);
        seedRoom(hotel, "101", "STANDARD", new BigDecimal(standardPrice));
        seedRoom(hotel, "201", "DELUXE", new BigDecimal(deluxePrice));
        log.info("Seeded demo hotel id={} name={}", hotel.getId(), name);
    }

    private void seedRoom(Hotel hotel, String number, String type, BigDecimal price) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(number);
        room.setRoomType(type);
        room.setPricePerNight(price);
        roomRepository.save(room);
    }
}
