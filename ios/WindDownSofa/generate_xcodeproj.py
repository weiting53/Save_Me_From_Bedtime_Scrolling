#!/usr/bin/env python3
"""
Generates WindDownSofa.xcodeproj/project.pbxproj for the Wind Down Sofa iOS app.
Run this script once from the ios/WindDownSofa/ directory.
"""

import os
import uuid


def new_id():
    return uuid.uuid4().hex.upper()[:24]


# ─── File definitions ──────────────────────────────────────────────────────────
# (path_from_WindDownSofa_folder, display_name)

SOURCE_FILES = [
    # App
    ("App/WindDownSofaApp.swift", "WindDownSofaApp.swift"),
    ("App/AppDelegate.swift", "AppDelegate.swift"),
    ("App/ContentView.swift", "ContentView.swift"),
    # Features – Home
    ("Features/Home/HomeView.swift", "HomeView.swift"),
    ("Features/Home/HomeViewModel.swift", "HomeViewModel.swift"),
    # Features – WindDown
    ("Features/WindDown/WindDownView.swift", "WindDownView.swift"),
    ("Features/WindDown/WindDownViewModel.swift", "WindDownViewModel.swift"),
    # Features – History
    ("Features/History/HistoryView.swift", "HistoryView.swift"),
    ("Features/History/HistoryViewModel.swift", "HistoryViewModel.swift"),
    # Features – Settings
    ("Features/Settings/SettingsView.swift", "SettingsView.swift"),
    ("Features/Settings/SettingsViewModel.swift", "SettingsViewModel.swift"),
    # Features – Onboarding
    ("Features/Onboarding/OnboardingView.swift", "OnboardingView.swift"),
    ("Features/Onboarding/OnboardingViewModel.swift", "OnboardingViewModel.swift"),
    # Core – Notification
    ("Core/Notification/NotificationManager.swift", "NotificationManager.swift"),
    # Core – Storage
    ("Core/Storage/SleepLog.swift", "SleepLog.swift"),
    ("Core/Storage/AppSettings.swift", "AppSettings.swift"),
    ("Core/Storage/StorageManager.swift", "StorageManager.swift"),
    # Core – Methods
    ("Core/Methods/WindDownMethodProtocol.swift", "WindDownMethodProtocol.swift"),
    ("Core/Methods/MethodRegistry.swift", "MethodRegistry.swift"),
    ("Core/Methods/Breathing478View.swift", "Breathing478View.swift"),
    ("Core/Methods/DailyReflectionView.swift", "DailyReflectionView.swift"),
    # UI
    ("UI/Theme.swift", "Theme.swift"),
    # UI – Components
    ("UI/Components/FoxMascotView.swift", "FoxMascotView.swift"),
    ("UI/Components/MethodCarouselView.swift", "MethodCarouselView.swift"),
    ("UI/Components/SleepTimeCardView.swift", "SleepTimeCardView.swift"),
    ("UI/Components/StreakCardView.swift", "StreakCardView.swift"),
    ("UI/Components/ReminderTimeCardView.swift", "ReminderTimeCardView.swift"),
]

RESOURCE_FILES = [
    ("Resources/Info.plist", "Info.plist"),
    ("Resources/WindDownSofa.entitlements", "WindDownSofa.entitlements"),
]

# ─── Assign stable UUIDs ───────────────────────────────────────────────────────

