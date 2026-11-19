package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.payload.response.AssetUsageResponse;
import space.nebula.nexus.payload.response.AssetUsageResponse.Kind;
import space.nebula.nexus.payload.response.AssetUsageResponse.Location;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.PostRevisionRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.projection.AssetReferenceText;
import space.nebula.nexus.repository.projection.PostAssetText;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class AssetUsageService {
	private static final int LOCATION_LIMIT = 25;
	private final PostRepository posts;
	private final PostRevisionRepository revisions;
	private final CommentRepository comments;
	private final UserRepository users;
	private final ProjectRepository projects;
	private final PostSeriesRepository series;
	private final FriendLinkRepository friendLinks;
	private final FileRepository files;

	public AssetUsageResponse getUsage(FileMetadata file) {
		var revisionLocations = revisions
				.findAssetReferences(file.getFileName(), thumbnailObjectName(file), Limit.unlimited()).stream()
				.map(revision -> new Location(Kind.ARTICLE_REVISION, revision.id(),
						revision.title() + " · v" + revision.versionNumber()));
		var references = Stream.concat(loadFor(file).locations(file), revisionLocations).toList();
		return new AssetUsageResponse(references.size(),
				List.copyOf(references.subList(0, Math.min(references.size(), LOCATION_LIMIT))));
	}

	public boolean isInUse(FileMetadata file) {
		return loadFor(file).isInUse(file) || isInArticleHistory(file);
	}

	public boolean isInArticleHistory(FileMetadata file) {
		return !revisions.findAssetReferences(file.getFileName(), thumbnailObjectName(file), Limit.of(1)).isEmpty();
	}

	public Snapshot loadSnapshot() {
		return load(null, null);
	}

	private Snapshot loadFor(FileMetadata file) {
		String name = file.getFileName();
		String thumbnail = thumbnailObjectName(file);
		int dot = name.lastIndexOf('.');
		String stem = dot > 0 ? name.substring(0, dot) : name;
		// Older thumbnail names may have no fragment in common with the original.
		String term = thumbnail == null ? name : thumbnail.contains(stem) ? stem : null;
		return load(term, file.getId());
	}

	private Snapshot load(String term, Long fileId) {
		return new Snapshot(posts.findAssetReferenceText(term), comments.findAssetReferenceText(term),
				users.findAssetReferenceText(term), projects.findAssetReferenceText(term),
				series.findAssetReferenceText(term), friendLinks.findAssetReferenceText(term),
				files.findMomentAssetReferences(fileId).stream()
						.collect(groupingBy(FileRepository.MomentAssetReference::getFileId,
								mapping(FileRepository.MomentAssetReference::getMomentId, toList()))));
	}

	public static String thumbnailObjectName(FileMetadata file) {
		String location = file.getThumbnailUrl();
		if (location == null || location.isBlank()) {
			return null;
		}
		int end = location.length();
		for (char separator : new char[]{'?', '#'}) {
			int index = location.indexOf(separator);
			if (index >= 0) {
				end = Math.min(end, index);
			}
		}
		String path = location.substring(0, end);
		String name = path.substring(path.lastIndexOf('/') + 1);
		return name.isEmpty() ? null : name;
	}

	public record Snapshot(List<PostAssetText> posts, List<AssetReferenceText> comments, List<AssetReferenceText> users,
			List<AssetReferenceText> projects, List<AssetReferenceText> series, List<AssetReferenceText> friendLinks,
			Map<Long, List<Long>> moments) {

		public boolean isInUse(FileMetadata file) {
			return locations(file).findAny().isPresent();
		}

		private Stream<Location> locations(FileMetadata file) {
			String name = file.getFileName();
			String thumbnail = thumbnailObjectName(file);
			Predicate<String> matches = value -> value != null
					&& (value.contains(name) || thumbnail != null && value.contains(thumbnail));
			Stream<Location> articleUses = posts.stream().flatMap(post -> {
				var locations = Stream.<Location>builder();
				if (matches.test(post.cover())) {
					locations.add(new Location(Kind.ARTICLE_COVER, post.id(), post.title()));
				}
				if (matches.test(post.content())) {
					locations.add(new Location(Kind.ARTICLE_CONTENT, post.id(), post.title()));
				}
				if (matches.test(post.summary())) {
					locations.add(new Location(Kind.ARTICLE_SUMMARY, post.id(), post.title()));
				}
				return locations.build();
			});
			return Stream.of(
					moments.getOrDefault(file.getId(), List.of()).stream()
							.map(id -> new Location(Kind.MOMENT, id, null)),
					articleUses, matchingLocations(comments, Kind.COMMENT, matches),
					matchingLocations(users, Kind.USER_AVATAR, matches),
					matchingLocations(projects, Kind.PROJECT_COVER, matches),
					matchingLocations(series, Kind.SERIES_COVER, matches),
					matchingLocations(friendLinks, Kind.FRIEND_LINK_AVATAR, matches)).flatMap(stream -> stream);
		}

		private Stream<Location> matchingLocations(List<AssetReferenceText> values, Kind kind,
				Predicate<String> matches) {
			return values.stream().filter(value -> matches.test(value.text()))
					.map(value -> new Location(kind, value.id(), value.label()));
		}
	}
}
