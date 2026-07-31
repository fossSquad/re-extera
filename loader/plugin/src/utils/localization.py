from elyx import strings
from org.telegram.messenger import LocaleController

def _localize(key):
    lang = LocaleController.getInstance().getCurrentLocale().getLanguage()
    return strings(key, locale=lang)
