package vn.xuandat.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.xuandat.backend.entity.Permission;

import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

}
