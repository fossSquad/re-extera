from java import dynamic_proxy
from java.lang import Runnable

class UIRunnable(dynamic_proxy(Runnable)):
    def __init__(self, func):
        super().__init__()
        self.func = func
    def run(self):
        self.func()
