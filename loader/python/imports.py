from typing import Any, List
from base_plugin import BasePlugin, MethodHook
from client_utils import get_last_fragment
import requests
import json
import re
import io
import zipfile
import shutil
import os
import time
import threading
from android.app import Activity
from hook_utils import find_class
from ui.bulletin import BulletinHelper
from ui.settings import Header, Divider, Text, Switch, Selector
from ui.alert import AlertDialogBuilder
from org.telegram.ui import LaunchActivity
from org.telegram.messenger import LocaleController, BuildVars, ApplicationLoader, FileLoader, NotificationCenter, MessageObject, AndroidUtilities
from java.nio import ByteBuffer
from dalvik.system import InMemoryDexClassLoader, DexClassLoader
from java import dynamic_proxy
from java.lang import Runnable