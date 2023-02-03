package disk.data;

import disk.SystemItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemItemRepository extends CrudRepository<SystemItem,String> {
}
