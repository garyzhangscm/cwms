"""Prepare an isolated source overlay; never edits the supplied backend checkout."""
from pathlib import Path
import argparse,difflib
p=argparse.ArgumentParser();p.add_argument('--backend',required=True);p.add_argument('--output',required=True);a=p.parse_args()
base=Path(__file__).resolve().parent; backend=Path(a.backend);output=Path(a.output);output.mkdir(parents=True,exist_ok=True)
patch=[]
def save(rel,old,new):
 dest=output/rel;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(new)
 patch.extend(difflib.unified_diff(old.splitlines(True),new.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
rel='src/main/java/com/garyzhangscm/cwms/outbound/service/OrderService.java';old=(backend/rel).read_text();s=old
anchor='    @Transactional\n    public Order allocate(Long orderId, Boolean asynchronous) {'
a=s.index(anchor);b=s.index('    @Transactional\n    public void allocate(Order order,',a)
s=s[:a]+'''    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.garyzhangscm.cwms.outbound.allocation.AllocationTaskService allocationTaskService;

    public Order allocate(Long orderId, Boolean asynchronous) {
        Order order = findById(orderId);
        if (asynchronous == null) {
            long pallets = order.getOrderLines().stream().filter(line -> !Boolean.TRUE.equals(line.getNonAllocatable()))
                .mapToLong(line -> orderLineService.getPalletQuantityEstimation(line, line.getExpectedQuantity() - line.getShippedQuantity())).sum();
            asynchronous = outboundConfigurationService.isSynchronousAllocationRequired(order.getWarehouseId(), pallets);
        }
        com.garyzhangscm.cwms.outbound.allocation.AllocationTask task = allocationTaskService.submit(
            order.getWarehouseId(), orderId, java.util.UUID.randomUUID().toString(), Boolean.TRUE.equals(asynchronous));
        if ("FAILED".equals(task.state) || "REVIEW_REQUIRED".equals(task.state)) throw OrderOperationException.raiseException(task.message);
        return findById(orderId);
    }

    public void executeAllocationTask(Long orderId) {
        Order order = findById(orderId);
        List<OrderLine> lines = order.getOrderLines().stream().filter(line -> !Boolean.TRUE.equals(line.getNonAllocatable())).collect(Collectors.toList());
        allocate(order, lines);
    }

'''+s[b:];save(rel,old,s)
rel='src/main/java/com/garyzhangscm/cwms/outbound/service/AllocationStrategyFactory.java';old=(backend/rel).read_text();i=old.index('    public AllocationStrategy getDefaultAllocationStrategy()');s=old[:i]+'''    public AllocationStrategy requireAllocationStrategy(AllocationStrategyType type) {
        if (type == null) throw new com.garyzhangscm.cwms.outbound.allocation.AllocationValidationFailure("INVALID_STRATEGY", "An allocation strategy is required.");
        return getAllocationStrategyByType(type).orElseThrow(() -> new com.garyzhangscm.cwms.outbound.allocation.AllocationValidationFailure("INVALID_STRATEGY", "Unsupported allocation strategy: " + type));
    }

'''+old[i:];save(rel,old,s)
rel='src/main/java/com/garyzhangscm/cwms/outbound/service/AllocationService.java';old=(backend/rel).read_text();a=old.index('        AllocationStrategy allocationStrategy =',old.index('public AllocationResult tryAllocate(AllocationRequest allocationRequest, Location sourceLocation)'));b=old.index('        logger.debug("Will allocate with strategy:',a)
s=old[:a]+'''        AllocationStrategy allocationStrategy = allocationStrategyService.requireAllocationStrategy(
                allocationRequest.getAllocationStrategyTypes().isEmpty() ? null : allocationRequest.getAllocationStrategyTypes().get(0));
'''+old[b:];save(rel,old,s)
for source in (base/'src').rglob('*.java'):
 rel='src/main/java/'+str(source.relative_to(base/'src'));save(rel,'',source.read_text())
(base/'backend.patch').write_text(''.join(patch))
print(output)
