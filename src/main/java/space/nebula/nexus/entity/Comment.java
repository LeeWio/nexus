package space.nebula.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import space.nebula.nexus.enums.CommentStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "blog_comment")
@SQLDelete(sql = "UPDATE blog_comment SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Comment extends BaseEntity {

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id") // Nullable for guestbook
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Comment> children = new ArrayList<>();

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;
}
