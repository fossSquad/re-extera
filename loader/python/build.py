# this is a script to build loader.plugin
# python3 build.py, output will be in build/plugin/loader.plugin
import os
import py_compile
import sys

LOADER_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(os.path.dirname(LOADER_DIR))

if LOADER_DIR not in sys.path:
    sys.path.insert(0, LOADER_DIR)

from __init__ import BUILD_ORDER

def build():
    print("Starting build process...")
    
    files = [os.path.join(LOADER_DIR, f) for f in BUILD_ORDER]
    
    for f in files:
        if not os.path.exists(f):
            print(f"Error: Required file not found: {f}")
            return
            
    print("Found files:")
    for f in files:
        print(f"  - {os.path.basename(f)}")

    print("\nChecking syntax of individual modules...")
    for f in files:
        try:
            py_compile.compile(f, doraise=True)
            print(f"  [OK] {os.path.basename(f)}")
        except py_compile.PyCompileError as e:
            print(f"\n[!] Syntax error in {os.path.basename(f)}:")
            print(e)
            return

    print("\nAssembling loader.plugin...")
    content = ""
    for f in files:
        with open(f, "r") as pyfile:
            content += pyfile.read()
            content += "\n\n"

    output_dir = os.path.join(PROJECT_ROOT, "build", "plugin")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "loader.plugin")
    
    with open(output_path, "w") as out:
        out.write(content)

    print("Checking syntax of assembled loader.plugin...")
    try:
        py_compile.compile(output_path, doraise=True)
        print("  [OK] loader.plugin syntax is valid.")
    except py_compile.PyCompileError as e:
        print("\n[!] Syntax error in final loader.plugin:")
        print(e)
        return

    print(f"\nSuccess! Generated loader.plugin in {os.path.relpath(output_dir, PROJECT_ROOT)}")

if __name__ == "__main__":
    build()
