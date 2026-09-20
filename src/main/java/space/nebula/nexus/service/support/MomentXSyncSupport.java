package space.nebula.nexus.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.MomentXSyncResponse;
import space.nebula.nexus.service.IXSyncService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Attaches X sync metadata onto moment API responses.
 */
@Component
@RequiredArgsConstructor
public class MomentXSyncSupport {

	private final IXSyncService xSyncService;

	public MomentResponse withSync(MomentResponse response) {
		if (response == null || response.id() == null) {
			return response;
		}
		return copyWithSync(response, xSyncService.findByMomentId(response.id()));
	}

	public List<MomentResponse> withSyncs(Collection<MomentResponse> responses) {
		if (responses == null || responses.isEmpty()) {
			return List.of();
		}
		List<Long> momentIds = responses.stream().map(MomentResponse::id).filter(Objects::nonNull).distinct().toList();
		Map<Long, MomentXSyncResponse> syncs = xSyncService.findByMomentIds(momentIds);
		return responses.stream().map(response -> copyWithSync(response, syncs.get(response.id()))).toList();
	}

	private static MomentResponse copyWithSync(MomentResponse response, MomentXSyncResponse sync) {
		return new MomentResponse(response.id(), response.content(), response.stockSymbol(), response.likesCount(),
				response.commentsCount(), response.visibility(), response.authorName(), response.authorAvatar(),
				response.images(), response.topics(), response.createdAt(), response.updatedAt(), sync);
	}
}
