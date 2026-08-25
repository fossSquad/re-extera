"""re:extera Python loader package.

The loader is shipped as a single ``loader.plugin`` file assembled by
``build.py`` from the fragments below, concatenated in ``BUILD_ORDER``. Each
fragment shares one namespace (imports live in ``imports.py``), so the order
here is significant: a fragment may only use names defined by earlier ones.
"""

BUILD_ORDER = [
    "metadata.py",
    "imports.py",
    "constants.py",
    "utils.py",
    "config.py",
    "dex.py",
    "plugin.py",
]
