package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.CoupleStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoupleRepository extends JpaRepository<CoupleEntity, String> {

	@Query("""
		select c
		from CoupleEntity c
		join fetch c.creatorUser cu
		left join fetch c.partnerUser pu
		where (cu.id = :userId or pu.id = :userId)
		  and c.status in :statuses
		order by c.createdAt desc
		""")
	List<CoupleEntity> findParticipantCouplesByStatuses(
		@Param("userId") String userId,
		@Param("statuses") Collection<CoupleStatus> statuses
	);

	@Query("""
		select c
		from CoupleEntity c
		join fetch c.creatorUser cu
		left join fetch c.partnerUser pu
		where (cu.id = :userId or pu.id = :userId)
		  and c.status = :status
		order by c.createdAt desc
		""")
	List<CoupleEntity> findParticipantCouplesByStatus(
		@Param("userId") String userId,
		@Param("status") CoupleStatus status
	);

	@Query("""
		select c
		from CoupleEntity c
		join fetch c.creatorUser cu
		left join fetch c.partnerUser pu
		where c.inviteCode = :inviteCode
		  and c.status = :status
		""")
	Optional<CoupleEntity> findByInviteCodeAndStatus(
		@Param("inviteCode") String inviteCode,
		@Param("status") CoupleStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select c
		from CoupleEntity c
		join fetch c.creatorUser cu
		left join fetch c.partnerUser pu
		where c.inviteCode = :inviteCode
		  and c.status = :status
		""")
	Optional<CoupleEntity> findByInviteCodeAndStatusForUpdate(
		@Param("inviteCode") String inviteCode,
		@Param("status") CoupleStatus status
	);

	boolean existsByInviteCode(String inviteCode);
}
