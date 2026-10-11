import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Dict, List, Optional


DEFAULT_HOST = os.environ.get("INVERTED_INDEX_URL") or os.environ.get("BACKEND_HOST") or "http://localhost:8080"


class InvertedIndexClient:

    def __init__(self, base_url: str = DEFAULT_HOST, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        if not (self.base_url.startswith("http://") or self.base_url.startswith("https://")):
            self.base_url = f"http://{self.base_url}"
        self.timeout = timeout

    def search_ids(self, query: str, page: int = 0, size: int = 10) -> List[int]:
        url = f"{self.base_url}/api/search"
        payload = {
            "query": query,
            "mode": "EXPANDED",
            "page": page,
            "size": size,
            "debug": False,
        }

        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=data,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
            method="POST",
        )

        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                result = json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Search failed (HTTP {e.code}): {err_body}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(f"Failed to connect to backend at {self.base_url}: {e.reason}") from e

        docs = result.get("docs", [])
        return [doc["id"] for doc in docs if "id" in doc]

    def get_documents_by_ids(self, doc_ids: List[int]) -> List[Dict[str, Any]]:
        if not doc_ids:
            return []

        ids_param = ",".join(str(d) for d in doc_ids)
        query_string = urllib.parse.urlencode({"ids": ids_param})
        url = f"{self.base_url}/api/documents?{query_string}"

        req = urllib.request.Request(url, headers={"Accept": "application/json"}, method="GET")

        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                docs = json.loads(resp.read().decode("utf-8"))
                return docs if isinstance(docs, list) else []
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Document retrieval failed (HTTP {e.code}): {err_body}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(f"Failed to connect to backend at {self.base_url}: {e.reason}") from e

    def get_document_by_id(self, doc_id: int):
        """
        Retrieves a single document by its ID.
        """
        docs = self.get_documents_by_ids([doc_id])
        return docs[0] if docs else None


def run_interactive(client: InvertedIndexClient):
    print("=" * 60)
    print("Inverted Index Interactive CLI")
    print(f"Connected to: {client.base_url}")
    print("Commands:")
    print("  search <query> [-p page] Search (always EXPANDED, returns doc IDs)")
    print("  page <number>            Change page for the last query")
    print("  get <id ...>             Retrieve document(s) by ID (e.g. get 1042 2055 or get 1042, 2055)")
    print("  help                     Show this help")
    print("  exit                     Exit CLI")
    print("=" * 60)

    last_query = None

    while True:
        try:
            line = input("\n> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nExiting.")
            break

        if not line:
            continue

        parts = line.split(maxsplit=1)
        cmd = parts[0].lower()
        arg = parts[1].strip() if len(parts) > 1 else ""

        if cmd in ("exit", "quit", "q"):
            print("Bye!")
            break

        if cmd == "help":
            print("Commands:")
            print("  search <query> [-p page] Search (always EXPANDED, returns doc IDs)")
            print("  page <number>            Change page for the last query")
            print("  get <id ...>             Retrieve document(s) by ID (e.g. get 1042 2055 or get 1042, 2055)")
            print("  exit                     Exit CLI")
            continue

        if cmd == "search":
            if not arg:
                print("Usage: search <query> [-p page]")
                continue

            query_str = arg
            page_val = 0
            if " -p " in query_str:
                query_str, p_val = query_str.rsplit(" -p ", 1)
                if p_val.strip().isdigit():
                    page_val = int(p_val.strip())
            elif " --page " in query_str:
                query_str, p_val = query_str.rsplit(" --page ", 1)
                if p_val.strip().isdigit():
                    page_val = int(p_val.strip())

            last_query = query_str.strip()

            try:
                ids = client.search_ids(query=last_query, page=page_val)
                if ids:
                    print(f"Document IDs found (Page {page_val}, count: {len(ids)}):")
                    for i, doc_id in enumerate(ids, 1):
                        print(f"[{i}] {doc_id}")
                else:
                    print(f"No documents found on page {page_val}.")
            except Exception as e:
                print(f"Error: {e}", file=sys.stderr)

        elif cmd == "page":
            if last_query is None:
                print("No previous search. Please run 'search <query>' first.")
                continue
            if not arg or not arg.isdigit():
                print("Usage: page <number>")
                continue

            page_num = int(arg)
            try:
                ids = client.search_ids(query=last_query, page=page_num)
                if ids:
                    print(f"Document IDs found for '{last_query}' (Page {page_num}, count: {len(ids)}):")
                    for i, doc_id in enumerate(ids, 1):
                        print(f"[{i}] {doc_id}")
                else:
                    print(f"No documents found on page {page_num}.")
            except Exception as e:
                print(f"Error: {e}", file=sys.stderr)

        elif cmd == "get":
            if not arg:
                print("Usage: get <id ...> (e.g. get 1042 2055 or get 1042, 2055)")
                continue

            raw_tokens = [t.strip() for t in arg.replace(",", " ").split() if t.strip()]
            target_ids: List[int] = []
            invalid_tokens: List[str] = []

            for token in raw_tokens:
                if token.isdigit():
                    target_ids.append(int(token))
                else:
                    invalid_tokens.append(token)

            if invalid_tokens:
                print(f"Invalid document ID(s): {', '.join(invalid_tokens)}. IDs must be integers.")
                if not target_ids:
                    continue

            unique_target_ids = list(dict.fromkeys(target_ids))

            try:
                docs = client.get_documents_by_ids(unique_target_ids)
                if docs:
                    docs_by_id = {doc.get("id"): doc for doc in docs if "id" in doc}
                    for tid in unique_target_ids:
                        doc = docs_by_id.get(tid)
                        if doc:
                            print(f"\nDocument #{tid}:")
                            source = doc.get("source", doc)
                            print(json.dumps(source, indent=2, ensure_ascii=False))
                        else:
                            print(f"\nDocument #{tid} not found.")
                else:
                    print(f"None of the requested documents were found: {unique_target_ids}")
            except Exception as e:
                print(f"Error: {e}", file=sys.stderr)

        else:
            print(f"Unknown command: '{cmd}'. Type 'help' for usage.")


def main():
    parser = argparse.ArgumentParser(
        prog="client.py",
        description="Interactive CLI Client for Inverted Index Search Engine (always uses Expanded search)",
    )
    parser.add_argument(
        "--host",
        "-H",
        default=DEFAULT_HOST,
        help=f"Backend base URL (default: {DEFAULT_HOST})",
    )
    parser.add_argument(
        "--timeout",
        "-t",
        type=float,
        default=30.0,
        help="HTTP request timeout in seconds (default: 30.0)",
    )

    args = parser.parse_args()
    client = InvertedIndexClient(base_url=args.host, timeout=args.timeout)
    run_interactive(client)


if __name__ == "__main__":
    main()
