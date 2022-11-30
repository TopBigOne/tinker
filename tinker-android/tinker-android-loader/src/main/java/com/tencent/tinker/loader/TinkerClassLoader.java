package com.tencent.tinker.loader;

import android.annotation.SuppressLint;
import android.util.Log;
//
//import com.qihoo360.loader2.PMF;
//import com.qihoo360.replugin.RePlugin;
//import com.qihoo360.replugin.helper.LogDebug;

import com.qihoo360.loader2.PMF;
import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.RePluginClassLoader;
import com.qihoo360.replugin.helper.LogDebug;
import com.tencent.tinker.anno.Keep;
import com.tencent.tinker.loader.shareutil.ShareTinkerLog;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongToDoubleFunction;

import dalvik.system.PathClassLoader;

/**
 * Created by tangyinsheng on 2020-01-09.
 */
@Keep
@SuppressLint("NewApi")
public final class TinkerClassLoader extends RePluginClassLoader {
    private static final String      TAG = "Tinker.ClassLoader";
    private final        ClassLoader mOriginAppClassLoader;
/*
    TinkerClassLoader(String dexPath, File optimizedDir, String libraryPath, ClassLoader originAppClassLoader) {
        super("", libraryPath, ClassLoader.getSystemClassLoader());
        Log.d(TAG, " start create TinkerClassLoader,and the parent class is RePluginClassLoader");
        mOriginAppClassLoader = originAppClassLoader;
        injectDexPath(this, dexPath, optimizedDir);
    }

    */

    TinkerClassLoader(String dexPath, File optimizedDir, String libraryPath, ClassLoader originAppClassLoader) {
        super(dexPath, libraryPath, ClassLoader.getSystemClassLoader(), originAppClassLoader);
        ShareTinkerLog.iBlack(TAG);
        ShareTinkerLog.iBlack("|-----------------------------------------------------------------------------------↓");
        ShareTinkerLog.iBlack("|                                                                                   |");
        ShareTinkerLog.d(TAG, "|   start create TinkerClassLoader,and the parent class is RePluginClassLoader      |");
        ShareTinkerLog.iBlack("|                                                                                   |");
        ShareTinkerLog.iBlack("|-----------------------------------------------------------------------------------↑");
        ShareTinkerLog.iBlack(TAG);
        mOriginAppClassLoader = originAppClassLoader;
        injectDexPath(this, dexPath, optimizedDir);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ShareTinkerLog.i(TAG, "TinkerClassLoader start  findClass  ,the class name is : " + name);
        ShareTinkerLog.iBlack(TAG);
        Class<?> cl = null;
        try {
            cl = super.findClass(name);

        } catch (ClassNotFoundException ignored) {
            ShareTinkerLog.w(TAG, "TinkerClassLoader start  findClass  ,in null by invoke super ; case 1");
            ShareTinkerLog.wBlack(TAG);
            cl = null;
        }
        if (cl != null) {
            return cl;
        } else {
            ShareTinkerLog.w(TAG, "TinkerClassLoader finally ,start load class by mOriginAppClassLoader; case 2");
            ShareTinkerLog.wBlack(TAG);
            return mOriginAppClassLoader.loadClass(name);
        }
    }

    @Override
    public URL getResource(String name) {
        // The lookup order we use here is the same as for classes.
        URL resource = Object.class.getClassLoader().getResource(name);
        if (resource != null) {
            return resource;
        }

        resource = findResource(name);
        if (resource != null) {
            return resource;
        }

        return mOriginAppClassLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        @SuppressWarnings("unchecked") final Enumeration<URL>[] resources = (Enumeration<URL>[]) new Enumeration<?>[]{Object.class.getClassLoader().getResources(name), findResources(name), mOriginAppClassLoader.getResources(name)};
        return new CompoundEnumeration<>(resources);
    }

    private static void injectDexPath(ClassLoader cl, String dexPath, File optimizedDir) {
        ShareTinkerLog.w(TAG, " start invoke injectDexPath.");
        try {
            final List<File> dexFiles = new ArrayList<>(16);
            for (String oneDexPath : dexPath.split(":")) {
                if (oneDexPath.isEmpty()) {
                    continue;
                }
                dexFiles.add(new File(oneDexPath));
            }
            if (!dexFiles.isEmpty()) {
                SystemClassLoaderAdder.injectDexesInternal(cl, dexFiles, optimizedDir);
            }
        } catch (Throwable thr) {
            throw new TinkerRuntimeException("Fail to create TinkerClassLoader.", thr);
        }
    }

    @Keep
    class CompoundEnumeration<E> implements Enumeration<E> {
        private Enumeration<E>[] enums;
        private int              index = 0;

        public CompoundEnumeration(Enumeration<E>[] enums) {
            this.enums = enums;
        }

        @Override
        public boolean hasMoreElements() {
            while (index < enums.length) {
                if (enums[index] != null && enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        @Override
        public E nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }
    }
}
