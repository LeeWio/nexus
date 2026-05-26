package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.service.IFriendLinkService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for public friend link operations. Allows users to view approved
 * links and apply for a link exchange.
 */
@Tag(name = "Public Friend Links", description = "Public endpoints for exploring and applying for friend links")
@RestController
@RequestMapping("/api/v1/public/friend-links")
@RequiredArgsConstructor
public class PublicFriendLinkController
{

	private final IFriendLinkService friendLinkService;

	@GetMapping
	@Operation(summary = "Retrieve all friend links", description = "Fetch a list of all approved external links for the blogroll.")
	public ApiResponse<List<FriendLinkResponse>> retrieveFriendLinks()
	{
		return friendLinkService.retrievePublicFriendLinks();
	}

	@PostMapping("/apply")
	@Operation(summary = "Apply for link exchange", description = "Submit a request to add your site to our blogroll. Requires review.")
	@RateLimit(count = 5, time = 1, unit = TimeUnit.HOURS, message = "Link application frequency too high. Please try again later.")
	public ApiResponse<Void> applyForFriendLink(@Valid @RequestBody FriendLinkRequest request)
	{
		return friendLinkService.applyForFriendLink(request);
	}
}
