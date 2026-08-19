# Klaat Demo Command

The `demo` command extracts the bundled showcase demo codelab into the current working directory, exports it to HTML, and immediately launches the local preview server.

## Usage

```bash
klaat demo [options]
```

## Options

- `-addr <host:port>`: Hostname and port to bind for the preview server (default: `localhost:9090`).
- `-o, --out <dir>`: Output directory for the exported demo codelab (default: `output`).
- `-f, --format <format>`: Output format (`html`, `md`, `offline`, default: `html`).
- `-prefix <url>`: Asset prefix URL (default: `https://distribute.kartikarora.me`).
- `--no-browser`: Do not automatically open the browser when the preview server starts.
- `--no-serve`: Extract and export only, without starting the preview server.

## Examples

```bash
# Extract showcase files, export to HTML, and start live preview
klaat demo

# Extract and export only without launching the local server
klaat demo --no-serve

# Run demo on custom port with offline format
klaat demo -addr localhost:8080 -f offline
```
