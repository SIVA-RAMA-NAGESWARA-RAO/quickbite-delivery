package com.quickbite.order.repository;

import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status);

    @Query("select count(o) from Order o where o.restaurantId = :restaurantId and o.status in :statuses")
    long countByRestaurantAndStatusIn(@Param("restaurantId") Long restaurantId,
                                       @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select count(o) from Order o where o.restaurantId = :restaurantId and o.status in :statuses "
            + "and o.createdAt >= :start and o.createdAt < :end")
    long countByRestaurantAndStatusInAndCreatedAtBetween(@Param("restaurantId") Long restaurantId,
                                                          @Param("statuses") Collection<OrderStatus> statuses,
                                                          @Param("start") Instant start,
                                                          @Param("end") Instant end);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.restaurantId = :restaurantId and o.status in :statuses")
    BigDecimal sumRevenueByRestaurantAndStatusIn(@Param("restaurantId") Long restaurantId,
                                                  @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.restaurantId = :restaurantId and o.status in :statuses "
            + "and o.createdAt >= :start and o.createdAt < :end")
    BigDecimal sumRevenueByRestaurantAndStatusInAndCreatedAtBetween(@Param("restaurantId") Long restaurantId,
                                                                     @Param("statuses") Collection<OrderStatus> statuses,
                                                                     @Param("start") Instant start,
                                                                     @Param("end") Instant end);
}
