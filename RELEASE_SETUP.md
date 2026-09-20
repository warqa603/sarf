# إعداد التطبيق للنشر على Google Play Store

## 1. إنشاء مفتاح التوقيع (Keystore)

قم بإنشاء مفتاح التوقيع باستخدام الأمر التالي:

```bash
keytool -genkey -v -keystore sarf-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sarf
```

**ملاحظات مهمة:**
- احفظ كلمة المرور في مكان آمن
- لا تشارك ملف `.jks` مع أي شخص
- أضف `sarf-release-key.jks` إلى `.gitignore`

## 2. إنشاء ملف keystore.properties

أنشئ ملف `keystore.properties` في المجلد الرئيسي للمشروع (نفس مستوى `build.gradle`):

```properties
storePassword=your_store_password_here
keyPassword=your_key_password_here
keyAlias=sarf
storeFile=../sarf-release-key.jks
```

**ملاحظة:** أضف `keystore.properties` إلى `.gitignore` أيضاً.

## 3. تفعيل التوقيع في build.gradle.kts

افتح `app/build.gradle.kts` وافك التعليق عن قسم `signingConfigs`:

```kotlin
signingConfigs {
    getByName("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = java.util.Properties()
            keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}
```

ثم في قسم `buildTypes` -> `release`، افك التعليق عن:
```kotlin
signingConfig = signingConfigs.getByName("release")
```

## 4. بناء APK/AAB للتوزيع

### بناء AAB (موصى به لـ Play Store):
```bash
./gradlew bundleRelease
```
الملف سيكون في: `app/build/outputs/bundle/release/app-release.aab`

### بناء APK:
```bash
./gradlew assembleRelease
```
الملف سيكون في: `app/build/outputs/apk/release/app-release.apk`

## 5. متطلبات Google Play Store

### معلومات التطبيق المطلوبة:
- ✅ **App ID**: `com.tajir.sarf`
- ✅ **AdMob App ID**: `ca-app-pub-4182222159500814~9693163315`
- ✅ **Banner Ad Unit (Warqa_banner)**: `ca-app-pub-4182222159500814/3206901156`
- ✅ **Interstitial Ad Unit (Warqa_interstitial)**: `ca-app-pub-4182222159500814/7394303032`
- ✅ **Rewarded Ad Unit (Warqa_reward)**: `ca-app-pub-4182222159500814/9501591629`

### الأذونات المستخدمة:
- `INTERNET` - مطلوب للإعلانات والاتصال بالإنترنت
- `ACCESS_NETWORK_STATE` - مطلوب للتحقق من حالة الشبكة
- `ACCESS_WIFI_STATE` - اختياري لتحسين استهداف الإعلانات

### سياسة الخصوصية:
يجب أن يكون لديك رابط لسياسة الخصوصية يتضمن:
- معلومات عن استخدام AdMob
- البيانات التي يتم جمعها
- كيفية استخدام البيانات

### Data Safety Form في Play Console:
عند ملء نموذج Data Safety، يجب الإعلان عن:
- **Data Collection**: نعم (لأن AdMob يجمع بيانات)
- **Data Types**: 
  - Device ID
  - Advertising ID
  - App interactions
- **Data Usage**: الإعلانات والتسويق
- **Data Sharing**: نعم (مع Google AdMob)

## 6. اختبار التطبيق قبل النشر

1. اختبر على أجهزة مختلفة
2. تأكد من عمل الإعلانات بشكل صحيح
3. اختبر جميع الميزات
4. تأكد من عدم وجود أخطاء في Logcat

## 7. رفع التطبيق إلى Play Store

1. سجل الدخول إلى [Google Play Console](https://play.google.com/console)
2. أنشئ تطبيق جديد
3. ارفع ملف AAB
4. املأ جميع المعلومات المطلوبة
5. أضف لقطات الشاشة والأيقونات
6. أضف رابط سياسة الخصوصية
7. املأ نموذج Data Safety
8. أرسل للتقييم

## ملاحظات مهمة:

- ⚠️ **لا ترفع تطبيق debug إلى Play Store**
- ⚠️ **احتفظ بنسخة احتياطية من ملف keystore**
- ⚠️ **تأكد من تحديث versionCode و versionName عند كل إصدار جديد**
- ✅ **استخدم AAB بدلاً من APK (أصغر حجماً وأفضل أداء)**

