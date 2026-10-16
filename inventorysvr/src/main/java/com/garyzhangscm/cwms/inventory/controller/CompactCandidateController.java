package com.garyzhangscm.cwms.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.allocationwire.CandidateWireCodec;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Additive endpoint: the exact running controller still selects all candidates. */
@RestController
public class CompactCandidateController {
    private final InventoryController delegate;
    private final CandidateWireCodec codec;
    public CompactCandidateController(InventoryController delegate, @Qualifier("getObjMapper") ObjectMapper mapper) {
        this.delegate = delegate;
        this.codec = new CandidateWireCodec(mapper, Inventory.class);
    }

    @RequestMapping(value="/inventories/pickable/compact-v1", method = RequestMethod.GET)
    public JsonNode findCompactPickableInventories(@RequestParam(name="itemId") Long itemId,
                                                   @RequestParam(name="inventoryStatusId") Long inventoryStatusId,
                                                   @RequestParam(name = "lpn", defaultValue = "", required = false) String lpn,
                                                   @RequestParam(name = "color", defaultValue = "", required = false) String color,
                                                   @RequestParam(name = "productSize", defaultValue = "", required = false) String productSize,
                                                   @RequestParam(name = "style", defaultValue = "", required = false) String style,
                                                   @RequestParam(name="attribute1", required = false, defaultValue = "") String attribute1,
                                                   @RequestParam(name="attribute2", required = false, defaultValue = "") String attribute2,
                                                   @RequestParam(name="attribute3", required = false, defaultValue = "") String attribute3,
                                                   @RequestParam(name="attribute4", required = false, defaultValue = "") String attribute4,
                                                   @RequestParam(name="attribute5", required = false, defaultValue = "") String attribute5,
                                                   @RequestParam(name = "receiptNumber", defaultValue = "", required = false) String receiptNumber,
                                                   @RequestParam(name = "locationId", defaultValue = "", required = false) Long locationId,
                                                   @RequestParam(name = "skipLocationIds", defaultValue = "", required = false) String skipLocationIds,
                                                   @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails,
                                                   @RequestParam(name = "lpnLimit", defaultValue = "9999", required = false) int lpnLimit) throws java.io.IOException {
        List<Inventory> candidates = delegate.findPickableInventories(itemId, inventoryStatusId,
                lpn, color, productSize, style, attribute1, attribute2, attribute3, attribute4,
                attribute5, receiptNumber, locationId, skipLocationIds, includeDetails, lpnLimit);
        return codec.encode(candidates);
    }
}
