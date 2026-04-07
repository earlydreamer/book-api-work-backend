package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.SweetbookBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SweetbookBookRepository extends JpaRepository<SweetbookBookEntity, Long> {

	java.util.Optional<SweetbookBookEntity> findBySnapshot_Id(Long snapshotId);
}
