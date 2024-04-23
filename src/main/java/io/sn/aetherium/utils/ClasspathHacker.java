package io.sn.aetherium.utils;

import io.ktor.server.application.Application;
import io.sn.aetherium.objects.AetheriumShardKt;
import sun.misc.Unsafe;

import javax.security.auth.callback.Callback;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClasspathHacker {

    static MethodHandles.Lookup lookup;
    static Unsafe unsafe;
    static List<Callback> callbacks = new ArrayList<>();

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object lookupBase = unsafe.staticFieldBase(lookupField);
            long lookupOffset = unsafe.staticFieldOffset(lookupField);
            lookup = (MethodHandles.Lookup) unsafe.getObject(lookupBase, lookupOffset);
        } catch (Throwable ignore) {
        }
    }

    public static ClassLoader add(Path path) throws Throwable {
        File file = new File(path.toUri().getPath());
        ClassLoader loader = AetheriumShardKt.class.getClassLoader();
        System.out.println(loader.getClass().getName());
        MethodHandle handle = lookup.findVirtual(URLClassLoader.class, "addURL", MethodType.methodType(void.class, java.net.URL.class));
        handle.invoke(loader, file.toURI().toURL());
        return loader;
    }

}
