package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FriendLinkMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FriendLink toEntity(FriendLinkRequest request);

    FriendLinkResponse toResponse(FriendLink friendLink);

    List<FriendLinkResponse> toResponseList(List<FriendLink> friendLinks);
}
