package space.nebula.nexus.payload.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuResponse {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String permission;
    private Integer type;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private List<MenuResponse> children;
}