# Top-level IDs
ID = {
    "project": "AA000000000000000000001",
    "main_target": "AA000000000000000000002",
    "products_group": "AA000000000000000000003",
    "main_group": "AA000000000000000000004",
    "sources_phase": "AA000000000000000000005",
    "frameworks_phase": "AA000000000000000000006",
    "resources_phase": "AA000000000000000000007",
    "debug_config": "AA000000000000000000008",
    "release_config": "AA000000000000000000009",
    "project_debug": "AA000000000000000000010",
    "project_release": "AA000000000000000000011",
    "config_list_proj": "AA000000000000000000012",
    "config_list_tgt": "AA000000000000000000013",
    "app_product": "AA000000000000000000014",
    # Groups
    "grp_WindDownSofa": "AA000000000000000000020",
    "grp_App": "AA000000000000000000021",
    "grp_Features": "AA000000000000000000022",
    "grp_Home": "AA000000000000000000023",
    "grp_WindDown": "AA000000000000000000024",
    "grp_History": "AA000000000000000000025",
    "grp_Settings": "AA000000000000000000026",
    "grp_Onboarding": "AA000000000000000000027",
    "grp_Core": "AA000000000000000000028",
    "grp_Notification": "AA000000000000000000029",
    "grp_Storage": "AA000000000000000000030",
    "grp_Methods": "AA000000000000000000031",
    "grp_UI": "AA000000000000000000032",
    "grp_Components": "AA000000000000000000033",
    "grp_Resources": "AA000000000000000000034",
}


def pad(s):
    return s.ljust(24)


# Assign file ref and build file IDs
file_refs = {}  # path -> fileref_id
build_files = {}  # path -> buildfile_id

for i, (path, name) in enumerate(SOURCE_FILES + RESOURCE_FILES):
    n = 100 + i
    file_refs[path] = f"AA0000000000000000{n:05d}"
    build_files[path] = f"AA0000000000000000{n + 500:05d}"

# ─── Builders ─────────────────────────────────────────────────────────────────


def pbx_build_file_section():
    lines = ["/* Begin PBXBuildFile section */"]
    for path, name in SOURCE_FILES:
        fid = file_refs[path]
        bid = build_files[path]
        lines.append(
            f"\t\t{bid} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {fid} /* {name} */; }};"
        )
    lines.append("/* End PBXBuildFile section */")
    return "\n".join(lines)


def pbx_file_reference_section():
    lines = ["/* Begin PBXFileReference section */"]
    # App product
    lines.append(
        f"\t\t{ID['app_product']} /* WindDownSofa.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = WindDownSofa.app; sourceTree = BUILT_PRODUCTS_DIR; }};"
    )
    for path, name in SOURCE_FILES:
        fid = file_refs[path]
        lines.append(
            f'\t\t{fid} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {name}; sourceTree = "<group>"; }};'
        )
    for path, name in RESOURCE_FILES:
        fid = file_refs[path]
        ext = name.split(".")[-1]
        if ext == "plist":
            ftype = "text.plist.xml"
        else:
            ftype = "text.plist.entitlements"
        lines.append(
            f'\t\t{fid} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = {ftype}; path = {name}; sourceTree = "<group>"; }};'
        )
    lines.append("/* End PBXFileReference section */")
    return "\n".join(lines)


def file_ids_for_group(paths):
    return [file_refs[p] for p in paths if p in file_refs]


