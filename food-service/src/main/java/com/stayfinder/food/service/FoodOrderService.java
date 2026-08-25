package com.stayfinder.food.service;

import com.stayfinder.food.client.BookingLookupService;
import com.stayfinder.food.client.BookingView;
import com.stayfinder.food.dto.CreateFoodOrderRequest;
import com.stayfinder.food.dto.FoodOrderResponse;
import com.stayfinder.food.dto.OrderItemRequest;
import com.stayfinder.food.dto.UpdateOrderStatusRequest;
import com.stayfinder.food.entity.FoodOrder;
import com.stayfinder.food.entity.MenuItem;
import com.stayfinder.food.entity.OrderItem;
import com.stayfinder.food.entity.OrderStatus;
import com.stayfinder.food.event.FoodEventPublisher;
import com.stayfinder.food.event.FoodOrderPlacedEvent;
import com.stayfinder.food.exception.BookingDependencyException;
import com.stayfinder.food.exception.InvalidOrderException;
import com.stayfinder.food.exception.ResourceNotFoundException;
import com.stayfinder.food.mapper.FoodMapper;
import com.stayfinder.food.repository.FoodOrderRepository;
import com.stayfinder.food.repository.MenuItemRepository;
import com.stayfinder.food.security.CurrentUser;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FoodOrderService {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderService.class);

    private final FoodOrderRepository foodOrderRepository;
    private final MenuItemRepository menuItemRepository;
    private final BookingLookupService bookingLookupService;
    private final FoodEventPublisher foodEventPublisher;

    public FoodOrderService(
            FoodOrderRepository foodOrderRepository,
            MenuItemRepository menuItemRepository,
            BookingLookupService bookingLookupService,
            FoodEventPublisher foodEventPublisher
    ) {
        this.foodOrderRepository = foodOrderRepository;
        this.menuItemRepository = menuItemRepository;
        this.bookingLookupService = bookingLookupService;
        this.foodEventPublisher = foodEventPublisher;
    }

    @Transactional
    public FoodOrderResponse create(CreateFoodOrderRequest request, CurrentUser currentUser) {
        BookingView booking = validateBooking(request, currentUser);

        FoodOrder order = new FoodOrder();
        order.setUserId(currentUser.userId());
        order.setHotelId(request.hotelId());
        order.setBookingId(booking.id());
        order.setRoomNumber(booking.roomNumber());
        order.setStatus(OrderStatus.PLACED);

        for (OrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemRequest.menuItemId()));
            if (!menuItem.getHotelId().equals(request.hotelId())) {
                throw new InvalidOrderException("Menu item does not belong to this hotel");
            }
            if (!menuItem.isAvailable()) {
                throw new InvalidOrderException("Menu item is not available: " + menuItem.getName());
            }
            OrderItem item = new OrderItem();
            item.setMenuItemId(menuItem.getId());
            item.setItemName(menuItem.getName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(menuItem.getPrice());
            order.addItem(item);
        }

        foodOrderRepository.save(order);
        foodEventPublisher.publishFoodOrderPlaced(toPlacedEvent(order, currentUser));
        log.info("Food order created id={} bookingId={} userId={}", order.getId(), booking.id(), currentUser.userId());
        return FoodMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<FoodOrderResponse> myOrders(CurrentUser currentUser) {
        return foodOrderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.userId()).stream()
                .map(FoodMapper::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FoodOrderResponse> hotelOrders(Long hotelId) {
        return foodOrderRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(FoodMapper::toOrderResponse)
                .toList();
    }

    @Transactional
    public FoodOrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        FoodOrder order = foodOrderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Food order not found: " + orderId));
        validateTransition(order.getStatus(), request.status());
        order.setStatus(request.status());
        log.info("Food order status updated id={} status={}", orderId, request.status());
        return FoodMapper.toOrderResponse(order);
    }

    private BookingView validateBooking(CreateFoodOrderRequest request, CurrentUser currentUser) {
        BookingView booking;
        try {
            log.info("Feign validation request bookingId={}", request.bookingId());
            booking = bookingLookupService.getBooking(request.bookingId());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Booking not found: " + request.bookingId());
        } catch (FeignException.Forbidden ex) {
            throw new AccessDeniedException("Booking does not belong to you");
        } catch (BookingDependencyException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("Booking Service call failed status={}", ex.status());
            throw new BookingDependencyException("Booking Service is unavailable");
        }

        if (!currentUser.isStaff() && !currentUser.userId().equals(booking.userId())) {
            throw new AccessDeniedException("Booking does not belong to you");
        }
        if (!"CONFIRMED".equals(booking.status())) {
            throw new InvalidOrderException("Food orders require a CONFIRMED booking");
        }
        if (!request.hotelId().equals(booking.hotelId())) {
            throw new InvalidOrderException("Booking does not belong to this hotel");
        }
        if (request.roomNumber() != null
                && !request.roomNumber().isBlank()
                && !request.roomNumber().equals(booking.roomNumber())) {
            throw new InvalidOrderException("Room number does not match the booking");
        }
        return booking;
    }

    private FoodOrderPlacedEvent toPlacedEvent(FoodOrder order, CurrentUser currentUser) {
        String email = currentUser.email() == null ? "unknown@stayfinder.local" : currentUser.email();
        return new FoodOrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                email,
                order.getHotelId(),
                order.getBookingId(),
                order.getRoomNumber(),
                order.getItems().size(),
                Instant.now()
        );
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            throw new InvalidOrderException("Order is already " + next);
        }
        boolean allowed = (current == OrderStatus.PLACED && next == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && next == OrderStatus.DELIVERED);
        if (!allowed) {
            throw new InvalidOrderException("Cannot change status from " + current + " to " + next);
        }
    }
}
