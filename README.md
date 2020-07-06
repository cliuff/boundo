# Boundo

**Requirement**
- Android Studio 4.0
- Gradle 6.5.1

**After check-out**
- Add a file named `custom.properties` in project root directory, within add the following lines:
> packageName=com.madness.collision
> signingKeyStorePath=**KeyStorePath**
> signingKeyStorePassword=**KeyStorePassword**
> signingKeyAlias=**KeyAlias**
> signingKeyPassword=**KeyPassword**

**After sync**
- **Edit Configurations**: configuration **app**, change **deploy** to **APK from app bundle**
- Adjust **Build Variants**, change the **Active Build Variant** of module **app** and **wearable** to **fullDebug**
