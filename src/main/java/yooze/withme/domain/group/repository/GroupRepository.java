package yooze.withme.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
