package com.garyzhangscm.cwms.outbound.repository;
import com.garyzhangscm.cwms.outbound.model.ManualPickOperation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManualPickOperationRepository extends JpaRepository<ManualPickOperation,String> { boolean existsByPickId(Long pickId); }
