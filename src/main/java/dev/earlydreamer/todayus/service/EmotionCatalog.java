package dev.earlydreamer.todayus.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmotionCatalog {

	private static final Map<String, EmotionView> EMOTIONS = Map.of(
		"happy", new EmotionView("😊", "좋아"),
		"loving", new EmotionView("🥰", "다정해"),
		"excited", new EmotionView("🤩", "설레"),
		"moody", new EmotionView("🤔", "생각이 많아"),
		"calm", new EmotionView("🌿", "차분해")
	);

	public EmotionView get(String emotionCode) {
		return EMOTIONS.getOrDefault(emotionCode, EMOTIONS.get("calm"));
	}

	public record EmotionView(String emoji, String label) {
	}
}
