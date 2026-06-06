package space.nebula.nexus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blog_subscriber")
public class Subscriber extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, ACTIVE, UNSUBSCRIBED

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "unsubscribe_token", length = 100)
    private String unsubscribeToken;
}
