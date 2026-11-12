package com.myapp.aqdeeha.modules.courier.repository;

import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {

    String SEARCH_WHERE = """
            where (:status is null or p.approvalStatus = :status)
              and (:q is null or lower(u.name) like :namePattern escape '\\' or u.phone like :phonePattern)
            """;

    /** Admin list: newest applications first. Pattern params are built by the service. */
    @Query(value = "select new com.myapp.aqdeeha.modules.courier.repository.CourierRow(p, u) "
            + "from CourierProfile p join User u on u.id = p.userId " + SEARCH_WHERE
            + " order by p.createdAt desc, p.userId desc",
            countQuery = "select count(p) from CourierProfile p join User u on u.id = p.userId " + SEARCH_WHERE)
    Page<CourierRow> search(@Param("status") ApprovalStatus status, @Param("q") String q,
                            @Param("namePattern") String namePattern, @Param("phonePattern") String phonePattern,
                            Pageable pageable);

    @Query("select new com.myapp.aqdeeha.modules.courier.repository.CourierRow(p, u) "
            + "from CourierProfile p join User u on u.id = p.userId where p.userId = :id")
    Optional<CourierRow> findRow(@Param("id") Long id);
}
