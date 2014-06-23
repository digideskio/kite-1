/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.compat;

import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class DynConstructors {
  public static class Ctor<C> {
    private final Constructor<C> ctor;
    private final Class<? extends C> constructed;

    private Ctor(Constructor<C> constructor, Class<? extends C> constructed) {
      this.ctor = constructor;
      this.constructed = constructed;
    }

    public Class<? extends C> getConstructedClass() {
      return constructed;
    }

    public C newInstanceChecked(Object... args) throws Exception {
      try {
        return ctor.newInstance(args);
      } catch (InstantiationException e) {
        throw e;
      } catch (IllegalAccessException e) {
        throw e;
      } catch (InvocationTargetException e) {
        // rethrow the cause is an exception
        Throwables.propagateIfPossible(e.getCause(), Exception.class);
        // otherwise, propagate the throwable
        throw Throwables.propagate(e.getCause());
      }
    }

    public C newInstance(Object... args) {
      try {
        return newInstanceChecked(args);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  public static class Builder {
    private final Class<?> baseClass;
    private Ctor ctor = null;

    public Builder(Class<?> baseClass) {
      this.baseClass = baseClass;
    }

    public Builder() {
      this.baseClass = null;
    }

    public Builder impl(Class<?>... types) {
      impl(baseClass, types);
      return this;
    }

    public Builder impl(String className, Class<?>... types) {
      // don't do any work if an implementation has been found
      if (ctor != null) {
        return this;
      }

      try {
        Class<?> targetClass = Class.forName(className);
        impl(targetClass, types);
      } catch (NoClassDefFoundError e) {
        // cannot load this implementation
      } catch (ClassNotFoundException e) {
        // not the right implementation
      }
      return this;
    }

    public <T> Builder impl(Class<T> targetClass, Class<?>... types) {
      // don't do any work if an implementation has been found
      if (ctor != null) {
        return this;
      }

      try {
        ctor = new Ctor<T>(targetClass.getConstructor(types), targetClass);
      } catch (NoSuchMethodException e) {
        // not the right implementation
      }
      return this;
    }

    public Builder hiddenImpl(Class<?>... types) {
      hiddenImpl(baseClass, types);
      return this;
    }

    @SuppressWarnings("unchecked")
    public Builder hiddenImpl(String className, Class<?>... types) {
      // don't do any work if an implementation has been found
      if (ctor != null) {
        return this;
      }

      try {
        Class targetClass = Class.forName(className);
        hiddenImpl(targetClass, types);
      } catch (NoClassDefFoundError e) {
        // cannot load this implementation
      } catch (ClassNotFoundException e) {
        // not the right implementation
      }
      return this;
    }

    public <T> Builder hiddenImpl(Class<T> targetClass, Class<?>... types) {
      // don't do any work if an implementation has been found
      if (ctor != null) {
        return this;
      }

      try {
        Constructor<T> hidden = targetClass.getDeclaredConstructor(types);
        AccessController.doPrivileged(new MakeAccessible(hidden));
        ctor = new Ctor<T>(hidden, targetClass);
      } catch (SecurityException e) {
        // unusable
      } catch (NoSuchMethodException e) {
        // not the right implementation
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    public <C> Ctor<C> buildChecked() throws NoSuchMethodException {
      if (ctor != null) {
        return ctor;
      }
      throw new NoSuchMethodException(
          "Cannot find constructor for " + baseClass);
    }

    @SuppressWarnings("unchecked")
    public <C> Ctor<C> build() {
      if (ctor != null) {
        return ctor;
      }
      throw new RuntimeException("Cannot find constructor for " + baseClass);
    }
  }

  private static class MakeAccessible implements PrivilegedAction<Void> {
    private Constructor<?> hidden;

    public MakeAccessible(Constructor<?> hidden) {
      this.hidden = hidden;
    }

    @Override
    public Void run() {
      hidden.setAccessible(true);
      return null;
    }
  }
}
