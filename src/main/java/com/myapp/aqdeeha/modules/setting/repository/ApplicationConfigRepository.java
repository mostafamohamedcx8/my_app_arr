package com.myapp.aqdeeha.modules.setting.repository;

import com.myapp.aqdeeha.modules.setting.entity.ApplicationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationConfigRepository extends JpaRepository<ApplicationConfig, Long> {

    Optional<ApplicationConfig> findByKey(String key);

    List<ApplicationConfig> findAllByOrderByKeyAsc();
}
