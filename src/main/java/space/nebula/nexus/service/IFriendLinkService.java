package space.nebula.nexus.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;

public interface IFriendLinkService {
    
    // Admin methods
    ApiResponse<PageResult<FriendLinkResponse>> getAdminFriendLinks(Pageable pageable);
    ApiResponse<FriendLinkResponse> getFriendLinkById(Long id);
    ApiResponse<FriendLinkResponse> createFriendLink(FriendLinkRequest request);
    ApiResponse<FriendLinkResponse> updateFriendLink(Long id, FriendLinkRequest request);
    ApiResponse<Void> deleteFriendLink(Long id);

    // Public methods
    ApiResponse<List<FriendLinkResponse>> getPublicFriendLinks();
}
