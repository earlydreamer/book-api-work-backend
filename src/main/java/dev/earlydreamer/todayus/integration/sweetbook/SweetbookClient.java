package dev.earlydreamer.todayus.integration.sweetbook;

import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.FinalizeBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoResult;
import java.util.Map;

public interface SweetbookClient {

	CreateBookResult createBook(CreateBookCommand command);

	UploadPhotoResult uploadPhoto(String bookUid, UploadPhotoCommand command);

	void createCover(String bookUid, String templateUid, Map<String, Object> parameters);

	void createContent(String bookUid, String templateUid, Map<String, Object> parameters, String breakBefore);

	FinalizeBookResult finalizeBook(String bookUid);

	CreateOrderResult createOrder(CreateOrderCommand command);
}
