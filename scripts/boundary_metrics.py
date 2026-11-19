#!/usr/bin/env python3
"""
도메인 경계 측정 스크립트 (/check-boundaries 2단계).

Python 3.8+ 표준 라이브러리만 사용한다. Windows 경로에서도 동작한다.

사용법 (프로젝트 루트에서):
    python scripts/boundary_metrics.py                      # 측정만
    python scripts/boundary_metrics.py --mode baseline      # docs/boundary-baseline.json 저장
    python scripts/boundary_metrics.py --mode check         # baseline 과 비교
옵션:
    --base com.example.app   기준 패키지 (없으면 @SpringBootApplication 클래스의 패키지)
    --kernel global          공용 커널 패키지 (쉼표 구분). 도메인 → 커널 참조는 얽힘으로 세지 않는다.
                             커널 → 도메인 참조는 따로 위반 후보로 보고한다.
    --assembly admin,home    화면 조립 패키지 (쉼표 구분). 얽힘으로 세되 따로 표시한다.
    --json                   사람이 읽는 요약 대신 JSON 만 출력

한계: import 문 기준으로 센다. 같은 패키지 안 참조, 와일드카드 import, 코드 안 완전 수식 이름은 세지 않는다.
"""
import argparse
import itertools
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path

MAIN_DIR = Path("src") / "main" / "java"
BASELINE = Path("docs") / "boundary-baseline.json"
CTOR_PARAM_THRESHOLD = 5

PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMPORT_RE = re.compile(r"^\s*import\s+(?!static\s)([\w.]+)\s*;", re.M)
TYPE_DECL_RE = re.compile(r"\b(class|record|enum|interface)\s+([A-Z]\w*)")
FINAL_FIELD_RE = re.compile(r"^\s*private\s+final\s+(?!static\b)([\w.<>,? ]+?)\s+(\w+)\s*(=|;)", re.M)
FIELD_RE = re.compile(r"^\s*(?:private|protected|public)?\s*(?!static\b)(?:final\s+)?([\w.<>,? ]+?)\s+(\w+)\s*(=|;)", re.M)


def configure_stdout():
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except AttributeError:  # Python < 3.7 에는 없지만 3.8+ 를 가정
        pass


def read(path):
    return path.read_text(encoding="utf-8", errors="replace")


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def detect_base_package(root):
    for f in (root / MAIN_DIR).rglob("*.java"):
        text = read(f)
        if "@SpringBootApplication" in text:
            m = PACKAGE_RE.search(text)
            if m:
                return m.group(1)
    sys.exit("기준 패키지를 찾지 못했습니다. --base 로 지정하세요.")


def balanced_parens(text, open_idx):
    """text[open_idx] == '(' 일 때 짝이 맞는 ')' 까지의 내용을 돌려준다."""
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[open_idx + 1:i]
    return ""


def split_params(params):
    params = params.strip()
    if not params:
        return []
    out, depth, cur = [], 0, ""
    for ch in params:
        if ch in "<(":
            depth += 1
        elif ch in ">)":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def param_type(param):
    # 애너테이션 제거 후 마지막 토큰이 이름, 그 앞이 타입
    p = re.sub(r"@\w+(\([^)]*\))?", "", param).replace("final ", "").strip()
    parts = p.rsplit(None, 1)
    return parts[0].strip() if len(parts) == 2 else p


def raw_type(t):
    return re.sub(r"<.*>", "", t).strip().split(".")[-1]


