package com.myapp.aqdeeha.modules.catalog.repository;

import com.myapp.aqdeeha.modules.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Case-insensitive thanks to the utf8mb4_unicode_ci collation. */
    boolean existsByName(String name);

    List<Category> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<Category> findAllByOrderBySortOrderAscIdAsc();
}
