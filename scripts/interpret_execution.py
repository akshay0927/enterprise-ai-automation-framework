"""Optional, bounded interpretation of a sanitized finding using a JSON stdin/stdout provider."""
import json
from pathlib import Path
import subprocess
import sys
import threading

from analyze_execution import GUIDANCE
from execution_summary import bounded, require, FIELDS

# A provider selects only grounded references and existing investigation guidance.
# Free text, executable instructions, and source report payloads are never accepted.
def provider_reply(command, request, timeout):
    with subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                          stderr=subprocess.DEVNULL, shell=False) as process:
        chunks = []
        def read_reply():
            data = process.stdout.read(4097)
            chunks.append(data)
            if len(data) > 4096:
                process.kill()
        reader = threading.Thread(target=read_reply, daemon=True)
        reader.start()
        try:
            process.stdin.write(json.dumps(request).encode())
            process.stdin.close()
            require(process.wait(timeout=timeout) == 0, "provider_failed")
            reader.join(timeout)
            require(chunks and len(chunks[0]) <= 4096, "provider_limit")
            return json.loads(chunks[0])
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()
            reader.join(timeout)


def interpret(finding, provider=None, timeout=2):
    category = finding["category"]
    require(category in GUIDANCE, "invalid_finding")
    refs = [f"observation-{i+1}" for i in range(len(finding["observations"]))]
    fallback = {"schemaVersion": 1, "source": "offline", "category": category, "evidenceRefs": refs,
                "summary": category.replace("_", " "), "suspectedCause": None,
                "recommendation": GUIDANCE[category], "missingEvidence": finding["missingEvidence"],
                "uncertainty": finding["uncertainty"], "abstentionReason": "insufficient evidence" if category == "unknown" else None}
    if provider is None:
        return fallback
    # Send only fixed vocabulary and opaque evidence references. Raw diagnostics
    # and report text cannot enter the provider command or prompt.
    payload = {"schemaVersion": 1, "category": category, "evidenceRefs": refs,
               "recommendationId": category, "uncertainty": finding["uncertainty"]}
    observations = []
    for ref, observation in zip(refs, finding["observations"]):
        if isinstance(observation, str):
            require(observation in ("typed_assertion", "incomplete_report", "toolchain_failed", "driver_mismatch", "sign_in_failure"), "invalid_signal")
        else:
            require(observation.keys() == FIELDS.keys()
                    and all(v in FIELDS[k] for k, v in observation.items()), "invalid_marker")
        observations.append({"id": ref, "observed": observation})
    request = {"evidence": observations, "candidate": payload, "allowedAlternative": {"abstain": True}}
    for attempt in range(2):
        try:
            response = provider_reply(provider, request, timeout)
            if response == {"abstain": True}:
                return dict(fallback, source="validated_provider", abstentionReason="provider abstained")
            require(response == payload, "unsupported_provider_claim")
            return dict(fallback, source="validated_provider")
        except (OSError, subprocess.SubprocessError, ValueError, RecursionError):
            continue
    return dict(fallback, abstentionReason="provider response unavailable or unsupported")


def main():
    directory = Path(sys.argv[1])
    for platform in ("Windows", "Linux"):
        analysis = json.loads(bounded(directory / f"analysis-{platform}.json", 1024*1024))
        interpretations = {key: interpret(finding) for key, finding in analysis["findings"].items()}
        result = {"schemaVersion": 1, "testedCommit": analysis["testedCommit"], "platform": platform,
                  "mode": "offline", "interpretations": interpretations}
        (directory / f"interpretations-{platform}.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
