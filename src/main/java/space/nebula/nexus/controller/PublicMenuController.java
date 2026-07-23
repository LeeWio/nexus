package space.nebula.nexus.controller;

import cn.hutool.core.lang.tree.Tree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IMenuService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/menus")
@RequiredArgsConstructor
@Tag(name = "Public Navigation", description = "Public endpoints for dynamic website navigation")
public class PublicMenuController {

	private final IMenuService menuService;

	@GetMapping("/navigation")
	@Operation(summary = "Retrieve the public navigation menu tree")
	public ApiResponse<List<Tree<Long>>> retrieveNavigation() {
		return menuService.retrievePublicNavigationMenuTree();
	}
}
