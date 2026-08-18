# Klaat Update Command

The `update` command updates existing Codelabs within a directory. It will read the metadata from existing generated output and attempt to fetch the latest changes from the original source.

## Usage
`klaat update [flags]`

## Flags
The `update` command accepts the same flags as `export`, which apply to any Codelabs updated during the run.
- `-auth` : OAuth2 token to use for authentication (Google Docs).
- `-prefix` : URL prefix for assets. Default is `https://distribute.kartikarora.me`.
- `-ga` : Global Google Analytics tracking ID (optional).

## Examples
```bash
klaat update
```
