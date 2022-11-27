package com.tencent.tinker.loader;

import android.annotation.SuppressLint;
import android.util.Log;

import com.qihoo360.loader2.PMF;
import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.helper.LogDebug;
import com.tencent.tinker.anno.Keep;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import dalvik.system.PathClassLoader;

/**
 * Created by tangyinsheng on 2020-01-09.
 */
@Keep
@SuppressLint("NewApi")
public final class TinkerClassLoader extends PathClassLoader {
    private static final String TAG = "Tinker.ClassLoader";
    private final ClassLoader mOriginAppClassLoader;

    TinkerClassLoader(String dexPath, File optimizedDir, String libraryPath, ClassLoader originAppClassLoader) {
        super("", libraryPath, ClassLoader.getSystemClassLoader());
        mOriginAppClassLoader = originAppClassLoader;
        injectDexPath(this, dexPath, optimizedDir);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        c = PMF.loadClass(className, resolve);
        if (c != null) {
            return c;
        } else {
            try {
                c = this.mOriginAppClassLoader.loadClass(className);
                if (LogDebug.LOG && RePlugin.getConfig().isPrintDetailLog()) {
                    LogDebug.d("RePluginClassLoader", "loadClass: load other class, cn=" + className);
                }

                return c;
            } catch (Throwable var5) {
                return super.loadClass(className, resolve);
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Log.d(TAG, "findClass: ");
        Class<?> cl = null;
        try {
            cl = super.findClass(name);
        } catch (ClassNotFoundException ignored) {
            cl = null;
        }
        if (cl != null) {
            return cl;
        } else {
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
        @SuppressWarnings("unchecked")
        final Enumeration<URL>[] resources = (Enumeration<URL>[]) new Enumeration<?>[] {
                Object.class.getClassLoader().getResources(name),
                findResources(name),
                mOriginAppClassLoader.getResources(name)
        };
        return new CompoundEnumeration<>(resources);
    }

    private static void injectDexPath(ClassLoader cl, String dexPath, File optimizedDir) {
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
        private int index = 0;

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
