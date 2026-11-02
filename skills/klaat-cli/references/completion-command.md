# Completion Command Reference

Generate shell completion scripts for Bash, Zsh, or Fish.

## Usage

```bash
klaat completion <shell>
```

Where `<shell>` is one of:
- `bash`
- `zsh`
- `fish`

## Shell Configuration

### Zsh
Add the following to your `~/.zshrc`:
```zsh
eval "$(klaat completion zsh)"
```

### Bash
Add the following to your `~/.bashrc`:
```bash
eval "$(klaat completion bash)"
```

### Fish
Save completion to fish completions directory:
```fish
klaat completion fish > ~/.config/fish/completions/klaat.fish
```
