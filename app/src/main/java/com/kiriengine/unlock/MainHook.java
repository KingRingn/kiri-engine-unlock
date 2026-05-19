package com.kiriengine.unlock;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "KiriEngineUnlock";
    private static final String PACKAGE_NAME = "com.kiriengine.app";

    // Class names (stable across versions due to @Keep annotations)
    private static final String USER_INFO_CLASS =
        "com.kiri.libcore.network.bean.UserInfo";
    private static final String USER_INFO_HELPER_COMPANION_CLASS =
        "com.kiri.libcore.helper.UserInfoHelper$Companion";
    private static final String GOOGLE_PAY_STORE_HELPER_CLASS =
        "com.kiri.libcore.helper.pay.GooglePayStoreHelper";
    private static final String KIRI_API_CLASS =
        "com.kiri.libcore.network.KiriEngineAppNewApi";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PACKAGE_NAME.equals(lpparam.packageName)) {
            return;
        }

        Log.i(TAG, "========================================");
        Log.i(TAG, " Kiri Engine Unlock - Applying hooks...");
        Log.i(TAG, "========================================");

        int successCount = 0;
        int totalHooks = 8;

        successCount += hookUserInfoIsVip(lpparam) ? 1 : 0;
        successCount += hookUserInfoGetVipType(lpparam) ? 1 : 0;
        successCount += hookUserInfoGetVipExpiryTime(lpparam) ? 1 : 0;
        successCount += hookUserInfoGetAccountType(lpparam) ? 1 : 0;
        successCount += hookUserInfoHelperGetUserInfo(lpparam) ? 1 : 0;
        successCount += hookUserInfoHelperIsGuestUser(lpparam) ? 1 : 0;
        successCount += hookBillingQueryPurchases(lpparam) ? 1 : 0;
        successCount += hookServerVerification(lpparam) ? 1 : 0;

        Log.i(TAG, "========================================");
        Log.i(TAG, " Hooks applied: " + successCount + "/" + totalHooks);
        Log.i(TAG, "========================================");
    }

    /**
     * PRIMARY HOOK: UserInfo.isVip() -> always return true.
     *
     * The UserInfo class is annotated with @Keep so the method name
     * is stable across ProGuard/R8 runs.
     *
     * JSON mapping: @SerializedName("isVip")
     * This boolean field is the central VIP status indicator.
     * Nearly all feature gates check this via UserInfoHelper.getUserInfo().isVip().
     */
    private boolean hookUserInfoIsVip(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = XposedHelpers.findClass(USER_INFO_CLASS, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "isVip", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });
            Log.i(TAG, "[+] UserInfo.isVip() -> true");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfo.isVip() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * UserInfo.getVipType() -> always return 999 (highest tier).
     *
     * vipType: 0 = free, higher = better tier.
     * Some UI elements (badges, icons) depend on this value.
     */
    private boolean hookUserInfoGetVipType(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = XposedHelpers.findClass(USER_INFO_CLASS, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "getVipType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(999);
                }
            });
            Log.i(TAG, "[+] UserInfo.getVipType() -> 999");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfo.getVipType() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * UserInfo.getVipExpiryTime() -> always return Long.MAX_VALUE.
     *
     * JSON mapping: @SerializedName("endTime")
     * A far-future timestamp means the subscription never expires.
     */
    private boolean hookUserInfoGetVipExpiryTime(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = XposedHelpers.findClass(USER_INFO_CLASS, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "getVipExpiryTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(Long.MAX_VALUE);
                }
            });
            Log.i(TAG, "[+] UserInfo.getVipExpiryTime() -> Long.MAX_VALUE");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfo.getVipExpiryTime() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * UserInfo.getAccountType() -> always return 1 (premium).
     *
     * JSON mapping: @SerializedName("type")
     * -1 = guest, 0 = free, 1+ = premium tiers.
     */
    private boolean hookUserInfoGetAccountType(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = XposedHelpers.findClass(USER_INFO_CLASS, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "getAccountType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(1);
                }
            });
            Log.i(TAG, "[+] UserInfo.getAccountType() -> 1");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfo.getAccountType() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * UserInfoHelper.Companion.getUserInfo() -> inject VIP fields.
     *
     * After the original method returns (deserializing from SharedPreferences),
     * we modify the returned UserInfo object to always have VIP status.
     * This catches any code that reads cached user info from SP.
     */
    private boolean hookUserInfoHelperGetUserInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> companionClass = XposedHelpers.findClass(
                USER_INFO_HELPER_COMPANION_CLASS, lpparam.classLoader);
            Class<?> userInfoClass = XposedHelpers.findClass(
                USER_INFO_CLASS, lpparam.classLoader);

            // Get field references
            final Field isVipField = userInfoClass.getDeclaredField("isVip");
            isVipField.setAccessible(true);
            final Field vipTypeField = userInfoClass.getDeclaredField("vipType");
            vipTypeField.setAccessible(true);
            final Field vipExpiryField = userInfoClass.getDeclaredField("vipExpiryTime");
            vipExpiryField.setAccessible(true);
            final Field accountTypeField = userInfoClass.getDeclaredField("accountType");
            accountTypeField.setAccessible(true);

            XposedHelpers.findAndHookMethod(companionClass, "getUserInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Object userInfo = param.getResult();
                            if (userInfo != null) {
                                isVipField.setBoolean(userInfo, true);
                                vipTypeField.setInt(userInfo, 999);
                                vipExpiryField.setLong(userInfo, Long.MAX_VALUE);
                                accountTypeField.setInt(userInfo, 1);
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error modifying UserInfo: " + t.getMessage());
                        }
                    }
                }
            );
            Log.i(TAG, "[+] UserInfoHelper.getUserInfo() -> VIP fields injected");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfoHelper.getUserInfo() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * UserInfoHelper.Companion.isGuestUser() -> always return false.
     *
     * Guest users have reduced functionality and can't access VIP features
     * even if isVip is true. This ensures the user is treated as logged-in.
     */
    private boolean hookUserInfoHelperIsGuestUser(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> companionClass = XposedHelpers.findClass(
                USER_INFO_HELPER_COMPANION_CLASS, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(companionClass, "isGuestUser",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
            );
            Log.i(TAG, "[+] UserInfoHelper.isGuestUser() -> false");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] UserInfoHelper.isGuestUser() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * GooglePayStoreHelper.queryPurchasesAsync() -> suppress billing query.
     *
     * This method queries Google Play for existing purchases.
     * We hook it to prevent it from reporting "no purchases found",
     * which could trigger fallback behavior or clear VIP status.
     *
     * Using hookAllMethods since parameter types are obfuscated Kotlin
     * functional interfaces that differ between builds.
     */
    private boolean hookBillingQueryPurchases(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> helperClass = XposedHelpers.findClass(
                GOOGLE_PAY_STORE_HELPER_CLASS, lpparam.classLoader);

            XposedBridge.hookAllMethods(helperClass, "queryPurchasesAsync",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Log.i(TAG, "[*] queryPurchasesAsync called - suppressing");
                        // Prevent the method from executing the billing query.
                        // The VIP status from our other hooks will take precedence.
                        param.setResult(null);
                    }
                }
            );
            Log.i(TAG, "[+] GooglePayStoreHelper.queryPurchasesAsync() -> suppressed");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "[-] GooglePayStoreHelper.queryPurchasesAsync() failed: "
                + t.getMessage());
            return false;
        }
    }

    /**
     * Server-side verification hooks.
     *
     * These Retrofit API methods are called after a purchase to verify
     * with the server. We hook them to prevent server-side purchase
     * verification from clearing the client-side VIP status.
     *
     * API endpoints:
     *   POST v1/app/pay/verifyInAppOrderStatus -> verifyOrderStatus / fetchVerifyInAppBuyOrderStatus
     *   POST v1/app/pay/verifySubOrderStatus -> verifySubOrderStatus
     */
    private boolean hookServerVerification(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> apiClass = XposedHelpers.findClass(
                KIRI_API_CLASS, lpparam.classLoader);

            // Hook all verify methods on the API interface
            int hooked = 0;
            for (Method method : apiClass.getDeclaredMethods()) {
                String name = method.getName();
                if (name.contains("verify") || name.contains("Verify")
                    || name.contains("checkPurchased") || name.contains("checkOld")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Log.i(TAG, "[*] Server verify: " + name + " - suppressed");
                        }
                    });
                    hooked++;
                }
            }

            Log.i(TAG, "[+] Server verification hooks: " + hooked + " methods");
            return hooked > 0;
        } catch (Throwable t) {
            Log.e(TAG, "[-] Server verification hooks failed: " + t.getMessage());
            return false;
        }
    }
}
