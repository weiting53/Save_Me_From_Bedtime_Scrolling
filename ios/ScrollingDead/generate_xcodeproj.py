#!/usr/bin/env python3
"""
Generates ScrollingDead.xcodeproj/project.pbxproj for the Scrolling Dead iOS app.
Run this script once from the ios/ScrollingDead/ directory.
"""

import os

# ─── File definitions ──────────────────────────────────────────────────────────
# (path_from_ScrollingDead_folder, display_name)

SOURCE_FILES = [
    ("ScrollingDeadApp.swift", "ScrollingDeadApp.swift"),
    ("OnboardingView.swift", "OnboardingView.swift"),
    ("NotificationManager.swift", "NotificationManager.swift"),
    ("Strings.swift", "Strings.swift"),
]

RESOURCE_FILES = [
    ("Resources/Info.plist", "Info.plist"),
    ("Resources/ScrollingDead.entitlements", "ScrollingDead.entitlements"),
]

# ─── Stable UUIDs ──────────────────────────────────────────────────────────────

ID = {
    "project": "BB000000000000000000001",
    "main_target": "BB000000000000000000002",
    "products_group": "BB000000000000000000003",
    "main_group": "BB000000000000000000004",
    "sources_phase": "BB000000000000000000005",
    "frameworks_phase": "BB000000000000000000006",
    "resources_phase": "BB000000000000000000007",
    "debug_config": "BB000000000000000000008",
    "release_config": "BB000000000000000000009",
    "project_debug": "BB000000000000000000010",
    "project_release": "BB000000000000000000011",
    "config_list_proj": "BB000000000000000000012",
    "config_list_tgt": "BB000000000000000000013",
    "app_product": "BB000000000000000000014",
    # Groups
    "grp_root": "BB000000000000000000020",
    "grp_sources": "BB000000000000000000021",
    "grp_resources": "BB000000000000000000022",
}

# Assign file ref and build file IDs
file_refs = {}
build_files = {}

for i, (path, _) in enumerate(SOURCE_FILES + RESOURCE_FILES):
    n = 100 + i
    file_refs[path] = f"BB0000000000000000{n:05d}"
    build_files[path] = f"BB0000000000000000{n + 500:05d}"


# ─── Section builders ──────────────────────────────────────────────────────────


def pbx_build_file_section():
    lines = ["/* Begin PBXBuildFile section */"]
    for path, name in SOURCE_FILES:
        lines.append(
            f"\t\t{build_files[path]} /* {name} in Sources */ = "
            f"{{isa = PBXBuildFile; fileRef = {file_refs[path]} /* {name} */; }};"
        )
    lines.append("/* End PBXBuildFile section */")
    return "\n".join(lines)


def pbx_file_reference_section():
    lines = ["/* Begin PBXFileReference section */"]
    lines.append(
        f"\t\t{ID['app_product']} /* ScrollingDead.app */ = "
        f"{{isa = PBXFileReference; explicitFileType = wrapper.application; "
        f"includeInIndex = 0; path = ScrollingDead.app; sourceTree = BUILT_PRODUCTS_DIR; }};"
    )
    for path, name in SOURCE_FILES:
        lines.append(
            f"\t\t{file_refs[path]} /* {name} */ = "
            f"{{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; "
            f'path = {name}; sourceTree = "<group>"; }};'
        )
    for path, name in RESOURCE_FILES:
        ext = name.split(".")[-1]
        ftype = "text.plist.xml" if ext == "plist" else "text.plist.entitlements"
        lines.append(
            f"\t\t{file_refs[path]} /* {name} */ = "
            f"{{isa = PBXFileReference; lastKnownFileType = {ftype}; "
            f'path = {name}; sourceTree = "<group>"; }};'
        )
    lines.append("/* End PBXFileReference section */")
    return "\n".join(lines)


