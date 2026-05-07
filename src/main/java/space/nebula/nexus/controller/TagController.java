package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.service.ITagService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Tag Management", description = "Endpoints for managing blog tags (Requires ADMIN role)")
public class TagController {

    @Resource
    private ITagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags")
    public ApiResponse<List<TagResponse>> getAllTags() {
        return tagService.getAllTags();
    }

    @PostMapping
    @Operation(summary = "Create a new tag")
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return tagService.createTag(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tag")
    public ApiResponse<TagResponse> updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return tagService.updateTag(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        return tagService.deleteTag(id);
    }
}
