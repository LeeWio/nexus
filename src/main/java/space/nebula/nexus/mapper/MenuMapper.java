package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.nebula.nexus.entity.Menu;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuMapper {
    
    @Mapping(target = "children", ignore = true)
    MenuResponse toResponse(Menu menu);
    
    List<MenuResponse> toResponseList(List<Menu> menus);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Menu toEntity(MenuRequest request);
}