def pbx_group_section():
    def group(gid, name, children, path=None):
        loc = f"path = {path};" if path else f"name = {name};"
        kids = "\n".join(f"\t\t\t\t{c}," for c in children)
        return (
            f"\n\t\t{gid} /* {name} */ = {{\n"
            f"\t\t\tisa = PBXGroup;\n"
            f"\t\t\tchildren = (\n{kids}\n\t\t\t);\n"
            f"\t\t\t{loc}\n"
            f'\t\t\tsourceTree = "<group>";\n'
            f"\t\t}};"
        )

    src_ids = [file_refs[p] for p, _ in SOURCE_FILES]
    res_ids = [file_refs[p] for p, _ in RESOURCE_FILES]

    lines = ["/* Begin PBXGroup section */"]
    lines.append(
        group(ID["main_group"], "ScrollingDead", [ID["grp_root"], ID["products_group"]])
    )
    lines.append(group(ID["products_group"], "Products", [ID["app_product"]]))
    lines.append(
        group(
            ID["grp_root"],
            "ScrollingDead",
            [ID["grp_sources"], ID["grp_resources"]],
            path="ScrollingDead",
        )
    )
    lines.append(group(ID["grp_sources"], "Sources", src_ids))
    lines.append(group(ID["grp_resources"], "Resources", res_ids, path="Resources"))
    lines.append("/* End PBXGroup section */")
    return "\n".join(lines)


def pbx_native_target_section():
    return (
        "/* Begin PBXNativeTarget section */\n"
        f"\t\t{ID['main_target']} /* ScrollingDead */ = {{\n"
        "\t\t\tisa = PBXNativeTarget;\n"
        f"\t\t\tbuildConfigurationList = {ID['config_list_tgt']};\n"
        "\t\t\tbuildPhases = (\n"
        f"\t\t\t\t{ID['sources_phase']} /* Sources */,\n"
        f"\t\t\t\t{ID['frameworks_phase']} /* Frameworks */,\n"
        f"\t\t\t\t{ID['resources_phase']} /* Resources */,\n"
        "\t\t\t);\n"
        "\t\t\tbuildRules = ();\n"
        "\t\t\tdependencies = ();\n"
        "\t\t\tname = ScrollingDead;\n"
        "\t\t\tpackageProductDependencies = ();\n"
        "\t\t\tproductName = ScrollingDead;\n"
        f"\t\t\tproductReference = {ID['app_product']} /* ScrollingDead.app */;\n"
        '\t\t\tproductType = "com.apple.product-type.application";\n'
        "\t\t};\n"
        "/* End PBXNativeTarget section */"
    )


def pbx_project_section():
    return (
        "/* Begin PBXProject section */\n"
        f"\t\t{ID['project']} /* Project object */ = {{\n"
        "\t\t\tisa = PBXProject;\n"
        "\t\t\tattributes = {\n"
        "\t\t\t\tBuildIndependentTargetsInParallel = 1;\n"
        "\t\t\t\tLastSwiftUpdateCheck = 1500;\n"
        "\t\t\t\tLastUpgradeCheck = 1500;\n"
        "\t\t\t\tTargetAttributes = {\n"
        f"\t\t\t\t\t{ID['main_target']} = {{\n"
        "\t\t\t\t\t\tCreatedOnToolsVersion = 15.0;\n"
        "\t\t\t\t\t};\n"
        "\t\t\t\t};\n"
        "\t\t\t};\n"
        f"\t\t\tbuildConfigurationList = {ID['config_list_proj']};\n"
        '\t\t\tcompatibilityVersion = "Xcode 14.0";\n'
        "\t\t\tdevelopmentRegion = zh_TW;\n"
        "\t\t\thasScannedForEncodings = 0;\n"
        "\t\t\tknownRegions = (en, Base, zh_TW,);\n"
        f"\t\t\tmainGroup = {ID['main_group']};\n"
        f"\t\t\tproductRefGroup = {ID['products_group']} /* Products */;\n"
        '\t\t\tprojectDirPath = "";\n'
        '\t\t\tprojectRoot = "";\n'
        "\t\t\ttargets = (\n"
        f"\t\t\t\t{ID['main_target']} /* ScrollingDead */,\n"
        "\t\t\t);\n"
        "\t\t};\n"
        "/* End PBXProject section */"
    )


