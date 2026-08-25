"""Shared runtime imports for every loader fragment.

The loader ships as a single file assembled from ``loader/*.py`` in the order
declared by :data:`loader.BUILD_ORDER`. All fragments share this one import
block, so every import the loader needs is declared here rather than in the
individual modules. Names below are provided partly by the Python standard
library and partly by the exteraGram / Chaquopy plugin runtime.
"""

import datetime
import io
import json
import os
import threading
import time
import zipfile
from typing import Any, List

import requests
from android.app import Activity
from base_plugin import BasePlugin
from client_utils import get_last_fragment
from dalvik.system import DexClassLoader, InMemoryDexClassLoader
from hook_utils import find_class
from java import dynamic_proxy
from java.lang import Runnable
from java.nio import ByteBuffer
from org.telegram.messenger import (
    AndroidUtilities,
    ApplicationLoader,
    BuildVars,
    LocaleController,
)
from ui.alert import AlertDialogBuilder
from ui.bulletin import BulletinHelper
from ui.settings import Divider, Selector, Text
