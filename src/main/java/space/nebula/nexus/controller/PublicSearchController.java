package space.nebula.nexus.controller;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IPostSearchService;

@Tag(name = "Public Search", description = "Public Elasticsearch APIs")
@RestController
@RequestMapping("/api/v1/public/search")
@RequiredArgsConstructor
public class PublicSearchController {

    private final IPostSearchService postSearchService;

    @Operation(summary = "Search Posts", description = "Full-text search on posts using Elasticsearch")
    @GetMapping("/posts")
    public ApiResponse<PageResult<PostDocument>> searchPosts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postSearchService.searchPosts(keyword, pageable);
    }
}
