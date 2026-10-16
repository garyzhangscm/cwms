package com.garyzhangscm.cwms.outbound.allocation;
public class AllocationValidationFailure extends RuntimeException {
 public final String code;
 public AllocationValidationFailure(String code, String message) { super(message); this.code = code; }
}
