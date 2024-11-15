package telproject.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import telproject.entity.RawData;

public interface RawDataDao extends JpaRepository<RawData, Long> {
}
