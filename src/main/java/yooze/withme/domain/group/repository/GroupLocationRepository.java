package yooze.withme.domain.group.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.group.entity.GroupLocation;

public interface GroupLocationRepository extends JpaRepository<GroupLocation, Long> {

    Optional<GroupLocation> findByGroupId(Long groupId);
}
