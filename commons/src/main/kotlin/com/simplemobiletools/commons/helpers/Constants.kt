package com.simplemobiletools.commons.helpers

const val APP_NAME = "app_name"
const val APP_LICENSES = "app_licenses"
const val APP_VERSION_NAME = "app_version_name"
const val REAL_FILE_PATH = "real_file_path"
const val IS_FROM_GALLERY = "is_from_gallery"
const val BROADCAST_REFRESH_MEDIA = "com.simplemobiletools.REFRESH_MEDIA"
const val OTG_PATH = "otg:/"

// shared preferences
const val PREFS_KEY = "Prefs"
const val APP_RUN_COUNT = "app_run_count"
const val LAST_VERSION = "last_version"
const val TREE_URI = "tree_uri_2"
const val OTG_TREE_URI = "otg_tree_uri"
const val OTG_BASE_PATH = "otg_base_path"
const val SD_CARD_PATH = "sd_card_path_2"
const val INTERNAL_STORAGE_PATH = "internal_storage_path"
const val TEXT_COLOR = "text_color"
const val BACKGROUND_COLOR = "background_color"
const val PRIMARY_COLOR = "primary_color_2"
const val CUSTOM_TEXT_COLOR = "custom_text_color"
const val CUSTOM_BACKGROUND_COLOR = "custom_background_color"
const val CUSTOM_PRIMARY_COLOR = "custom_primary_color"
const val WIDGET_BG_COLOR = "widget_bg_color"
const val WIDGET_TEXT_COLOR = "widget_text_color"
const val PASSWORD_PROTECTION = "password_protection"
const val PASSWORD_HASH = "password_hash"
const val PROTECTION_TYPE = "protection_type"
const val APP_PASSWORD_PROTECTION = "app_password_protection"
const val APP_PASSWORD_HASH = "app_password_hash"
const val APP_PROTECTION_TYPE = "app_protection_type"
const val KEEP_LAST_MODIFIED = "keep_last_modified"
const val USE_ENGLISH = "use_english"
const val WAS_USE_ENGLISH_TOGGLED = "was_use_english_toggled"
const val WAS_SHARED_THEME_EVER_ACTIVATED = "was_shared_theme_ever_activated"
const val IS_USING_SHARED_THEME = "is_using_shared_theme"
const val WAS_SHARED_THEME_FORCED = "was_shared_theme_forced"
const val WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN = "was_custom_theme_switch_description_shown"
const val WAS_SHARED_THEME_AFTER_UPDATE_CHECKED = "was_shared_theme_after_update_checked"
const val SHOW_INFO_BUBBLE = "show_info_bubble"
const val SORTING = "sorting"
const val LAST_CONFLICT_RESOLUTION = "last_conflict_resolution"
const val LAST_CONFLICT_APPLY_TO_ALL = "last_conflict_apply_to_all"
const val AVOID_WHATS_NEW = "avoid_whats_new"

// licenses
const val LICENSE_KOTLIN = 1
const val LICENSE_SUBSAMPLING = 2
const val LICENSE_GLIDE = 4
const val LICENSE_CROPPER = 8
const val LICENSE_MULTISELECT = 16
const val LICENSE_RTL = 32
const val LICENSE_JODA = 64
const val LICENSE_STETHO = 128
const val LICENSE_OTTO = 256
const val LICENSE_PHOTOVIEW = 512
const val LICENSE_PICASSO = 1024
const val LICENSE_PATTERN = 2048
const val LICENSE_REPRINT = 4096
const val LICENSE_GIF_DRAWABLE = 8192
const val LICENSE_AUTOFITTEXTVIEW = 16384
const val LICENSE_ROBOLECTRIC = 32768
const val LICENSE_ESPRESSO = 65536
const val LICENSE_GSON = 131072
const val LICENSE_LEAK_CANARY = 262144

// global intents
const val OPEN_DOCUMENT_TREE = 1000
const val OPEN_DOCUMENT_TREE_OTG = 1001
const val REQUEST_SET_AS = 1002
const val REQUEST_EDIT_IMAGE = 1003

// sorting
const val SORT_BY_NAME = 1
const val SORT_BY_DATE_MODIFIED = 2
const val SORT_BY_SIZE = 4
const val SORT_BY_DATE_TAKEN = 8
const val SORT_BY_EXTENSION = 16
const val SORT_BY_PATH = 32
const val SORT_BY_NUMBER = 64
const val SORT_BY_FIRST_NAME = 128
const val SORT_BY_MIDDLE_NAME = 256
const val SORT_BY_SURNAME = 512
const val SORT_DESCENDING = 1024

// security
const val PROTECTION_PATTERN = 0
const val PROTECTION_PIN = 1
const val PROTECTION_FINGERPRINT = 2

const val SHOW_ALL_TABS = -1
const val SHOW_PATTERN = 0
const val SHOW_PIN = 1
const val SHOW_FINGERPRINT = 2

// permissions
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2
const val PERMISSION_CAMERA = 3
const val PERMISSION_RECORD_AUDIO = 4
const val PERMISSION_READ_CONTACTS = 5
const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_READ_CALENDAR = 7
const val PERMISSION_WRITE_CALENDAR = 8
const val PERMISSION_CALL_PHONE = 9

// conflict resolving
const val CONFLICT_SKIP = 1
const val CONFLICT_OVERWRITE = 2
const val CONFLICT_MERGE = 3

fun getDateFormats() = arrayListOf(
        "yyyy-MM-dd",
        "yyyyMMdd",
        "yyyy.MM.dd",
        "yy-MM-dd",
        "yyMMdd",
        "yy.MM.dd",
        "yy/MM/dd",
        "MM-dd",
        "--MM-dd",
        "MMdd",
        "MM/dd",
        "MM.dd"
)
