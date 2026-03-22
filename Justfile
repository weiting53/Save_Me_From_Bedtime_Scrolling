set shell := ["bash", "-cu"]

default:
	@just --list

setup:
	uv venv --clear
	uv sync
	uv run pre-commit install

lint:
	uv run ruff check . --fix
	uv run ruff format .

run:
	uv run python main.py