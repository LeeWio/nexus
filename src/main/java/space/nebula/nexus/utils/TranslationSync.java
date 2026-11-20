package space.nebula.nexus.utils;

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
		target.getTranslations().clear();
		values.forEach((locale, value) -> {
			CategoryTranslation translation = new CategoryTranslation();
			translation.setCategory(target);
			translation.setLocale(locale);
			translation.setName(value.name());
			translation.setDescription(value.description());
			target.getTranslations().put(locale, translation);
		});
	}

	public static void tag(Tag target, Map<String, TagTranslationRequest> values) {
		if (values == null)
			return;
		target.getTranslations().clear();
		values.forEach((locale, value) -> {
			TagTranslation translation = new TagTranslation();
			translation.setTag(target);
			translation.setLocale(locale);
			translation.setName(value.name());
			target.getTranslations().put(locale, translation);
		});
	}

	public static void lifeListCategory(LifeListCategory target,
			Map<String, LifeListCategoryTranslationRequest> values) {
		if (values == null)
			return;
		target.getTranslations().clear();
		values.forEach((locale, value) -> {
			LifeListCategoryTranslation translation = new LifeListCategoryTranslation();
			translation.setCategory(target);
			translation.setLocale(locale);
			translation.setName(value.name());
			target.getTranslations().put(locale, translation);
		});
	}

	public static void post(Post target, Map<String, PostTranslationRequest> values) {
		if (values == null)
			return;
		target.getTranslations().clear();
		values.forEach((locale, value) -> {
			PostTranslation translation = new PostTranslation();
			translation.setPost(target);
			translation.setLocale(locale);
			translation.setTitle(value.title());
			translation.setSummary(value.summary());
			translation.setContent(value.content());
			translation.setAutoSummary(value.autoSummary());
			translation.setToc(value.toc());
			target.getTranslations().put(locale, translation);
		});
	}
}
