
<div align="right">
  <details>
    <summary >🌐 Language</summary>
    <div>
      <div align="center">
        <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=en">English</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=zh-CN">简体中文</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=zh-TW">繁體中文</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=ja">日本語</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=ko">한국어</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=hi">हिन्दी</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=th">ไทย</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=fr">Français</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=de">Deutsch</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=es">Español</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=it">Italiano</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=ru">Русский</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=pt">Português</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=nl">Nederlands</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=pl">Polski</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=ar">العربية</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=fa">فارسی</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=tr">Türkçe</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=vi">Tiếng Việt</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=id">Bahasa Indonesia</a>
        | <a href="https://openaitx.github.io/view.html?user=cliuff&project=boundo&lang=as">অসমীয়া</
      </div>
    </div>
  </details>
</div>

<p align="center">
  <img src="doconfig/markdown/shot1.png" width="270" alt="Screenshot 1">
  <img src="doconfig/markdown/shot2.png" width="270" alt="Screenshot 2">
  <img src="doconfig/markdown/shot3.png" width="270" alt="Screenshot 3">
</p>

# Boundo Meta-app: Info & Manager

Boundo Meta-app is all about your apps and beyond.  
Check info of apps on your phone, and organize them into categories.  
Designed for Android enthusiasts, Meta-app is available on **Android** and **Wear OS**.

### 1. Develop
**1.1 Requirements**
- Android Studio `Meerkat` (AGP `8.9`)
- Java `17`, Kotlin `2.1.21`, Gradle `8.13`

**1.2 Configure automatic signing**
- **Copy** the file named `custom.properties.template` in `doconfig` directory,
  **rename** it to `custom.properties` and make necessary **change**s to it

**1.3 Build universal APKs (App API feature included, release variant)**
- Configure signing as specified in section 1.2
- Download BundleTool binary from [BundleTool Releases](https://github.com/google/bundletool/releases),
  and save it as `doconfig/bundletool.jar` in this project
- Run Gradle task from commandline `gradlew :app:genUniversalApks`
- Look for `app/build/outputs/app-universal-release.apks` in this project

**1.4 Build FOSS version (less feature)**
- Configure signing as specified in section 1.2
- Download BundleTool binary from [BundleTool Releases](https://github.com/google/bundletool/releases),
  and save it as `doconfig/bundletool.jar` in this project
- Run Gradle task from commandline `gradlew :app:genFossApks`
- Look for `app/build/outputs/app-universal-foss.apks` in this project

### 2. Download
[<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='100'/>](
https://play.google.com/store/apps/details?id=com.madness.collision&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
