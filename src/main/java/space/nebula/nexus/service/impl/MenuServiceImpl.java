package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Menu;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.mapper.MenuMapper;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;
import space.nebula.nexus.repository.MenuRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IMenuService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

	private final MenuRepository menuRepository;
	private final UserRepository userRepository;
	private final MenuMapper menuMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<MenuResponse>> retrieveFullMenuTree() {
		List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
		return ApiResponse.success(buildHierarchy(menuMapper.toResponseList(allMenus), 0L));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.NAVIGATION, allEntries = true)
	@LogOperation("Create Menu")
	public ApiResponse<MenuResponse> createMenu(MenuRequest request) {
		Menu newMenu = menuMapper.toEntity(request);
		if (newMenu.getParentId() == null) {
			newMenu.setParentId(0L);
		}
		menuRepository.save(newMenu);
		log.info("Created new menu item: {}", newMenu.getName());
		return ApiResponse.success("Menu item created successfully", menuMapper.toResponse(newMenu));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.NAVIGATION, allEntries = true)
	@LogOperation("Update Menu")
	public ApiResponse<MenuResponse> updateMenu(Long id, MenuRequest request) {
		Menu existingMenu = menuRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Menu", "id", id));

		menuMapper.updateEntity(existingMenu, request);
		menuRepository.save(existingMenu);

		log.info("Updated menu item: {}", existingMenu.getName());
		return ApiResponse.success("Menu item updated successfully", menuMapper.toResponse(existingMenu));
	}

	@Override
	@Transactional
	@CacheEvict(value = CacheConstants.NAVIGATION, allEntries = true)
	@LogOperation("Delete Menu")
	public ApiResponse<Void> deleteMenu(Long id) {
		if (!menuRepository.existsById(id)) {
			throw new ResourceNotFoundException("Menu", "id", id);
		}
		menuRepository.deleteById(id);
		log.info("Purged menu item ID: {}", id);
		return ApiResponse.success("Menu item deleted successfully", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<MenuResponse>> retrieveAuthenticatedUserMenuTree() {
		String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
		User currentUser = userRepository.findByUsername(authenticatedUsername)
				.orElseThrow(() -> new BusinessException("Authenticated user not found"));

		Set<Menu> userAuthorizedMenus = currentUser.getRoles().stream().flatMap(role -> role.getMenus().stream())
				.collect(Collectors.toSet());

		// Filter only relevant visual types for tree display
		List<MenuResponse> localizedMenuResponses = menuMapper.toResponseList(userAuthorizedMenus.stream()
				.filter(menu -> menu.getType() < 2).sorted((m1, m2) -> m1.getSortOrder().compareTo(m2.getSortOrder()))
				.collect(Collectors.toList()));

		return ApiResponse.success(buildHierarchy(localizedMenuResponses, 0L));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.NAVIGATION, key = CacheConstants.PUBLIC_TREE_KEY)
	public ApiResponse<List<MenuResponse>> retrievePublicNavigationMenuTree() {
		List<Menu> publicVisibleMenus = menuRepository.findByIsPublicTrueAndIsVisibleTrueOrderBySortOrderAsc();
		List<MenuResponse> publicResponses = menuMapper.toResponseList(publicVisibleMenus);
		return ApiResponse.success(buildHierarchy(publicResponses, 0L));
	}

	private List<MenuResponse> buildHierarchy(List<MenuResponse> flatMenus, Long rootParentId) {
		Map<Long, List<MenuResponse>> childrenByParentMap = flatMenus.stream()
				.collect(Collectors.groupingBy(MenuResponse::getParentId));

		flatMenus.forEach(menu -> menu.setChildren(childrenByParentMap.getOrDefault(menu.getId(), new ArrayList<>())));

		return childrenByParentMap.getOrDefault(rootParentId, new ArrayList<>());
	}
}
