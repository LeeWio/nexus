package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_notification")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String type; // e.g., "SYSTEM", "COMMENT", "WIKI", "HEALTH_CHECK"

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(length = 500)
    private String link; // Optional link to the relevant page
}
