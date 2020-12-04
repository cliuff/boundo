# Boundo

**Requirements**
- Android Studio 4.1
- Gradle 6.7

**After Git check-out**
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
