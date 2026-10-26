package vn.xuandat.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.xuandat.backend.enums.PermissionCode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 80)
    private PermissionCode code;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}

