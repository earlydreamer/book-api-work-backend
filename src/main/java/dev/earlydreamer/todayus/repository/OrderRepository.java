package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.OrderEntity;
import dev.earlydreamer.todayus.entity.OrderStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

	Optional<OrderEntity> findTopBySnapshot_IdOrderByCreatedAtDesc(Long snapshotId);

	boolean existsBySnapshot_IdAndStatusIn(Long snapshotId, Collection<OrderStatus> statuses);

	Optional<OrderEntity> findBySweetbookOrderUid(String sweetbookOrderUid);
}