def pbx_sources_phase():
    lines = [
        "/* Begin PBXSourcesBuildPhase section */",
        f"\t\t{ID['sources_phase']} /* Sources */ = {{",
        "\t\t\tisa = PBXSourcesBuildPhase;",
        "\t\t\tbuildActionMask = 2147483647;",
        "\t\t\tfiles = (",
    ]
    for path, name in SOURCE_FILES:
        lines.append(f"\t\t\t\t{build_files[path]} /* {name} in Sources */,")
    lines += [
        "\t\t\t);",
        "\t\t\trunOnlyForDeploymentPostprocessing = 0;",
        "\t\t};",
        "/* End PBXSourcesBuildPhase section */",
    ]
    return "\n".join(lines)


def pbx_frameworks_phase():
    return (
        "/* Begin PBXFrameworksBuildPhase section */\n"
        f"\t\t{ID['frameworks_phase']} /* Frameworks */ = {{\n"
        "\t\t\tisa = PBXFrameworksBuildPhase;\n"
        "\t\t\tbuildActionMask = 2147483647;\n"
        "\t\t\tfiles = ();\n"
        "\t\t\trunOnlyForDeploymentPostprocessing = 0;\n"
        "\t\t};\n"
        "/* End PBXFrameworksBuildPhase section */"
    )


def pbx_resources_phase():
    return (
        "/* Begin PBXResourcesBuildPhase section */\n"
        f"\t\t{ID['resources_phase']} /* Resources */ = {{\n"
        "\t\t\tisa = PBXResourcesBuildPhase;\n"
        "\t\t\tbuildActionMask = 2147483647;\n"
        "\t\t\tfiles = ();\n"
        "\t\t\trunOnlyForDeploymentPostprocessing = 0;\n"
        "\t\t};\n"
        "/* End PBXResourcesBuildPhase section */"
    )


def xc_build_configuration_section():
    shared = (
        "\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;\n"
        "\t\t\t\tCLANG_ANALYZER_NONNULL = YES;\n"
        '\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";\n'
        "\t\t\t\tCLANG_ENABLE_MODULES = YES;\n"
        "\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;\n"
        "\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;\n"
        "\t\t\t\tCOPY_PHASE_STRIP = NO;\n"
        "\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;\n"
        "\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu11;\n"
        "\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;\n"
        "\t\t\t\tMTL_FAST_MATH = YES;\n"
        "\t\t\t\tSDKROOT = iphoneos;\n"
    )
    target_shared = (
        "\t\t\t\tCODE_SIGN_ENTITLEMENTS = "
        '"ScrollingDead/Resources/ScrollingDead.entitlements";\n'
        "\t\t\t\tCODE_SIGN_STYLE = Automatic;\n"
        "\t\t\t\tCURRENT_PROJECT_VERSION = 1;\n"
        '\t\t\t\tDEVELOPMENT_TEAM = "";\n'
        "\t\t\t\tENABLE_PREVIEWS = YES;\n"
        "\t\t\t\tGENERATE_INFOPLIST_FILE = NO;\n"
        "\t\t\t\tINFOPLIST_FILE = "
        '"ScrollingDead/Resources/Info.plist";\n'
        "\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;\n"
        "\t\t\t\tMARKETING_VERSION = 1.0;\n"
        '\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = "com.yourname.ScrollingDead";\n'
        '\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";\n'
        "\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;\n"
        "\t\t\t\tSWIFT_VERSION = 5.0;\n"
        '\t\t\t\tTARGETED_DEVICE_FAMILY = "1,2";\n'
    )
    return (
        "/* Begin XCBuildConfiguration section */\n"
        # Base Debug
        f"\t\t{ID['debug_config']} /* Debug */ = {{\n"
        "\t\t\tisa = XCBuildConfiguration;\n"
        "\t\t\tbuildSettings = {\n"
        + shared
        + "\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;\n"
        "\t\t\t\tENABLE_TESTABILITY = YES;\n"
        "\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;\n"
        "\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;\n"
        '\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = ("DEBUG=1", "$(inherited)",);\n'
        "\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;\n"
        "\t\t\t\tONLY_ACTIVE_ARCH = YES;\n"
        "\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;\n"
        '\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = "-Onone";\n'
        "\t\t\t};\n"
        "\t\t\tname = Debug;\n"
        "\t\t};\n"
        # Base Release
        f"\t\t{ID['release_config']} /* Release */ = {{\n"
        "\t\t\tisa = XCBuildConfiguration;\n"
        "\t\t\tbuildSettings = {\n"
        + shared
        + '\t\t\t\tDEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";\n'
        "\t\t\t\tENABLE_NS_ASSERTIONS = NO;\n"
        "\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;\n"
        "\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;\n"
        '\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = "-O";\n'
        "\t\t\t\tVALIDATE_PRODUCT = YES;\n"
        "\t\t\t};\n"
        "\t\t\tname = Release;\n"
        "\t\t};\n"
        # Target Debug
        f"\t\t{ID['project_debug']} /* Debug */ = {{\n"
        "\t\t\tisa = XCBuildConfiguration;\n"
        "\t\t\tbuildSettings = {\n" + target_shared + "\t\t\t};\n"
        "\t\t\tname = Debug;\n"
        "\t\t};\n"
        # Target Release
        f"\t\t{ID['project_release']} /* Release */ = {{\n"
        "\t\t\tisa = XCBuildConfiguration;\n"
        "\t\t\tbuildSettings = {\n" + target_shared + "\t\t\t};\n"
        "\t\t\tname = Release;\n"
        "\t\t};\n"
        "/* End XCBuildConfiguration section */"
    )


