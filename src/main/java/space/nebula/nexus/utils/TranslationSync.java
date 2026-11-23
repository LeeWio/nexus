package space.nebula.nexus.utils;

import java.util.Iterator;
import java.util.Map;

import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.CategoryTranslation;
import space.nebula.nexus.entity.LifeListCategory;
import space.nebula.nexus.entity.LifeListCategoryTranslation;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostTranslation;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.TagTranslation;
import space.nebula.nexus.payload.request.CategoryTranslationRequest;
import space.nebula.nexus.payload.request.LifeListCategoryTranslationRequest;
import space.nebula.nexus.payload.request.PostTranslationRequest;
import space.nebula.nexus.payload.request.TagTranslationRequest;

public final class TranslationSync {
	private TranslationSync() {
	}

	public static void category(Category target, Map<String, CategoryTranslationRequest> values) {
		if (values == null)
			return;
		Map<String, CategoryTranslation> translations = target.getTranslations();
		values.forEach((locale, value) -> {
			CategoryTranslation translation = translations.get(locale);
			if (translation == null) {
				translation = new CategoryTranslation();
				translation.setCategory(target);
				translation.setLocale(locale);
				translations.put(locale, translation);
			}
			translation.setName(value.name());
			translation.setDescription(value.description());
		});
		removeMissing(translations, values);
	}

	public static void tag(Tag target, Map<String, TagTranslationRequest> values) {
		if (values == null)
			return;
		Map<String, TagTranslation> translations = target.getTranslations();
		values.forEach((locale, value) -> {
			TagTranslation translation = translations.get(locale);
			if (translation == null) {
				translation = new TagTranslation();
				translation.setTag(target);
				translation.setLocale(locale);
				translations.put(locale, translation);
			}
			translation.setName(value.name());
		});
		removeMissing(translations, values);
	}

	public static void lifeListCategory(LifeListCategory target,
			Map<String, LifeListCategoryTranslationRequest> values) {
		if (values == null)
			return;
		Map<String, LifeListCategoryTranslation> translations = target.getTranslations();
		values.forEach((locale, value) -> {
			LifeListCategoryTranslation translation = translations.get(locale);
			if (translation == null) {
				translation = new LifeListCategoryTranslation();
				translation.setCategory(target);
				translation.setLocale(locale);
				translations.put(locale, translation);
			}
			translation.setName(value.name());
		});
		removeMissing(translations, values);
	}

	public static void post(Post target, Map<String, PostTranslationRequest> values) {
		if (values == null)
			return;
		Map<String, PostTranslation> translations = target.getTranslations();
		values.forEach((locale, value) -> {
			if (isSourceLocale(locale))
				return;
			PostTranslation translation = translations.get(locale);
			if (translation == null) {
				translation = new PostTranslation();
				translation.setPost(target);
				translation.setLocale(locale);
				translations.put(locale, translation);
			}
			translation.setTitle(value.title());
			translation.setSummary(value.summary());
			translation.setContent(value.content());
			translation.setAutoSummary(value.autoSummary());
			translation.setToc(value.toc());
		});
		Iterator<Map.Entry<String, PostTranslation>> entries = translations.entrySet().iterator();
		while (entries.hasNext()) {
			String locale = entries.next().getKey();
			if (isSourceLocale(locale) || !values.containsKey(locale)) {
				entries.remove();
			}
		}
	}

	private static boolean isSourceLocale(String locale) {
		return locale != null && locale.toLowerCase().startsWith("zh");
	}

	private static void removeMissing(Map<String, ?> translations, Map<String, ?> values) {
		translations.keySet().removeIf(locale -> !values.containsKey(locale));
	}
}
