package com.garyzhangscm.cwms.inventory.repository;
import com.garyzhangscm.cwms.inventory.model.ManualPickInventoryOperation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManualPickInventoryOperationRepository extends JpaRepository<ManualPickInventoryOperation,String> {}
