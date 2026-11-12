package com.myapp.aqdeeha.modules.admin.repository;

import com.myapp.aqdeeha.modules.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Admin.AdminRole role);
}
