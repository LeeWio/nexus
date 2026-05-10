package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Menu;
import space.nebula.nexus.entity.Role;
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
    public ApiResponse<List<MenuResponse>> getMenuTree() {
        List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        return ApiResponse.success(buildTree(menuMapper.toResponseList(allMenus), 0L));
    }

    @Override
    @Transactional
    @LogOperation("Create Menu")
    public ApiResponse<MenuResponse> createMenu(MenuRequest request) {
        Menu menu = menuMapper.toEntity(request);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        menuRepository.save(menu);
        log.info("Created menu: {}", menu.getName());
        return ApiResponse.success("Menu created successfully", menuMapper.toResponse(menu));
    }

    @Override
    @Transactional
    @LogOperation("Update Menu")
    public ApiResponse<MenuResponse> updateMenu(Long id, MenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Menu not found"));
        
        menu.setName(request.name());
        menu.setParentId(request.parentId() != null ? request.parentId() : 0L);
        menu.setPath(request.path());
        menu.setPermission(request.permission());
        menu.setType(request.type());
        menu.setIcon(request.icon());
        menu.setSortOrder(request.sortOrder());
        
        menuRepository.save(menu);
        log.info("Updated menu: {}", menu.getName());
        return ApiResponse.success("Menu updated successfully", menuMapper.toResponse(menu));
    }

    @Override
    @Transactional
    @LogOperation("Delete Menu")
    public ApiResponse<Void> deleteMenu(Long id) {
        if (!menuRepository.existsById(id)) {
            throw new BusinessException(404, "Menu not found");
        }
        // Basic check for children could be added here
        menuRepository.deleteById(id);
        log.info("Deleted menu id: {}", id);
        return ApiResponse.success("Menu deleted successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<MenuResponse>> getCurrentUserMenuTree() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        Set<Menu> userMenus = user.getRoles().stream()
                .flatMap(role -> role.getMenus().stream())
                .collect(Collectors.toSet());

        // Filter only directory and menu types for tree display, exclude button permissions
        List<MenuResponse> responses = menuMapper.toResponseList(
                userMenus.stream()
                        .filter(m -> m.getType() < 2)
                        .sorted((m1, m2) -> m1.getSortOrder().compareTo(m2.getSortOrder()))
                        .collect(Collectors.toList())
        );

        return ApiResponse.success(buildTree(responses, 0L));
    }

    private List<MenuResponse> buildTree(List<MenuResponse> menus, Long parentId) {
        Map<Long, List<MenuResponse>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(MenuResponse::getParentId));
        
        menus.forEach(m -> m.setChildren(parentMap.getOrDefault(m.getId(), new ArrayList<>())));
        
        return parentMap.getOrDefault(parentId, new ArrayList<>());
    }
}
