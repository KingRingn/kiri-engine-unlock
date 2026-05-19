package com.kiriengine.unlock;

import android.content.res.XModuleResources;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed module entry point.
 * Implements IXposedHookLoadPackage for method hooking.
 */
public class ModuleMain implements IXposedHookLoadPackage {

    private final MainHook mainHook = new MainHook();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        mainHook.handleLoadPackage(lpparam);
    }
}
