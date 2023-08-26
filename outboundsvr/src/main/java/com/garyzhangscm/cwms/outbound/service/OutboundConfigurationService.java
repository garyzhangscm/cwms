package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.AllocationConfiguration;
import com.garyzhangscm.cwms.outbound.model.AllocationConfigurationType;
import com.garyzhangscm.cwms.outbound.model.OutboundConfiguration;
import com.garyzhangscm.cwms.outbound.model.PickConfiguration;
import com.garyzhangscm.cwms.outbound.repository.OutboundConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class OutboundConfigurationService {

    @Autowired
    private OutboundConfigurationRepository outboundConfigurationRepository;

    public OutboundConfiguration findById(Long id) {
        return outboundConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("outbound configuration not found by id: " + id));

    }


    public OutboundConfiguration save(OutboundConfiguration outboundConfiguration) {
        return outboundConfigurationRepository.save(outboundConfiguration);
    }

    public OutboundConfiguration saveOrUpdate(
            OutboundConfiguration outboundConfiguration) {
        if (outboundConfiguration.getId() == null
                && findByWarehouse(
                outboundConfiguration.getWarehouseId()) != null) {
            outboundConfiguration.setId(
                    findByWarehouse(
                            outboundConfiguration.getWarehouseId()).getId());
        }
        return save(outboundConfiguration);
    }



    public boolean isShortAutoReallocationEnabled(Long warehouseId) {
        OutboundConfiguration outboundConfiguration = findByWarehouse(warehouseId);
        if (Objects.isNull(outboundConfiguration)) {
            return false;
        }
        return Boolean.TRUE.equals(outboundConfiguration.getShortAutoReallocation());
    }

    public OutboundConfiguration findByWarehouse(Long warehouseId) {
        return outboundConfigurationRepository.findByWarehouseId(warehouseId);
    }

    public boolean isSynchronousAllocationRequired(Long warehouseId, Long palletQuantity) {
        OutboundConfiguration outboundConfiguration = findByWarehouse(warehouseId);
        if (Objects.isNull(outboundConfiguration)) {
            return false;
        }
        if (Boolean.TRUE.equals(outboundConfiguration.getAsynchronousAllocation())) {
            return true;
        }
        if (Objects.nonNull(outboundConfiguration.getAsynchronousAllocationPalletThreshold()) &&
                outboundConfiguration.getAsynchronousAllocationPalletThreshold() > 0) {
            // if the configuration is not setup to explicitly allow asynchronous allocation
            // then check if the total pallet quantity that is to be allocated exceed the
            // threshold. If there're too many pallets to be allocated, then we will use
            // asynchronous allocation. We assume each pallet quantity will generate a pallet
            // pick so too many pallet picks to be generated will cause performance issue.
            return palletQuantity >= outboundConfiguration.getAsynchronousAllocationPalletThreshold();
        }
        return false;
    }


    public OutboundConfiguration addOutboundConfiguration(OutboundConfiguration outboundConfiguration) {
        return saveOrUpdate(outboundConfiguration);
    }

    public OutboundConfiguration changeOutboundConfiguration(Long id, OutboundConfiguration outboundConfiguration) {
        outboundConfiguration.setId(id);
        return saveOrUpdate(outboundConfiguration);

    }
}
