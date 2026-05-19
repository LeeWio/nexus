package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "sys_menu")
@SQLDelete(sql = "UPDATE sys_menu SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Menu extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String path;

    @Column(length = 100)
    private String permission;

    @Column(nullable = false)
    private Integer type; // 0-目录，1-菜单，2-按钮/API权限

    @Column(length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
}