def xc_config_list_section():
    return (
        "/* Begin XCConfigurationList section */\n"
        f"\t\t{ID['config_list_proj']} = {{\n"
        "\t\t\tisa = XCConfigurationList;\n"
        "\t\t\tbuildConfigurations = (\n"
        f"\t\t\t\t{ID['debug_config']} /* Debug */,\n"
        f"\t\t\t\t{ID['release_config']} /* Release */,\n"
        "\t\t\t);\n"
        "\t\t\tdefaultConfigurationIsVisible = 0;\n"
        "\t\t\tdefaultConfigurationName = Release;\n"
        "\t\t};\n"
        f"\t\t{ID['config_list_tgt']} = {{\n"
        "\t\t\tisa = XCConfigurationList;\n"
        "\t\t\tbuildConfigurations = (\n"
        f"\t\t\t\t{ID['project_debug']} /* Debug */,\n"
        f"\t\t\t\t{ID['project_release']} /* Release */,\n"
        "\t\t\t);\n"
        "\t\t\tdefaultConfigurationIsVisible = 0;\n"
        "\t\t\tdefaultConfigurationName = Release;\n"
        "\t\t};\n"
        "/* End XCConfigurationList section */"
    )


# ─── Main ──────────────────────────────────────────────────────────────────────


def generate():
    content = (
        "// !$*UTF8*$!\n{\n"
        "\tarchiveVersion = 1;\n"
        "\tclasses = {};\n"
        "\tobjectVersion = 56;\n"
        "\tobjects = {\n\n"
        + pbx_build_file_section()
        + "\n\n"
        + pbx_file_reference_section()
        + "\n\n"
        + pbx_frameworks_phase()
        + "\n\n"
        + pbx_group_section()
        + "\n\n"
        + pbx_native_target_section()
        + "\n\n"
        + pbx_project_section()
        + "\n\n"
        + pbx_resources_phase()
        + "\n\n"
        + pbx_sources_phase()
        + "\n\n"
        + xc_build_configuration_section()
        + "\n\n"
        + xc_config_list_section()
        + "\n\n"
        + "\t};\n"
        f"\trootObject = {ID['project']} /* Project object */;\n"
        "}\n"
    )

    out = os.path.join(
        os.path.dirname(__file__),
        "ScrollingDead.xcodeproj",
        "project.pbxproj",
    )
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w") as f:
        f.write(content)
    print(f"✅  Written: {out}")
    print(f"   {len(SOURCE_FILES)} source files + {len(RESOURCE_FILES)} resource files")


if __name__ == "__main__":
    generate()
