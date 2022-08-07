# Boundo

**Requirements**
- Android Studio Chipmunk Patch 2
- Gradle 7.3.3
- Java 11

**After Git check-out**
- Clone project `Apk Parser` from (https://github.com/cliuff/apk-parser.git) into `subproject`, 
  resulting in `ROOT_DIR/subproject/apk-parser`
- Copy the file named `custom.properties.template` in project root directory and rename it to `custom.properties`
- Make necessary changes to `custom.properties`

**After Gradle sync**
- Adjust **Run/Debug Configurations** settings
    - Open **Edit Run/Debug configurations** dialog
    - Click **Edit Configurations...**
    - Select configuration **boundo.app**
    - Locate **deploy** under **Installation Options**
    - Change the configuration to **APK from app bundle**
- Adjust **Build Variants** settings
    - Open **Build Variants** tool window
    - Change the **Active Build Variant** of module **boundo.app** to **fullDebug**
    - Change the **Active Build Variant** of module **boundo.wearable** to **fullDebug**

**Attention**
- As a standalone project, `apk-parser` may need to be built independently after code changes in that module,  
  by clicking window menu item "Build" -> "Make module boundo.apk-parser",
  otherwise changes may not take effect immediately.
