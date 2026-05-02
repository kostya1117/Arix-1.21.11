package net.optifine.reflect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.optifine.Log;

public class ReflectorMethod implements IResolvable {
    private ReflectorClass reflectorClass;
    private String targetMethodName;
    private Class[] targetMethodParameterTypes;
    private boolean checked;
    private Method targetMethod;

    public ReflectorMethod(ReflectorClass reflectorClass, String targetMethodName) {
        this(reflectorClass, targetMethodName, null);
    }

    public ReflectorMethod(ReflectorClass reflectorClass, String targetMethodName, Class[] targetMethodParameterTypes) {
        this.reflectorClass = null;
        this.targetMethodName = null;
        this.targetMethodParameterTypes = null;
        this.checked = false;
        this.targetMethod = null;
        this.reflectorClass = reflectorClass;
        this.targetMethodName = targetMethodName;
        this.targetMethodParameterTypes = targetMethodParameterTypes;
        ReflectorResolver.register(this);
    }

    public Method getTargetMethod() {
        if (this.checked) {
            return this.targetMethod;
        } else {
            this.checked = true;
            Class cls = this.reflectorClass.getTargetClass();
            if (cls == null) {
                return null;
            } else {
                try {
                    String var10000;
                    if (this.targetMethodParameterTypes == null) {
                        Method[] ms = getMethods(cls, this.targetMethodName);
                        if (ms.length <= 0) {
                            var10000 = cls.getName();
                            Log.log("(Reflector) Method not present: " + var10000 + "." + this.targetMethodName);
                            return null;
                        }

                        if (ms.length > 1) {
                            var10000 = cls.getName();
                            Log.warn("(Reflector) More than one method found: " + var10000 + "." + this.targetMethodName);

                            for(int i = 0; i < ms.length; ++i) {
                                Method m = ms[i];
                                Log.warn("(Reflector)  - " + String.valueOf(m));
                            }

                            return null;
                        }

                        this.targetMethod = ms[0];
                    } else {
                        this.targetMethod = getMethod(cls, this.targetMethodName, this.targetMethodParameterTypes);
                    }

                    if (this.targetMethod == null) {
                        var10000 = cls.getName();
                        Log.log("(Reflector) Method not present: " + var10000 + "." + this.targetMethodName);
                        return null;
                    } else {
                        this.targetMethod.setAccessible(true);
                        return this.targetMethod;
                    }
                } catch (Throwable var5) {
                    var5.printStackTrace();
                    return null;
                }
            }
        }
    }

    public boolean exists() {
        if (this.checked) {
            return this.targetMethod != null;
        } else {
            return this.getTargetMethod() != null;
        }
    }

    public Class getReturnType() {
        Method tm = this.getTargetMethod();
        return tm == null ? null : tm.getReturnType();
    }

    public void deactivate() {
        this.checked = true;
        this.targetMethod = null;
    }

    public Object call(Object... params) {
        return Reflector.call(this, params);
    }

    public boolean callBoolean(Object... params) {
        return Reflector.callBoolean(this, params);
    }

    public int callInt(Object... params) {
        return Reflector.callInt(this, params);
    }

    public long callLong(Object... params) {
        return Reflector.callLong(this, params);
    }

    public float callFloat(Object... params) {
        return Reflector.callFloat(this, params);
    }

    public double callDouble(Object... params) {
        return Reflector.callDouble(this, params);
    }

    public String callString(Object... params) {
        return Reflector.callString(this, params);
    }

    public Object call(Object param) {
        return Reflector.call(this, param);
    }

    public boolean callBoolean(Object param) {
        return Reflector.callBoolean(this, param);
    }

    public int callInt(Object param) {
        return Reflector.callInt(this, param);
    }

    public long callLong(Object param) {
        return Reflector.callLong(this, param);
    }

    public float callFloat(Object param) {
        return Reflector.callFloat(this, param);
    }

    public double callDouble(Object param) {
        return Reflector.callDouble(this, param);
    }

    public String callString1(Object param) {
        return Reflector.callString(this, param);
    }

    public void callVoid(Object... params) {
        Reflector.callVoid(this, params);
    }

    public static Method getMethod(Class cls, String methodName, Class[] paramTypes) {
        Method[] ms = cls.getDeclaredMethods();

        for(int i = 0; i < ms.length; ++i) {
            Method m = ms[i];
            if (m.getName().equals(methodName)) {
                Class[] types = m.getParameterTypes();
                if (Reflector.matchesTypes(paramTypes, types)) {
                    return m;
                }
            }
        }

        return null;
    }

    public static Method[] getMethods(Class cls, String methodName) {
        List listMethods = new ArrayList();
        Method[] ms = cls.getDeclaredMethods();

        for(int i = 0; i < ms.length; ++i) {
            Method m = ms[i];
            if (m.getName().equals(methodName)) {
                listMethods.add(m);
            }
        }

        Method[] methods = (Method[])listMethods.toArray(new Method[listMethods.size()]);
        return methods;
    }

    public void resolve() {
        Method m = this.getTargetMethod();
    }
}
