package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sys_visit_log")
@SQLDelete(sql = "UPDATE sys_visit_log SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class VisitLog extends BaseEntity {

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 50)
    private String browser;

    @Column(length = 50)
    private String os;

    @Column(name = "request_url", nullable = false, length = 255)
    private String requestUrl;

    @Column(length = 255)
    private String referer;

    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;
}
