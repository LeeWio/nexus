package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_link_check_log")
public class LinkCheckLog extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // e.g., "POST", "FRIEND_LINK"

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_title", length = 200)
    private String sourceTitle;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "is_broken", nullable = false)
    private Boolean isBroken = false;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
