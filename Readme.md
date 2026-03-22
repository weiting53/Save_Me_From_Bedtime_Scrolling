
## Setup

This project uses:

- [`uv`](https://docs.astral.sh/uv/) for Python and dependency management
- [`just`](https://just.systems/man/en/) for common development commands
- [`pre-commit`](https://pre-commit.com/) for commit-time checks
- [`ruff`](https://docs.astral.sh/ruff/) for linting and formatting

Please download these libraries beforing using.

### Setup project Enviroment
```bash
just setup
```
This command will:
- create a local virtual environment in .venv if needed
- install and sync project dependencies with uv sync
- install the Git pre-commit hook

### Lint
```bash
just lint
```
This runs Ruff to check and format the codebase.

### Run
```bash
just run
```