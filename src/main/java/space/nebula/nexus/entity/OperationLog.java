package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Entity for persistent storage of operation logs.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "sys_operation_log")
@SQLDelete(sql = "UPDATE sys_operation_log SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class OperationLog extends BaseEntity {

    @Column(name = "username")
    private String username;

    @Column(name = "description")
    private String description;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "request_method")
    private String requestMethod;

    @Column(name = "request_url")
    private String requestUrl;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "status")
    private Integer status; // 1: Success, 0: Failure

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "trace_id")
    private String traceId;
}
