package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookWebhookHistory;
import com.garyzhangscm.cwms.quickbook.model.WebhookStatus;
import com.garyzhangscm.cwms.quickbook.repository.QuickbookWebhookRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class QuickBookWebhookHistoryService {

	private static final Logger logger = LoggerFactory.getLogger(QuickBookWebhookHistoryService.class);
	
	@Autowired
	private QuickbookWebhookRepository quickbookWebhookRepository;



	public QuickBookWebhookHistory findById(Long id) {
		QuickBookWebhookHistory quickBookWebhookHistory = quickbookWebhookRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.raiseException("quick book webhook history not found by id: " + id));
		return quickBookWebhookHistory;
	}


	public List<QuickBookWebhookHistory> findAll(Long warehouseId,
												 String signature,
												 String payload,
												 String status) {

		return quickbookWebhookRepository.findAll(
				(Root<QuickBookWebhookHistory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
					List<Predicate> predicates = new ArrayList<Predicate>();

					if (Objects.nonNull(warehouseId)) {

						predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
					}


					if (Strings.isNotBlank(signature)) {

						predicates.add(criteriaBuilder.equal(root.get("signature"), signature));
					}
					if (Strings.isNotBlank(payload)) {

						predicates.add(criteriaBuilder.equal(root.get("payload"), payload));
					}
					if (Strings.isNotBlank(status)) {

						predicates.add(criteriaBuilder.equal(root.get("status"), WebhookStatus.valueOf(status)));
					}


					Predicate[] p = new Predicate[predicates.size()];
					return criteriaBuilder.and(predicates.toArray(p));
				});
	}

	public QuickBookWebhookHistory save(QuickBookWebhookHistory quickBookWebhookHistory) {
		return quickbookWebhookRepository.save(quickBookWebhookHistory);
	}

	public QuickBookWebhookHistory addNewWebhookRequest(String signature, String payload) {
		return addNewWebhookRequest(
				signature, payload, WebhookStatus.PENDING
		);

	}
	public QuickBookWebhookHistory addNewWebhookRequest(String signature, String payload, WebhookStatus status) {
		return addNewWebhookRequest(
				signature, payload, status, ""
		);

	}
	public QuickBookWebhookHistory addNewWebhookRequest(String signature, String payload,
														WebhookStatus status,
														String errorMessage) {
		QuickBookWebhookHistory quickBookWebhookHistory = new QuickBookWebhookHistory(
				signature, payload, status, errorMessage
		);
		return save(quickBookWebhookHistory);

	}
	public QuickBookWebhookHistory findPendingWebhookRequest(String signature, String payload) {
		return findAll(null, signature, payload, WebhookStatus.PENDING.toString())
				.stream().findFirst().orElse(null);
	}
	public QuickBookWebhookHistory findPendingWebhookRequest(String payload) {
		return findPendingWebhookRequest(null, payload);
	}
}
