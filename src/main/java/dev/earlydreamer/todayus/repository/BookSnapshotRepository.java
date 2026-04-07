package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.BookSnapshotEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookSnapshotRepository extends JpaRepository<BookSnapshotEntity, Long> {

	Optional<BookSnapshotEntity> findTopByCouple_IdOrderByCreatedAtDesc(String coupleId);

	@Query("""
		select distinct s
		from BookSnapshotEntity s
		left join fetch s.items i
		left join fetch i.photoAsset pa
		where s.id = :id
		""")
	Optional<BookSnapshotEntity> findByIdWithItems(@Param("id") Long id);

	boolean existsByCouple_IdAndStatusIn(String coupleId, Collection<BookSnapshotStatus> statuses);
}