def pbx_group_section():
    lines = ["/* Begin PBXGroup section */"]

    def group(gid, name, children, path=None):
        path_str = f"path = {path};" if path else f"name = {name};"
        child_lines = "\n".join(f"\t\t\t\t{c}," for c in children)
        return f"""
\t\t{gid} /* {name} */ = {{
\t\t\tisa = PBXGroup;
\t\t\tchildren = (
{child_lines}
\t\t\t);
\t\t\t{path_str}
\t\t\tsourceTree = \"<group>\";
\t\t}};"""

    # Main group
    lines.append(
        group(
            ID["main_group"],
            "WindDownSofa",
            [
                ID["grp_WindDownSofa"],
                ID["products_group"],
            ],
        )
    )

    # Products group
    lines.append(group(ID["products_group"], "Products", [ID["app_product"]]))

    # WindDownSofa group
    lines.append(
        group(
            ID["grp_WindDownSofa"],
            "WindDownSofa",
            [
                ID["grp_App"],
                ID["grp_Features"],
                ID["grp_Core"],
                ID["grp_UI"],
                ID["grp_Resources"],
            ],
            path="WindDownSofa",
        )
    )

    # App group
    app_files = [p for p, _ in SOURCE_FILES if p.startswith("App/")]
    lines.append(group(ID["grp_App"], "App", file_ids_for_group(app_files), path="App"))

    # Features group
    lines.append(
        group(
            ID["grp_Features"],
            "Features",
            [
                ID["grp_Home"],
                ID["grp_WindDown"],
                ID["grp_History"],
                ID["grp_Settings"],
                ID["grp_Onboarding"],
            ],
            path="Features",
        )
    )

    # Feature sub-groups
    for grp_key, prefix, path_name in [
        ("grp_Home", "Features/Home/", "Home"),
        ("grp_WindDown", "Features/WindDown/", "WindDown"),
        ("grp_History", "Features/History/", "History"),
        ("grp_Settings", "Features/Settings/", "Settings"),
        ("grp_Onboarding", "Features/Onboarding/", "Onboarding"),
    ]:
        files = [p for p, _ in SOURCE_FILES if p.startswith(prefix)]
        lines.append(
            group(ID[grp_key], path_name, file_ids_for_group(files), path=path_name)
        )

    # Core group
    lines.append(
        group(
            ID["grp_Core"],
            "Core",
            [
                ID["grp_Notification"],
                ID["grp_Storage"],
                ID["grp_Methods"],
            ],
            path="Core",
        )
    )

    # Core sub-groups
    for grp_key, prefix, path_name in [
        ("grp_Notification", "Core/Notification/", "Notification"),
        ("grp_Storage", "Core/Storage/", "Storage"),
        ("grp_Methods", "Core/Methods/", "Methods"),
    ]:
        files = [p for p, _ in SOURCE_FILES if p.startswith(prefix)]
        lines.append(
            group(ID[grp_key], path_name, file_ids_for_group(files), path=path_name)
        )

    # UI group
    lines.append(
        group(
            ID["grp_UI"],
            "UI",
            [
                file_refs["UI/Theme.swift"],
                ID["grp_Components"],
            ],
            path="UI",
        )
    )

    # Components group
    comp_files = [p for p, _ in SOURCE_FILES if p.startswith("UI/Components/")]
    lines.append(
        group(
            ID["grp_Components"],
            "Components",
            file_ids_for_group(comp_files),
            path="Components",
        )
    )

    # Resources group
    res_files = [file_refs[p] for p, _ in RESOURCE_FILES]
    lines.append(group(ID["grp_Resources"], "Resources", res_files, path="Resources"))

    lines.append("/* End PBXGroup section */")
    return "\n".join(lines)


def pbx_native_target_section():
    return f"""/* Begin PBXNativeTarget section */
\t\t{ID["main_target"]} /* WindDownSofa */ = {{
\t\t\tisa = PBXNativeTarget;
\t\t\tbuildConfigurationList = {ID["config_list_tgt"]} /* Build configuration list for PBXNativeTarget "WindDownSofa" */;
\t\t\tbuildPhases = (
\t\t\t\t{ID["sources_phase"]} /* Sources */,
\t\t\t\t{ID["frameworks_phase"]} /* Frameworks */,
\t\t\t\t{ID["resources_phase"]} /* Resources */,
\t\t\t);
\t\t\tbuildRules = (
\t\t\t);
\t\t\tdependencies = (
\t\t\t);
\t\t\tname = WindDownSofa;
\t\t\tpackageProductDependencies = (
\t\t\t);
\t\t\tproductName = WindDownSofa;
\t\t\tproductReference = {ID["app_product"]} /* WindDownSofa.app */;
\t\t\tproductType = "com.apple.product-type.application";
\t\t}};
/* End PBXNativeTarget section */"""


