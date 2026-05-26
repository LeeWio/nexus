package space.nebula.nexus.mapper;

import cn.hutool.core.util.StrUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.enums.WebhookEvent;
import space.nebula.nexus.payload.request.WebhookRequest;
import space.nebula.nexus.payload.response.WebhookResponse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface WebhookMapper {

	@Mapping(target = "events", source = "events", qualifiedByName = "eventsToString")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	Webhook toEntity(WebhookRequest request);

	@Mapping(target = "events", source = "events", qualifiedByName = "eventsToString")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@org.mapstruct.MappingTarget Webhook webhook, WebhookRequest request);

	@Mapping(target = "events", source = "events", qualifiedByName = "stringToEvents")
	WebhookResponse toResponse(Webhook webhook);

	@Named("eventsToString")
	default String eventsToString(List<WebhookEvent> events) {
		if (events == null || events.isEmpty()) return "";
		return events.stream().map(Enum::name).collect(Collectors.joining(","));
	}

	@Named("stringToEvents")
	default List<WebhookEvent> stringToEvents(String events) {
		if (StrUtil.isBlank(events)) return List.of();
		return Arrays.stream(events.split(","))
				.map(String::trim)
				.filter(StrUtil::isNotBlank)
				.map(WebhookEvent::valueOf)
				.collect(Collectors.toList());
	}
}