class JavaClass:
    def __init__(self, path, base, root):
        self.path = path
        self.rel = path.relative_to(root).as_posix()
        text = read(path)
        self.lines = text.count("\n") + (0 if text.endswith("\n") else 1)
        self.src = strip_comments(text)
        m = PACKAGE_RE.search(self.src)
        self.package = m.group(1) if m else ""
        self.imports = IMPORT_RE.findall(self.src)
        decl = TYPE_DECL_RE.search(self.src)
        self.kind = decl.group(1) if decl else "class"
        self.name = decl.group(2) if decl else path.stem
        self.fqn = "{}.{}".format(self.package, self.name)
        self.domain = domain_of(self.package, base)
        self.ctor_params = self._constructors()  # list of list[type]

    def _constructors(self):
        ctors = []
        name = re.escape(self.name)
        # 명시적 생성자
        for m in re.finditer(r"(?:public|protected|private)?\s*\b" + name + r"\s*\(", self.src):
            prefix = self.src[max(0, m.start() - 30):m.start()]
            if re.search(r"\b(new|class|record|enum|interface|return)\s*$", prefix):
                continue
            params = balanced_parens(self.src, m.end() - 1)
            tail = self.src[m.end() - 1 + len(params) + 2:].lstrip()
            if not tail.startswith("{") and not tail.startswith("throws"):
                continue
            ctors.append([param_type(p) for p in split_params(params)])
        # record 컴포넌트 (최상위 타입)
        if self.kind == "record":
            m = re.search(r"\brecord\s+" + name + r"\s*\(", self.src)
            if m:
                ctors.append([param_type(p) for p in split_params(balanced_parens(self.src, m.end() - 1))])
        # Lombok
        if self.kind == "class":
            body = self.src
            if "@RequiredArgsConstructor" in body:
                ctors.append([t.strip() for t, _, op in FINAL_FIELD_RE.findall(body) if op == ";"])
            if "@AllArgsConstructor" in body:
                ctors.append([t.strip() for t, _, _ in FIELD_RE.findall(body)])
        return ctors

    @property
    def injected_types(self):
        """스프링 빈의 생성자 주입 타입 (가장 긴 생성자 기준)."""
        if self.kind != "class":
            return []
        return max(self.ctor_params, key=len) if self.ctor_params else []

    @property
    def max_ctor_params(self):
        return max((len(c) for c in self.ctor_params), default=0)

    @property
    def is_service(self):
        return self.name.endswith("Service") and re.search(r"@Service\b", self.src) is not None

    def resolve(self, simple, base):
        """주입 타입 이름 → 도메인. 프로젝트 밖 타입이면 None."""
        for imp in self.imports:
            if imp.split(".")[-1] == simple:
                return domain_of(imp.rsplit(".", 1)[0], base) if imp.startswith(base + ".") else None
        return self.domain  # import 가 없으면 같은 패키지


def domain_of(package, base):
    if package == base or not package.startswith(base + "."):
        return None
    return package[len(base) + 1:].split(".")[0]


def import_domain(imp, base):
    if not imp.startswith(base + "."):
        return None
    return domain_of(imp.rsplit(".", 1)[0], base)


def find_cycles(edges):
    pairs = sorted({tuple(sorted((a, b))) for a, b in edges if (b, a) in edges and a != b})
    # 강하게 연결된 요소 (3개 이상 도메인 순환까지 잡기 위해)
    graph = defaultdict(set)
    for a, b in edges:
        graph[a].add(b)
    index, low, stack, on, sccs, counter = {}, {}, [], set(), [], [0]

    def strong(v):
        index[v] = low[v] = counter[0]
        counter[0] += 1
        stack.append(v)
        on.add(v)
        for w in graph[v]:
            if w not in index:
                strong(w)
                low[v] = min(low[v], low[w])
            elif w in on:
                low[v] = min(low[v], index[w])
        if low[v] == index[v]:
            comp = []
            while True:
                w = stack.pop()
                on.discard(w)
                comp.append(w)
                if w == v:
                    break
            if len(comp) > 1:
                sccs.append(sorted(comp))

    for v in list(graph):
        if v not in index:
            strong(v)
    return [list(p) for p in pairs], sorted(sccs)


def git_cochange(root, base, kernel):
    prefix = (MAIN_DIR / Path(*base.split("."))).as_posix() + "/"
    try:
        out = subprocess.run(
            ["git", "log", "--no-merges", "--name-only", "--pretty=format:@@%H"],
            cwd=str(root), capture_output=True, text=True, encoding="utf-8", check=True,
        ).stdout
    except (OSError, subprocess.CalledProcessError) as e:
        return {"error": str(e)}
    commits = []
    for block in out.split("@@")[1:]:
        lines = [l.strip() for l in block.splitlines()[1:] if l.strip()]
        domains = set()
        for f in lines:
            f = f.replace("\\", "/")
            if f.startswith(prefix):
                d = f[len(prefix):].split("/")
                if len(d) > 1 and d[0] not in kernel:
                    domains.add(d[0])
        commits.append(domains)
    touching = [c for c in commits if c]
    pairs = Counter()
    for c in touching:
        for a, b in itertools.combinations(sorted(c), 2):
            pairs[(a, b)] += 1
    return {
        "commits_total": len(commits),
        "commits_touching_domains": len(touching),
        "avg_domains_per_commit": round(sum(len(c) for c in touching) / len(touching), 2) if touching else 0,
        "cochange_top10": [{"pair": list(p), "commits": n} for p, n in pairs.most_common(10)],
    }


