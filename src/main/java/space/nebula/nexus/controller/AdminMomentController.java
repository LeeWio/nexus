package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

@RestController
@RequestMapping("/api/v1/admin/moments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Moment Management", description = "Endpoints for managing microblogs/moments")
public class AdminMomentController {

    private final IMomentService momentService;

    @GetMapping
    @Operation(summary = "Get all moments (paginated)")
    public ApiResponse<PageResult<MomentResponse>> getAllMoments(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return momentService.getAdminMoments(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get moment by ID")
    public ApiResponse<MomentResponse> getMomentById(@PathVariable Long id) {
        return momentService.getMomentById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new moment")
    public ApiResponse<MomentResponse> createMoment(@Valid @RequestBody MomentRequest request) {
        return momentService.createMoment(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a moment")
    public ApiResponse<MomentResponse> updateMoment(@PathVariable Long id, @Valid @RequestBody MomentRequest request) {
        return momentService.updateMoment(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a moment")
    public ApiResponse<Void> deleteMoment(@PathVariable Long id) {
        return momentService.deleteMoment(id);
    }
}
