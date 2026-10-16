package com.garyzhangscm.cwms.resources.repository;
import com.garyzhangscm.cwms.resources.model.PreprintedLpn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PreprintedLpnRepository extends JpaRepository<PreprintedLpn,Long> {
 Optional<PreprintedLpn> findByWarehouseIdAndLpn(Long warehouseId,String lpn);
 List<PreprintedLpn> findByWarehouseIdAndBatch(Long warehouseId,String batch);
}