def measure(root, base, kernel, assembly):
    classes = [JavaClass(p, base, root) for p in sorted((root / MAIN_DIR).rglob("*.java"))
               if p.name != "package-info.java"]
    by_name = defaultdict(list)
    for c in classes:
        by_name[c.name].append(c)
    domains = sorted({c.domain for c in classes if c.domain})

    cross_lines, kernel_to_domain, pair_counter, edges = [], [], Counter(), set()
    for c in classes:
        if not c.domain:
            continue
        for imp in c.imports:
            d = import_domain(imp, base)
            if not d or d == c.domain:
                continue
            if c.domain in kernel:
                kernel_to_domain.append({"from": c.rel, "import": imp})
                continue
            if d in kernel:
                continue
            cross_lines.append({"from": c.rel, "from_domain": c.domain, "to_domain": d, "import": imp})
            pair_counter[(c.domain, d)] += 1
            edges.add((c.domain, d))

    service_injections = 0
    cross_injections = []
    for c in classes:
        if not c.domain:
            continue
        types = [raw_type(t) for t in c.injected_types]
        if c.is_service:
            service_injections += len(types)
        for t in types:
            if not (t.endswith("Service") or t.endswith("Repository")):
                continue
            d = c.resolve(t, base)
            if d and d != c.domain and d not in kernel:
                cross_injections.append({
                    "class": c.name, "domain": c.domain, "injects": t, "target_domain": d,
                    "layer": "service" if c.is_service else ("controller" if c.name.endswith("Controller") else "other"),
                    "assembly": c.domain in assembly,
                    "repository": t.endswith("Repository"),
                })

    many_params = sorted(
        ({"class": c.name, "domain": c.domain or "(root)", "params": c.max_ctor_params, "kind": c.kind}
         for c in classes if c.max_ctor_params >= CTOR_PARAM_THRESHOLD),
        key=lambda x: -x["params"])

    pairs, sccs = find_cycles(edges)

    # 클래스별 얽힘 점수: 다른 도메인으로 나가는 import + 다른 도메인에서 들어오는 import
    fan_out = Counter(l["from"] for l in cross_lines)
    fan_in = Counter()
    rel_of = {c.fqn: c.rel for c in classes}
    for l in cross_lines:
        if l["import"] in rel_of:
            fan_in[rel_of[l["import"]]] += 1
    for k in kernel_to_domain:
        fan_out[k["from"]] += 1
        if k["import"] in rel_of:
            fan_in[rel_of[k["import"]]] += 1
    tangled = sorted(
        ({"file": f, "out": fan_out[f], "in": fan_in[f], "score": fan_out[f] + fan_in[f]}
         for f in set(fan_out) | set(fan_in)),
        key=lambda x: (-x["score"], x["file"]))[:5]

    core_cross = [l for l in cross_lines if l["from_domain"] not in assembly]
    return {
        "base_package": base,
        "kernel": sorted(kernel),
        "assembly": sorted(assembly),
        "summary": {
            "classes": len(classes),
            "lines": sum(c.lines for c in classes),
            "domains": len(domains),
            "cross_domain_imports": len(cross_lines),
            "cross_domain_imports_core": len(core_cross),
            "cross_domain_imports_from_assembly": len(cross_lines) - len(core_cross),
            "kernel_to_domain_imports": len(kernel_to_domain),
            "service_injections": service_injections,
            "cross_domain_injections": len(cross_injections),
            "cross_domain_repository_injections": sum(1 for x in cross_injections if x["repository"]),
            "classes_ctor_params_ge5": len(many_params),
            "cycle_pairs": len(pairs),
        },
        "domain_list": domains,
        "cross_domain_pairs_top10": [{"from": a, "to": b, "imports": n} for (a, b), n in pair_counter.most_common(10)],
        "kernel_to_domain": kernel_to_domain,
        "cross_domain_injections": cross_injections,
        "ctor_params_ge5": many_params,
        "cycle_pairs": pairs,
        "cycle_components": sccs,
        "most_tangled_top5": tangled,
        "git": git_cochange(root, base, kernel),
    }


