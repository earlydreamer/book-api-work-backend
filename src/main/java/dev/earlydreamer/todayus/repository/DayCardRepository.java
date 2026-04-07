package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.DayCardEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayCardRepository extends JpaRepository<DayCardEntity, Long> {

	@EntityGraph(attributePaths = {"entries", "entries.user"})
	Optional<DayCardEntity> findByCouple_IdAndLocalDate(String coupleId, LocalDate localDate);

	@EntityGraph(attributePaths = {"entries", "entries.user"})
	List<DayCardEntity> findByCouple_IdOrderByLocalDateDesc(String coupleId);

	@EntityGraph(attributePaths = {"entries", "entries.user"})
	List<DayCardEntity> findByCouple_IdAndLocalDateBetweenOrderByLocalDateDesc(
		String coupleId,
		LocalDate startDate,
		LocalDate endDate
	);
}
