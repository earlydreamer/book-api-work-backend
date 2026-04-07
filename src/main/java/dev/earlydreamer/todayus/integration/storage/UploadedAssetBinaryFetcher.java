package dev.earlydreamer.todayus.integration.storage;

import dev.earlydreamer.todayus.entity.UploadedAssetEntity;

public interface UploadedAssetBinaryFetcher {

	FetchedUploadedAsset fetch(UploadedAssetEntity uploadedAsset);
}