def flat_numbers(data):
    nums = dict(data["summary"])
    git = data.get("git", {})
    if "avg_domains_per_commit" in git:
        nums["avg_domains_per_commit"] = git["avg_domains_per_commit"]
        nums["commits_touching_domains"] = git["commits_touching_domains"]
    return nums


def print_report(data, diff=None):
    s = data["summary"]
    g = data["git"]
    print("기준 패키지: {}  (커널: {}, 조립: {})".format(
        data["base_package"], ",".join(data["kernel"]) or "-", ",".join(data["assembly"]) or "-"))
    print("도메인: {}".format(", ".join(data["domain_list"])))
    print()
    print("[요약]")
    for k, v in flat_numbers(data).items():
        delta = ""
        if diff and k in diff:
            delta = "  (baseline {} → {:+})".format(diff[k][0], diff[k][1])
        print("  {:<36} {}{}".format(k, v, delta))
    print()
    print("[도메인 쌍별 import 상위 10]")
    for p in data["cross_domain_pairs_top10"]:
        print("  {:>8} → {:<8} {}".format(p["from"], p["to"], p["imports"]))
    print()
    print("[커널 → 도메인 참조 (위반 후보)]")
    for k in data["kernel_to_domain"] or [{"from": "-", "import": ""}]:
        print("  {}  ->  {}".format(k["from"], k["import"]))
    print()
    print("[다른 도메인 Service·Repository 주입]")
    for x in data["cross_domain_injections"]:
        tag = "조립" if x["assembly"] else x["layer"]
        print("  {}.{} ({}) ← {}.{}".format(x["domain"], x["class"], tag, x["target_domain"], x["injects"]))
    print()
    print("[생성자 파라미터 {}개 이상]".format(CTOR_PARAM_THRESHOLD))
    for x in data["ctor_params_ge5"]:
        print("  {:<24} {:<8} {} ({})".format(x["class"], x["domain"], x["params"], x["kind"]))
    print()
    print("[순환] 쌍: {}  / 순환 묶음: {}".format(data["cycle_pairs"] or "-", data["cycle_components"] or "-"))
    print()
    print("[가장 얽힌 클래스 상위 5] (out=다른 도메인으로 나가는 import, in=들어오는 import)")
    for t in data["most_tangled_top5"]:
        print("  {:<60} out {} / in {}".format(t["file"], t["out"], t["in"]))
    print()
    if "error" in g:
        print("[git] 실패: {}".format(g["error"]))
    else:
        print("[git] 커밋 {}개 중 본 코드 도메인을 건드린 커밋 {}개, 커밋당 평균 도메인 {}".format(
            g["commits_total"], g["commits_touching_domains"], g["avg_domains_per_commit"]))
        for p in g["cochange_top10"]:
            print("  {} + {} : {}회".format(p["pair"][0], p["pair"][1], p["commits"]))


def main():
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=".")
    ap.add_argument("--base")
    ap.add_argument("--kernel", default="global")
    ap.add_argument("--assembly", default="")
    ap.add_argument("--mode", choices=["measure", "baseline", "check"], default="measure")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    base = args.base or detect_base_package(root)
    kernel = {k for k in args.kernel.split(",") if k}
    assembly = {a for a in args.assembly.split(",") if a}
    data = measure(root, base, kernel, assembly)

    diff = None
    baseline_path = root / BASELINE
    if args.mode == "baseline":
        baseline_path.parent.mkdir(parents=True, exist_ok=True)
        baseline_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    elif args.mode == "check":
        if not baseline_path.exists():
            sys.exit("{} 가 없습니다. 먼저 --mode baseline 으로 저장하세요.".format(BASELINE.as_posix()))
        old = flat_numbers(json.loads(baseline_path.read_text(encoding="utf-8")))
        new = flat_numbers(data)
        diff = {k: (old[k], round(new[k] - old[k], 2)) for k in new if k in old}

    if args.json:
        if diff:
            data["diff_vs_baseline"] = diff
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print_report(data, diff)
        if args.mode == "baseline":
            print("\nbaseline 저장: {}".format(BASELINE.as_posix()))


if __name__ == "__main__":
    main()
