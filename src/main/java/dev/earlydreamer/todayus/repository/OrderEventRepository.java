package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

	boolean existsByDedupeKey(String dedupeKey);
}
