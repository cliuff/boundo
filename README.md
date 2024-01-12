<p align="center">
  <img src="doconfig/markdown/shot1.png" width="270">
  <img src="doconfig/markdown/shot2.png" width="270">
  <img src="doconfig/markdown/shot3.png" width="270">
</p>

# Boundo: App API Checker

**Requirements**
- Android Studio `Hedgehog Patch 1` (AGP `8.2.1`)
- Gradle Wrapper `8.4`
- Java `17`
- Kotlin `1.9.22`
- Jetpack Compose Compiler `1.5.8` (requires Kotlin `1.9.22`)

**After Git check-out**
- **Copy** the file named `custom.properties.template` in `doconfig` directory,
  **rename** it to `custom.properties` and make necessary **change**s to it

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
