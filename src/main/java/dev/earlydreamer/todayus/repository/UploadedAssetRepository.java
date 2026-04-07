package dev.earlydreamer.todayus.repository;

import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedAssetRepository extends JpaRepository<UploadedAssetEntity, String> {

	Optional<UploadedAssetEntity> findByIdAndOwnerUser_Id(String id, String ownerUserId);
}
