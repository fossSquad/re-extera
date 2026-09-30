CLASS_NAME = "ni.shikatu.re_extera.Main"
METHOD_NAME = "start"
DEV_ARTIFACT_URL = "https://nightly.link/fossSquad/re-extera/workflows/build/master/re-extera-dev.zip"
DEV_API_URL = "https://api.github.com/repos/fossSquad/re-extera/actions/workflows/build.yml/runs?branch=master&per_page=1&status=success"
RELEASE_API_URL = "https://api.github.com/repos/fossSquad/re-extera/releases"
LOCAL_DEX_PATH = "/storage/emulated/0/Android/media/com.exteragram.messenger/classes.dex"
CACHE_DIR_NAME = "re_extera_cache"
DEX_OPT_DIR_NAME = "dex_opt"
VERSION_FIELD_NAME = "VERSION"
USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:152.0) Gecko/20100101 Firefox/152.0"
DEV_RUN_URL_TEMPLATE = "https://nightly.link/fossSquad/re-extera/actions/runs/{}/re-extera-dev.zip"
DEV_DOWNLOAD_HEADERS = {
    "User-Agent": USER_AGENT,
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
    "Accept-Encoding": "gzip, deflate, br, zstd",
    "Connection": "keep-alive",
    "Referer": "https://nightly.link/fossSquad/re-extera/workflows/build/master?preview",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Priority": "u=0, i",
}