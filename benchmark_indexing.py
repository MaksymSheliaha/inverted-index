#!/usr/bin/env python3
"""
Скрипт для бенчмарку паралельної індексації (Index Endpoint Scalability Benchmark).

Викликає POST /api/index по черзі з різною кількістю потоків threadNum:
1, 2, 4, 6, 8, 10, ..., 32 (налаштовується).

Зберігає результати у:
- CSV:  indexing_benchmark_results.csv
- JSON: indexing_benchmark_results.json
- PNG:  indexing_scalability.png (графіки часу, прискорення та ефективності)
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path


def parse_args():
    # За замовчуванням: 1, 2, 4, 6, 8, ..., 32
    default_threads = [1] + list(range(2, 33, 2))
    default_threads_str = ",".join(map(str, default_threads))

    parser = argparse.ArgumentParser(
        description="Бенчмарк масштабованості паралельної індексації датасету (POST /api/index)."
    )
    parser.add_argument(
        "--host",
        default="http://localhost:8080",
        help="Базовий URL бекенду (за замовчуванням: http://localhost:8080)",
    )
    parser.add_argument(
        "--threads",
        default=default_threads_str,
        help="Список потоків через кому (за замовчуванням: 1,2,4,6,...,32)",
    )
    parser.add_argument(
        "--runs",
        type=int,
        default=1,
        help="Кількість прогонів для кожної кількості потоків (для усереднення, за замовчуванням: 1)",
    )
    parser.add_argument(
        "--pause",
        type=float,
        default=1.0,
        help="Пауза між запусками в секундах (за замовчуванням: 1.0)",
    )
    parser.add_argument(
        "--out-dir",
        default=".",
        help="Директорія для збереження результатів (за замовчуванням поточна)",
    )
    return parser.parse_args()


def call_index_api(host: str, thread_num: int, timeout: int = 300) -> dict:
    """Викликає POST /api/index з параметром threadNum."""
    url = f"{host.rstrip('/')}/api/index"
    payload = json.dumps({"threadNum": thread_num}).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )

    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status_code = response.getcode()
            body = response.read().decode("utf-8")
            elapsed_client_ms = (time.time() - start_time) * 1000.0

            if status_code != 200:
                return {
                    "success": False,
                    "error": f"HTTP {status_code}: {body[:200]}",
                    "client_time_ms": elapsed_client_ms,
                }

            data = json.loads(body)
            data["client_time_ms"] = elapsed_client_ms
            return data
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="ignore")
        return {
            "success": False,
            "error": f"HTTP {e.code}: {err_body[:200]}",
            "client_time_ms": (time.time() - start_time) * 1000.0,
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "client_time_ms": (time.time() - start_time) * 1000.0,
        }


def plot_scalability(results: list, out_file: str):
    """Будує графіки Часу виконання, Прискорення (Speedup) та Ефективності (Efficiency)."""
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print("\n[Увага] matplotlib не встановлено. Графік не буде побудовано (pip install matplotlib).")
        return

    valid_results = [r for r in results if r.get("success")]
    if not valid_results:
        return

    threads = [r["threads"] for r in valid_results]
    times_ms = [r["build_time_ms"] for r in valid_results]
    speedups = [r["speedup"] for r in valid_results]
    efficiencies = [r["efficiency"] for r in valid_results]

    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(18, 5))
    fig.patch.set_facecolor("#FAFAFA")

    # 1. Час виконання
    ax1.plot(threads, [t / 1000.0 for t in times_ms], marker="o", color="#1f77b4", linewidth=2)
    ax1.set_title("Час індексації vs Потоки", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Кількість потоків (threadNum)", fontsize=11)
    ax1.set_ylabel("Час виконання (секунди)", fontsize=11)
    ax1.grid(True, linestyle="--", alpha=0.6)
    ax1.set_xticks(threads)

    # 2. Прискорення (Speedup) S(p) = T(1) / T(p)
    ax2.plot(threads, speedups, marker="s", color="#2ca02c", linewidth=2, label="Реальне прискорення")
    ax2.plot(threads, threads, linestyle="--", color="#d62728", alpha=0.7, label="Ідеальне лінійне")
    ax2.set_title("Прискорення (Speedup: S = T₁ / Tₚ)", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Кількість потоків (threadNum)", fontsize=11)
    ax2.set_ylabel("Коефіцієнт прискорення", fontsize=11)
    ax2.legend()
    ax2.grid(True, linestyle="--", alpha=0.6)
    ax2.set_xticks(threads)

    # 3. Ефективність (Efficiency) E(p) = S(p) / p
    ax3.plot(threads, [e * 100.0 for e in efficiencies], marker="^", color="#ff7f0e", linewidth=2)
    ax3.axhline(100.0, linestyle="--", color="#d62728", alpha=0.7, label="100% (Ідеальна)")
    ax3.set_title("Ефективність розпаралелювання (E = S / p)", fontsize=12, fontweight="bold")
    ax3.set_xlabel("Кількість потоків (threadNum)", fontsize=11)
    ax3.set_ylabel("Ефективність (%)", fontsize=11)
    ax3.set_ylim(0, 110)
    ax3.legend()
    ax3.grid(True, linestyle="--", alpha=0.6)
    ax3.set_xticks(threads)

    plt.tight_layout()
    plt.savefig(out_file, dpi=200)
    plt.close()
    print(f"[Успіх] Графік масштабованості збережено у: {out_file}")


def main():
    args = parse_args()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    thread_list = []
    for t_str in args.threads.split(","):
        t_clean = t_str.strip()
        if t_clean.isdigit():
            thread_list.append(int(t_clean))

    if not thread_list:
        print("Помилка: не вказано жодної коректної кількості потоків.")
        sys.exit(1)

    print("=" * 90)
    print(" БЕНЧМАРК ПАРАЛЕЛЬНОЇ ІНДЕКСАЦІЇ (INDEXING SCALABILITY)")
    print(f" Цільовий сервер:   {args.host}")
    print(f" Ендпоінт:          POST /api/index")
    print(f" Потоки (threadNum):{thread_list}")
    print(f" Прогонів на рівень:{args.runs}")
    print("=" * 90)

    # Заголовок таблиці
    print(f"{'Threads':>8} | {'Build Time(ms)':>14} | {'Time(s)':>8} | {'Speedup':>8} | {'Efficiency':>11} | {'Docs/s':>10} | {'Status':>8}")
    print("-" * 90)

    results = []
    baseline_time_ms = None  # Час для 1 потоку (T1)

    for t_num in thread_list:
        run_times = []
        doc_count = 0
        success = True
        error_msg = ""

        for r_idx in range(args.runs):
            if r_idx > 0 or t_num != thread_list[0]:
                time.sleep(args.pause)

            res = call_index_api(args.host, t_num)

            if res.get("success") or res.get("indexed"):
                b_time = res.get("indexBuildTimeMs") or res.get("client_time_ms", 0)
                run_times.append(b_time)
                doc_count = res.get("documents", 0)
            else:
                success = False
                error_msg = res.get("error") or res.get("message") or "Unknown error"
                break

        if success and run_times:
            avg_time_ms = sum(run_times) / len(run_times)
            avg_time_sec = avg_time_ms / 1000.0

            # Встановлення T1 для розрахунку Speedup
            if t_num == 1 or baseline_time_ms is None:
                baseline_time_ms = avg_time_ms

            speedup = baseline_time_ms / avg_time_ms if avg_time_ms > 0 else 1.0
            efficiency = speedup / t_num if t_num > 0 else 1.0
            docs_per_sec = (doc_count / avg_time_sec) if avg_time_sec > 0 else 0.0

            record = {
                "threads": t_num,
                "build_time_ms": round(avg_time_ms, 2),
                "build_time_sec": round(avg_time_sec, 3),
                "speedup": round(speedup, 3),
                "efficiency": round(efficiency, 3),
                "doc_count": doc_count,
                "docs_per_sec": round(docs_per_sec, 1),
                "success": True,
            }
            results.append(record)

            print(
                f"{t_num:8d} | "
                f"{avg_time_ms:14.1f} | "
                f"{avg_time_sec:8.2f} | "
                f"{speedup:8.2f} | "
                f"{(efficiency * 100.0):10.1f}% | "
                f"{docs_per_sec:10.1f} | "
                f"{'OK':>8}"
            )
        else:
            record = {
                "threads": t_num,
                "build_time_ms": None,
                "build_time_sec": None,
                "speedup": None,
                "efficiency": None,
                "doc_count": 0,
                "docs_per_sec": 0,
                "success": False,
                "error": error_msg,
            }
            results.append(record)
            print(f"{t_num:8d} | {'ПОМИЛКА: ' + error_msg[:45]:>77}")

    if not any(r.get("success") for r in results):
        print("\nЖоден тест не завершився успішно. Перевірте доступність бекенду.")
        return

    # Збереження в CSV
    csv_file = out_dir / "indexing_benchmark_results.csv"
    with open(csv_file, mode="w", newline="", encoding="utf-8") as f:
        fieldnames = [
            "threads",
            "build_time_ms",
            "build_time_sec",
            "speedup",
            "efficiency",
            "doc_count",
            "docs_per_sec",
            "success",
        ]
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for r in results:
            writer.writerow(r)

    # Збереження в JSON
    json_file = out_dir / "indexing_benchmark_results.json"
    with open(json_file, mode="w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    print("-" * 90)
    print(f"[Успіх] CSV результати збережено у:  {csv_file}")
    print(f"[Успіх] JSON результати збережено у: {json_file}")

    # Побудова графіка
    plot_file = out_dir / "indexing_scalability.png"
    plot_scalability(results, str(plot_file))

    print("=" * 90)
    print(" Бенчмарк індексації завершено!")
    print("=" * 90)


if __name__ == "__main__":
    main()
