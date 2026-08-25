#!/usr/bin/env python3
"""Assemble ``loader.plugin`` from the loader fragments.

Usage:
    python3 loader/build.py            # assemble + syntax check + ruff lint
    python3 loader/build.py --no-lint  # skip the ruff lint step

The loader is shipped as one file: the fragments in ``loader/*.py`` are
concatenated in :data:`loader.BUILD_ORDER` order and written to
``build/plugin/loader.plugin``. Output goes to ``build/plugin/loader.plugin``.
"""

import argparse
import os
import py_compile
import shutil
import subprocess
import sys


LOADER_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(LOADER_DIR)

if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

from loader import BUILD_ORDER  # noqa: E402  (import needs ROOT_DIR on sys.path)


def _syntax_check(path):
    """Return True if ``path`` compiles, otherwise print the error and return False."""
    try:
        py_compile.compile(path, doraise=True)
        return True
    except py_compile.PyCompileError as exc:
        print(f"[-] Syntax error in {os.path.basename(path)}: {exc}")
        return False


def _assemble(files, output_path):
    parts = []
    for path in files:
        with open(path, encoding="utf-8") as fragment:
            parts.append(fragment.read().strip())
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    # Two blank lines between fragments keeps top-level defs PEP 8-compliant.
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n\n\n".join(parts) + "\n")


def _lint(output_path):
    """Run ruff on the assembled plugin. Returns True on success/skip."""
    if shutil.which("ruff") is None:
        print("[*] ruff not installed (pip install ruff) — skipping lint")
        return True
    result = subprocess.run(
        ["ruff", "check", output_path],
        cwd=ROOT_DIR,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        print("[-] ruff reported issues in the assembled loader.plugin")
        print(result.stdout.strip() or result.stderr.strip())
        return False
    print("[+] ruff check passed")
    return True


def build(lint=True):
    print("-" * 80)
    print("[*] Starting build process")
    print("-" * 80)

    files = [os.path.join(LOADER_DIR, name) for name in BUILD_ORDER]
    missing = [f for f in files if not os.path.exists(f)]
    if missing:
        for f in missing:
            print(f"[-] Required file not found: {f}")
        return False

    print("[*] Found source fragments:")
    for f in files:
        print(f"    - {os.path.basename(f)}")

    print("-" * 80)
    print("[*] Checking syntax of individual modules")
    for f in files:
        if not _syntax_check(f):
            return False
        print(f"[+] {os.path.basename(f)}")

    print("-" * 80)
    print("[*] Assembling loader.plugin")
    output_dir = os.path.join(ROOT_DIR, "build", "plugin")
    output_path = os.path.join(output_dir, "loader.plugin")
    _assemble(files, output_path)

    print("[*] Checking syntax of assembled loader.plugin")
    if not _syntax_check(output_path):
        return False
    print("[+] loader.plugin syntax is valid")

    if lint:
        print("-" * 80)
        print("[*] Linting assembled loader.plugin with ruff")
        if not _lint(output_path):
            return False

    print("-" * 80)
    print(f"[+] Success! Generated loader.plugin in {os.path.relpath(output_dir, ROOT_DIR)}")
    print("-" * 80)
    return True


def main():
    parser = argparse.ArgumentParser(description="Build loader.plugin")
    parser.add_argument("--no-lint", action="store_true", help="skip the ruff lint step")
    args = parser.parse_args()
    sys.exit(0 if build(lint=not args.no_lint) else 1)


if __name__ == "__main__":
    main()
