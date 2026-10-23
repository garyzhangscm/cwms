package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ClientRestriction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class RfidItemLookupService {
    @PersistenceContext private EntityManager em;
    @Autowired private ClientRestrictionUtil restrictions;

    public static Set<String> variants(String csv) {
        String[] values = csv.split(",", -1);
        if (values.length > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 50 UPCs");
        Set<String> result = new LinkedHashSet<>();
        for (String raw : values) {
            String value = raw.trim();
            if (!value.matches("(?:[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UPC/GTIN");
            String padded = String.format("%14s", value).replace(' ', '0');
            int sum = 0;
            for (int i = 0; i < 13; i++) sum += (padded.charAt(i) - '0') * (i % 2 == 0 ? 3 : 1);
            if ((10 - sum % 10) % 10 != padded.charAt(13) - '0')
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GTIN check digit");
            for (int length : new int[]{8, 12, 13, 14}) {
                String prefix = padded.substring(0, 14 - length);
                if (prefix.chars().allMatch(c -> c == '0')) result.add(padded.substring(14 - length));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lookup(Long companyId, Long warehouseId, String upcs, ClientRestriction access) {
        Set<String> codes = variants(upcs);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Item> item = query.from(Item.class);
        List<Predicate> filters = new ArrayList<>();
        filters.add(cb.equal(item.get("companyId"), companyId));
        filters.add(cb.or(cb.equal(item.get("warehouseId"), warehouseId), cb.isNull(item.get("warehouseId"))));
        filters.add(cb.trim(item.<String>get("defaultInventoryAttribute1")).in(codes));
        // A warehouse override suppresses the company item even when its UPC differs.
        Subquery<Long> override = query.subquery(Long.class);
        Root<Item> other = override.from(Item.class);
        override.select(other.get("id")).where(
            cb.equal(other.get("companyId"), companyId),
            cb.equal(other.get("warehouseId"), warehouseId),
            cb.equal(other.get("name"), item.get("name")));
        filters.add(cb.or(cb.isNotNull(item.get("warehouseId")), cb.not(cb.exists(override))));
        query.multiselect(item.get("id").alias("id"), item.get("name").alias("name"),
            item.get("description").alias("description"), item.get("defaultInventoryAttribute1").alias("upc"));
        query.where(restrictions.addClientRestriction(item, filters, access, cb));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple row : em.createQuery(query).getResultList()) {
            Map<String, Object> value = new LinkedHashMap<>();
            for (String key : new String[]{"id", "name", "description", "upc"}) value.put(key, row.get(key));
            result.add(value);
        }
        return result;
    }
}
