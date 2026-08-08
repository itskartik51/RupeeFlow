# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# ==========================================
# 🛡️ RUPEEFLOW: PRECAUTIONARY RULES FOR R8
# ==========================================

# 1. Firebase Firestore & JSON Serialization Protection
# Firestore aur JSON parsing ko variables ke exact naam chahiye hote hain.
-keepattributes Signature
-keepclassmembers class com.kartikey.rupeeflow.** {
    <fields>;
    <init>();
}

# 2. Keep all core Data Models & Items intact
# Agar R8 in classes ka naam badal dega toh UI aur Cache fail ho jayega.
-keep class com.kartikey.rupeeflow.UI_Screens.AppData { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Home.ContriRoomModel { *; }

# 3. Prevent Compose UI Models from being stripped
-keep class com.kartikey.rupeeflow.UI_Screens.Home.ContriExpense { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Home.MemberLedger { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Home.Settlement { *; }
-keep class com.kartikey.rupeeflow.UI_Screens.Home.PastCycle { *; }

# 4. Keep OkHttp (To prevent network crashes during background live market fetch)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
