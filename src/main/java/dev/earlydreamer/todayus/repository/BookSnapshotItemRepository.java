package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.BookSnapshotItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookSnapshotItemRepository extends JpaRepository<BookSnapshotItemEntity, Long> {
}
