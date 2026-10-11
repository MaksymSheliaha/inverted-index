# Inverted Index Interactive CLI Client

Simple interactive Python CLI client for the Inverted Index Search Engine.

## Features
- **Always Expanded Search**: Automatically uses `EXPANDED` search mode (LLM query expansion).
- **Page Parameter Support**: Search with a specific page or browse pages with the `page` command.
- **IDs Only Output**: Search results display only the matching document IDs.
- **Batch Document Retrieval**: Retrieve multiple documents by their IDs in a single `get` command.
- **Zero External Dependencies**: Pure Python 3 standard library (`urllib`, `json`, `argparse`).

---

## Usage

Start the interactive shell:
```bash
python client.py
```
Or specify a custom host:
```bash
python client.py --host http://localhost:8080
```

### Commands in Interactive Mode
- `search <query> [-p page]` — Execute expanded search (returns doc IDs only).
- `page <number>` — Repeat the previous search on a different page.
- `get <id ...>` — Fetch and display one or multiple documents by ID:
  - Space-separated: `get 1042 2055 3110`
  - Comma-separated: `get 1042, 2055, 3110`
- `help` — Show command help.
- `exit` or `quit` — Exit the client.
