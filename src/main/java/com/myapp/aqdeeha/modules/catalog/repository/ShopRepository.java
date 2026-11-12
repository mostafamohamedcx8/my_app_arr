package com.myapp.aqdeeha.modules.catalog.repository;

import com.myapp.aqdeeha.modules.catalog.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Soft-deleted shops are excluded automatically (@SQLRestriction on {@link Shop}). */
public interface ShopRepository extends JpaRepository<Shop, Long> {

    String SEARCH_WHERE = """
            where (:categoryId is null or s.categoryId = :categoryId)
              and (:pattern is null or lower(s.name) like :pattern escape '\\' or lower(s.street) like :pattern escape '\\')
            """;

    boolean existsByCategoryId(Long categoryId);

    /** {@code categoryId} null = all categories (admin). Optional search on name or street. */
    @Query(value = "select s from Shop s " + SEARCH_WHERE + " order by s.name asc, s.id asc",
            countQuery = "select count(s) from Shop s " + SEARCH_WHERE)
    Page<Shop> search(@Param("categoryId") Long categoryId, @Param("pattern") String pattern, Pageable pageable);
}
