# Boundo

**Requirements**
- Android Studio 4.0
- Gradle 6.6.1

**After Git check-out**
- Add a file named `custom.properties` in project root directory, within add the following lines:
> packageName=com.madness.collision  
> signingKeyStorePath=**KeyStorePath**  
> signingKeyStorePassword=**KeyStorePassword**  
> signingKeyAlias=**KeyAlias**  
> signingKeyPassword=**KeyPassword**  

**After Gradle sync**
- Adjust **Run/Debug Configurations** settings
    - Open **Edit Run/Debug configurations** dialog
    - Click **Edit Configurations...**
    - Select configuration **app**
    - Locate **deploy** under **Installation Options**
    - Change the configuration to **APK from app bundle**
- Adjust **Build Variants** settings
    - Open **Build Variants** tool window
    - Change the **Active Build Variant** of module **app** to **fullDebug**
    - Change the **Active Build Variant** of module **wearable** to **fullDebug**
