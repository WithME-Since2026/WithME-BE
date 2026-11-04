package yooze.withme.domain.group.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupLocation;

public interface GroupLocationRepository extends JpaRepository<GroupLocation, Long> {

    Optional<GroupLocation> findByGroupId(Long groupId);

    @Modifying
    @Query("delete from GroupLocation l where l.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