def pbx_project_section():
    return f"""/* Begin PBXProject section */
\t\t{ID["project"]} /* Project object */ = {{
\t\t\tisa = PBXProject;
\t\t\tattributes = {{
\t\t\t\tBuildIndependentTargetsInParallel = 1;
\t\t\t\tLastSwiftUpdateCheck = 1500;
\t\t\t\tLastUpgradeCheck = 1500;
\t\t\t\tTargetAttributes = {{
\t\t\t\t\t{ID["main_target"]} = {{
\t\t\t\t\t\tCreatedOnToolsVersion = 15.0;
\t\t\t\t\t}};
\t\t\t\t}};
\t\t\t}};
\t\t\tbuildConfigurationList = {ID["config_list_proj"]} /* Build configuration list for PBXProject "WindDownSofa" */;
\t\t\tcompatibilityVersion = "Xcode 14.0";
\t\t\tdevelopmentRegion = zh_TW;
\t\t\thasScannedForEncodings = 0;
\t\t\tknownRegions = (
\t\t\t\ten,
\t\t\t\tBase,
\t\t\t\tzh_TW,
\t\t\t);
\t\t\tmainGroup = {ID["main_group"]};
\t\t\tproductRefGroup = {ID["products_group"]} /* Products */;
\t\t\tprojectDirPath = "";
\t\t\tprojectRoot = "";
\t\t\ttargets = (
\t\t\t\t{ID["main_target"]} /* WindDownSofa */,
\t\t\t);
\t\t}};
/* End PBXProject section */"""


def pbx_sources_phase():
    lines = [
        "/* Begin PBXSourcesBuildPhase section */",
        f"\t\t{ID['sources_phase']} /* Sources */ = {{",
        "\t\t\tisa = PBXSourcesBuildPhase;",
        "\t\t\tbuildActionMask = 2147483647;",
        "\t\t\tfiles = (",
    ]
    for path, name in SOURCE_FILES:
        bid = build_files[path]
        lines.append(f"\t\t\t\t{bid} /* {name} in Sources */,")
    lines += [
        "\t\t\t);",
        "\t\t\trunOnlyForDeploymentPostprocessing = 0;",
        "\t\t};",
        "/* End PBXSourcesBuildPhase section */",
    ]
    return "\n".join(lines)


def pbx_frameworks_phase():
    return f"""/* Begin PBXFrameworksBuildPhase section */
\t\t{ID["frameworks_phase"]} /* Frameworks */ = {{
\t\t\tisa = PBXFrameworksBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};
/* End PBXFrameworksBuildPhase section */"""


def pbx_resources_phase():
    return f"""/* Begin PBXResourcesBuildPhase section */
\t\t{ID["resources_phase"]} /* Resources */ = {{
\t\t\tisa = PBXResourcesBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};
/* End PBXResourcesBuildPhase section */"""


