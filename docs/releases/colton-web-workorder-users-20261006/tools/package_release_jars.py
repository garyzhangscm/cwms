"""Package additive classes into exact running JARs; preserve all existing dependencies."""
import argparse, hashlib, json, shutil, zipfile
from pathlib import Path

def package(baseline, output, replacements):
    with zipfile.ZipFile(baseline) as old, zipfile.ZipFile(output, "w") as new:
        remaining=dict(replacements)
        for info in old.infolist():
            data=remaining.pop(info.filename, None)
            new.writestr(info, old.read(info.filename) if data is None else data)
        for name,data in remaining.items():
            new.writestr(name,data,compress_type=zipfile.ZIP_DEFLATED)
    return {"baseline_sha256":hashlib.sha256(Path(baseline).read_bytes()).hexdigest(),
            "output_sha256":hashlib.sha256(Path(output).read_bytes()).hexdigest(),"classes":sorted(replacements)}

def classes(root, patterns):
    result={}
    for pattern in patterns:
        files=list(root.glob(pattern))
        if not files: raise RuntimeError("No class matched "+pattern)
        for file in files: result["BOOT-INF/classes/"+file.relative_to(root).as_posix()]=file.read_bytes()
    return result

if __name__=="__main__":
    parser=argparse.ArgumentParser();parser.add_argument("root");parser.add_argument("workorder_baseline");parser.add_argument("resource_baseline");parser.add_argument("patched_classes");parser.add_argument("settlement_class");parser.add_argument("output");a=parser.parse_args()
    root=Path(a.root);out=Path(a.output);out.mkdir(parents=True,exist_ok=True)
    wo=classes(root/"workordersvr/target/classes",[
        "com/garyzhangscm/cwms/workorder/service/WorkOrderCompleteTransactionService*.class",
        "com/garyzhangscm/cwms/workorder/service/WorkOrderDeletionService*.class",
        "com/garyzhangscm/cwms/workorder/deletion/*.class",
        "com/garyzhangscm/cwms/completionpreview/*.class",
        "com/garyzhangscm/cwms/deletionpreview/*.class"])
    wo.update(classes(Path(a.patched_classes),["com/garyzhangscm/cwms/workorder/controller/WorkOrderController.class","com/garyzhangscm/cwms/workorder/service/WorkOrderService.class"]))
    wo["BOOT-INF/classes/com/garyzhangscm/cwms/workorder/service/WorkOrderLineService.class"]=Path(a.settlement_class).read_bytes()
    resource=classes(root/"ressvr/target/classes",["com/garyzhangscm/cwms/resources/controller/UserPasswordResetController.class","com/garyzhangscm/cwms/resources/service/UserPasswordResetService.class","com/garyzhangscm/cwms/resources/model/PasswordResetRequest.class","com/garyzhangscm/cwms/resourcereleasepreview/*.class"])
    report={"workorder":package(a.workorder_baseline,out/"workorderserver-v1.62.jar",wo),"resource":package(a.resource_baseline,out/"resourceserver-v1.62.jar",resource)}
    (out/"jar-manifest.json").write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
