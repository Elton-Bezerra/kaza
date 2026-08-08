package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private String subject;
    private String email;
    @Column(name = "display_name") private String displayName;
    @Column(nullable = false) private OffsetDateTime createdAt;
    @Column(nullable = false) private OffsetDateTime updatedAt;

    protected User() {}
    public User(String subject, String email, String displayName) {
        this.subject = subject; this.email = email; this.displayName = displayName;
    }
    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public void refresh(String email, String displayName) { this.email = email; this.displayName = displayName; }
}