def xc_build_configuration_section():
    return f"""/* Begin XCBuildConfiguration section */
\t\t{ID["debug_config"]} /* Debug */ = {{
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {{
\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;
\t\t\t\tASSET_CATALOG_COMPILER_OPTIMIZATION = time;
\t\t\t\tCLANG_ANALYZER_NONNULL = YES;
\t\t\t\tCLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
\t\t\t\tCLANG_ENABLE_MODULES = YES;
\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;
\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;
\t\t\t\tCOPY_PHASE_STRIP = NO;
\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;
\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;
\t\t\t\tENABLE_TESTABILITY = YES;
\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu11;
\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;
\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;
\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;
\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (
\t\t\t\t\t"DEBUG=1",
\t\t\t\t\t"$(inherited)",
\t\t\t\t);
\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
\t\t\t\tMTL_FAST_MATH = YES;
\t\t\t\tONLY_ACTIVE_ARCH = YES;
\t\t\t\tSDKROOT = iphoneos;
\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = "-Onone";
\t\t\t}};
\t\t\tname = Debug;
\t\t}};
\t\t{ID["release_config"]} /* Release */ = {{
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {{
\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;
\t\t\t\tCLANG_ANALYZER_NONNULL = YES;
\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
\t\t\t\tCLANG_ENABLE_MODULES = YES;
\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;
\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;
\t\t\t\tCOPY_PHASE_STRIP = NO;
\t\t\t\tDEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
\t\t\t\tENABLE_NS_ASSERTIONS = NO;
\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;
\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu11;
\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;
\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;
\t\t\t\tMTL_FAST_MATH = YES;
\t\t\t\tSDKROOT = iphoneos;
\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;
\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = "-O";
\t\t\t\tVALIDATE_PRODUCT = YES;
\t\t\t}};
\t\t\tname = Release;
\t\t}};
\t\t{ID["project_debug"]} /* Debug */ = {{
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {{
\t\t\t\tASSET_CATALOG_COMPILER_OPTIMIZATION = time;
\t\t\t\tCODE_SIGN_ENTITLEMENTS = "WindDownSofa/Resources/WindDownSofa.entitlements";
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tDEVELOPMENT_ASSET_PATHS = "";
\t\t\t\tDEVELOPMENT_TEAM = "";
\t\t\t\tENABLE_PREVIEWS = YES;
\t\t\t\tGENERATE_INFOPLIST_FILE = NO;
\t\t\t\tINFOPLIST_FILE = "WindDownSofa/Resources/Info.plist";
\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;
\t\t\t\tLE_SWIFT_VERSION = 5.0;
\t\t\t\tMARKETING_VERSION = 1.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = "com.yourname.WindDownSofa";
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tTARGETED_DEVICE_FAMILY = "1,2";
\t\t\t}};
\t\t\tname = Debug;
\t\t}};
\t\t{ID["project_release"]} /* Release */ = {{
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {{
\t\t\t\tASSET_CATALOG_COMPILER_OPTIMIZATION = time;
\t\t\t\tCODE_SIGN_ENTITLEMENTS = "WindDownSofa/Resources/WindDownSofa.entitlements";
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tDEVELOPMENT_ASSET_PATHS = "";
\t\t\t\tDEVELOPMENT_TEAM = "";
\t\t\t\tENABLE_PREVIEWS = YES;
\t\t\t\tGENERATE_INFOPLIST_FILE = NO;
\t\t\t\tINFOPLIST_FILE = "WindDownSofa/Resources/Info.plist";
\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;
\t\t\t\tLE_SWIFT_VERSION = 5.0;
\t\t\t\tMARKETING_VERSION = 1.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = "com.yourname.WindDownSofa";
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tTARGETED_DEVICE_FAMILY = "1,2";
\t\t\t}};
\t\t\tname = Release;
\t\t}};
/* End XCBuildConfiguration section */"""


def xc_config_list_section():
    return f"""/* Begin XCConfigurationList section */
\t\t{ID["config_list_proj"]} /* Build configuration list for PBXProject "WindDownSofa" */ = {{
\t\t\tisa = XCConfigurationList;
\t\t\tbuildConfigurations = (
\t\t\t\t{ID["debug_config"]} /* Debug */,
\t\t\t\t{ID["release_config"]} /* Release */,
\t\t\t);
\t\t\tdefaultConfigurationIsVisible = 0;
\t\t\tdefaultConfigurationName = Release;
\t\t}};
\t\t{ID["config_list_tgt"]} /* Build configuration list for PBXNativeTarget "WindDownSofa" */ = {{
\t\t\tisa = XCConfigurationList;
\t\t\tbuildConfigurations = (
\t\t\t\t{ID["project_debug"]} /* Debug */,
\t\t\t\t{ID["project_release"]} /* Release */,
\t\t\t);
\t\t\tdefaultConfigurationIsVisible = 0;
\t\t\tdefaultConfigurationName = Release;
\t\t}};
/* End XCConfigurationList section */"""


def generate():
    content = f"""// !$*UTF8*$!
{{
\tarchiveVersion = 1;
\tclasses = {{
\t}};
\tobjectVersion = 56;
\tobjects = {{

{pbx_build_file_section()}

{pbx_file_reference_section()}

{pbx_frameworks_phase()}

{pbx_group_section()}

{pbx_native_target_section()}

{pbx_project_section()}

{pbx_resources_phase()}

{pbx_sources_phase()}

{xc_build_configuration_section()}

{xc_config_list_section()}

\t}};
\trootObject = {ID["project"]} /* Project object */;
}}
"""
    out = os.path.join(
        os.path.dirname(__file__), "WindDownSofa.xcodeproj", "project.pbxproj"
    )
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w") as f:
        f.write(content)
    print(f"✅  Written: {out}")
    print(
        f"   Registered {len(SOURCE_FILES)} source files + {len(RESOURCE_FILES)} resource files"
    )


if __name__ == "__main__":
    generate()
