package dev.earlydreamer.todayus.integration.sweetbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.earlydreamer.todayus.config.SweetbookProperties;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.FinalizeBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoResult;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SweetbookApiClient implements SweetbookClient {

	private final SweetbookProperties sweetbookProperties;
	private final WebClient sweetbookWebClient;
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	public SweetbookApiClient(SweetbookProperties sweetbookProperties, WebClient sweetbookWebClient) {
		this.sweetbookProperties = sweetbookProperties;
		this.sweetbookWebClient = sweetbookWebClient;
	}

	@Override
	public CreateBookResult createBook(CreateBookCommand command) {
		String responseBody = sweetbookWebClient.post()
			.uri("/v1/books")
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.header("Idempotency-Key", command.idempotencyKey())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(writeJson(Map.of(
				"title", command.title(),
				"bookSpecUid", command.bookSpecUid()
			)))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		JsonNode dataNode = requireSuccess(responseBody, "sweetbook_book_create_failed");
		return new CreateBookResult(dataNode.path("bookUid").asText());
	}

	@Override
	public UploadPhotoResult uploadPhoto(String bookUid, UploadPhotoCommand command) {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("file", new NamedByteArrayResource(command.bytes(), command.fileName()))
			.header(HttpHeaders.CONTENT_TYPE, command.contentType());

		String responseBody = sweetbookWebClient.post()
			.uri("/v1/books/{bookUid}/photos", bookUid)
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		JsonNode dataNode = requireSuccess(responseBody, "sweetbook_asset_upload_failed");
		return new UploadPhotoResult(dataNode.path("fileName").asText());
	}

	@Override
	public void createCover(String bookUid, String templateUid, Map<String, Object> parameters) {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("templateUid", templateUid);
		multipartBodyBuilder.part("parameters", writeJson(parameters));

		String responseBody = sweetbookWebClient.post()
			.uri("/v1/books/{bookUid}/cover", bookUid)
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		requireSuccess(responseBody, "sweetbook_cover_create_failed");
	}

	@Override
	public void createContent(String bookUid, String templateUid, Map<String, Object> parameters, String breakBefore) {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("templateUid", templateUid);
		multipartBodyBuilder.part("parameters", writeJson(parameters));

		String responseBody = sweetbookWebClient.post()
			.uri((uriBuilder) -> uriBuilder
				.path("/v1/books/{bookUid}/contents")
				.queryParam("breakBefore", breakBefore)
				.build(bookUid))
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		requireSuccess(responseBody, "sweetbook_contents_create_failed");
	}

	@Override
	public FinalizeBookResult finalizeBook(String bookUid) {
		String responseBody = sweetbookWebClient.post()
			.uri("/v1/books/{bookUid}/finalization", bookUid)
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.retrieve()
			.bodyToMono(String.class)
			.block();

		JsonNode dataNode = requireSuccess(responseBody, "sweetbook_finalization_failed");
		return new FinalizeBookResult(
			dataNode.path("pageCount").asInt(),
			Instant.parse(dataNode.path("finalizedAt").asText())
		);
	}

	@Override
	public CreateOrderResult createOrder(CreateOrderCommand command) {
		String responseBody = sweetbookWebClient.post()
			.uri("/v1/orders")
			.header(HttpHeaders.AUTHORIZATION, bearerToken())
			.header("Idempotency-Key", command.idempotencyKey())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(writeJson(Map.of(
				"items", java.util.List.of(Map.of(
					"bookUid", command.bookUid(),
					"quantity", command.quantity()
				)),
				"shipping", Map.of(
					"recipientName", command.recipientName(),
					"recipientPhone", command.recipientPhone(),
					"postalCode", command.postalCode(),
					"address1", command.address1(),
					"address2", command.address2() == null ? "" : command.address2(),
					"memo", command.shippingMemo() == null ? "" : command.shippingMemo()
				),
				"externalRef", command.externalRef(),
				"externalUserId", command.externalUserId() == null ? "" : command.externalUserId()
			)))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		JsonNode dataNode = requireSuccess(responseBody, "sweetbook_order_create_failed");
		return new CreateOrderResult(
			dataNode.path("orderUid").asText(),
			dataNode.path("orderStatus").asInt(),
			dataNode.path("orderStatusDisplay").asText(),
			Instant.parse(dataNode.path("orderedAt").asText())
		);
	}

	private JsonNode requireSuccess(String responseBody, String errorCode) {
		try {
			JsonNode rootNode = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
			if (!rootNode.path("success").asBoolean(false)) {
				throw new ApiException(
					HttpStatus.BAD_GATEWAY,
					errorCode,
					"Sweetbook 요청이 실패했어요.",
					rootNode.path("message").asText("Sweetbook 응답이 실패로 돌아왔어요.")
				);
			}
			return rootNode.path("data");
		} catch (IOException exception) {
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				errorCode,
				"Sweetbook 응답을 읽을 수 없어요.",
				"Sweetbook 응답 JSON을 해석하지 못했어요."
			);
		}
	}

	private String writeJson(Map<String, Object> value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (IOException exception) {
			throw new IllegalStateException("Sweetbook 요청 JSON을 만들 수 없어요.", exception);
		}
	}

	private String bearerToken() {
		return "Bearer " + sweetbookProperties.apiKey();
	}

	private static final class NamedByteArrayResource extends ByteArrayResource {

		private final String fileName;

		private NamedByteArrayResource(byte[] byteArray, String fileName) {
			super(byteArray);
			this.fileName = fileName;
		}

		@Override
		public String getFilename() {
			return this.fileName;
		}
	}
}
