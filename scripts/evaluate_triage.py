"""Run a labeled synthetic evaluation. No network or paid model is used."""
import json
from pathlib import Path
import sys

from analyze_execution import classify
from interpret_execution import interpret


def evaluate():
    path = Path(__file__).with_name("triage_cases.json")
    cases = json.loads(path.read_text(encoding="utf-8"))
    blank = dict(documentRequested="false", documentSent="false", productsSent="false",
                 cartSent="false", pageState="unmarked", location="blank")
    cart = dict(blank, documentRequested="true", documentSent="true", productsSent="true",
                pageState="error", location="fixture")
    observations = {"blank": [blank], "none": [], "cart": [cart], "mixed": [blank, cart],
                    "received": [dict(blank, documentRequested="true")]}
    correct = abstained = grounded = valid = unsupported = expected_unknown = 0
    for case in cases:
        markers = observations[case["observations"]]
        finding = classify({"result": "failed", "category": case["type"], "observations": markers})
        result = interpret(finding, [sys.executable, "-c", "import json,sys; print(json.dumps(json.load(sys.stdin)[\"candidate\"]))"])
        correct += result["category"] == case["expected"]
        expected_unknown += case["expected"] == "unknown"
        abstained += case["expected"] == "unknown" and result["abstentionReason"] is not None
        valid += result["schemaVersion"] == 1 and result["source"] == "validated_provider"
        grounded += result["evidenceRefs"] == [f"observation-{i+1}" for i in range(len(finding["observations"]))]
        unsupported += result["suspectedCause"] is not None
    result = {"schemaVersion": 1, "dataset": "synthetic-v1", "sampleSize": len(cases),
              "mode": "deterministic classifier with simulated echo provider",
              "correctClassifications": correct, "validatedResponses": valid, "groundedReferenceSets": grounded,
              "appropriateAbstentions": abstained, "expectedAbstentions": expected_unknown,
              "unsupportedCauseClaims": unsupported, "realModelEvaluated": False}
    return result


if __name__ == "__main__":
    result = evaluate()
    path = Path("target/comparison/evaluation.json")
    path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    if not (result["sampleSize"] == result["correctClassifications"] == result["validatedResponses"]
            == result["groundedReferenceSets"] and result["unsupportedCauseClaims"] == 0
            and result["appropriateAbstentions"] == result["expectedAbstentions"]):
        sys.exit("Evaluation gate failed")
