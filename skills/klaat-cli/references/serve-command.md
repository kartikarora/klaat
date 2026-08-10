# Klaat Serve Command

The `serve` command starts a local web server to preview generated Codelabs locally.

## Usage
`klaat serve [flags]`

## Flags

- `-addr` : `hostname:port` to bind the server to. Default is `localhost:9090`.
- `-d` or `--dir` : Directory to serve the files from. Default is the current directory (`.`).
- `--no-browser` : Prevents the tool from automatically opening the browser to preview the codelabs.

## Examples
```bash
klaat serve -addr localhost:8080 -d out/
klaat serve --no-browser
```
