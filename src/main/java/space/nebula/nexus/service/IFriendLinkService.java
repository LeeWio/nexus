package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;

public interface IFriendLinkService {

    ApiResponse<PageResult<FriendLinkResponse>> retrieveAdminFriendLinks(Pageable pageable);

    ApiResponse<FriendLinkResponse> retrieveFriendLinkById(Long id);

    ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request);

    ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request);

    ApiResponse<Void> deleteFriendLink(Long id);

    /**
     * Publicly accessible list of approved friend links.
     */
    ApiResponse<List<FriendLinkResponse>> retrievePublicFriendLinks();

    /**
     * Public submission of a new friend link application.
     */
    ApiResponse<Void> applyForFriendLink(FriendLinkRequest request);

    /**
     * Admin moderation of a friend link application.
     */
    ApiResponse<Void> moderateFriendLink(Long id, FriendLinkStatus status);
}
