/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.morphline.api;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kitesdk.morphline.shaded.com.google.common.reflect.ClassPath;
import org.kitesdk.morphline.shaded.com.google.common.reflect.ClassPath.ClassInfo;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Preconditions;

/**
 * Additional user defined parameters that will be passed to all morphline commands.
 */
public class MorphlineContext {

  private ExceptionHandler exceptionHandler;
  private MetricRegistry metricRegistry;
  private HealthCheckRegistry healthCheckRegistry;
  private Map<String, Class<CommandBuilder>> commandBuilders = Collections.EMPTY_MAP;

  private static final Logger LOG = LoggerFactory.getLogger(MorphlineContext.class);

  /** For public access use {@link Builder#build()} instead */  
  protected MorphlineContext() {}


  public ExceptionHandler getExceptionHandler() {
    assert exceptionHandler != null;
    return exceptionHandler;
  }

  public MetricRegistry getMetricRegistry() {
    assert metricRegistry != null;
    return metricRegistry;
  }

  public HealthCheckRegistry getHealthCheckRegistry() {
    assert healthCheckRegistry != null;
    return healthCheckRegistry;
  }

  public Class<CommandBuilder> getCommandBuilder(String builderName) {
    return commandBuilders.get(builderName);
  }

  public void importCommandBuilders(Collection<String> importSpecs) {
    if (commandBuilders == Collections.EMPTY_MAP) { // intentionally effective no more than once
      commandBuilders = new HashMap();
      LOG.info("Importing commands");
      Collection<Class<CommandBuilder>> builderClasses = getTopLevelClasses(importSpecs, CommandBuilder.class);
      for (Class<CommandBuilder> builderClass : builderClasses) {
        try {
          CommandBuilder builder = builderClass.newInstance();
          for (String builderName : builder.getNames()) {
            LOG.debug("Importing command: {} from class: {}", builderName, builderClass.getName());
            if (builderName.contains(".")) {
              LOG.warn("Command name should not contain a period character: " + builderName);
            }
            commandBuilders.put(builderName, builderClass);
          }
        } catch (Exception e) {
          throw new MorphlineRuntimeException(e);
        }
      }
      LOG.info("Done importing commands");
    }
  }

  /**
   * Returns all classes that implement the given interface and are contained in a Java package or
   * its subpackages (importSpec ends with ".**"), or are contained in the given Java package
   * (importSpec ends with ".*"), or are a Java class with the given fully qualified class name
   * (importSpec ends otherwise).
   * 
   * Uses a shaded version of com.google.guava.reflect-14.0.1 to enable running with prior versions
   * of guava without issues.
   */
  <T> Collection<Class<T>> getTopLevelClasses(Iterable<String> importSpecs, Class<T> iface) {    
    HashMap<String,Class<T>> classes = new LinkedHashMap();
    for (ClassLoader loader : getClassLoaders()) {
      ClassPath classPath;
      try {
        classPath = ClassPath.from(loader);
      } catch (IOException e) {
        continue;
      }
      for (String importSpec : importSpecs) {
        Set<ClassInfo> classInfos = null;
        if (importSpec.endsWith(".**")) {
          String packageName = importSpec.substring(0, importSpec.length() - ".**".length());
          classInfos = classPath.getTopLevelClassesRecursive(packageName);
        } else if (importSpec.endsWith(".*")) {
          String packageName = importSpec.substring(0, importSpec.length() - ".*".length());
          classInfos = classPath.getTopLevelClasses(packageName);
        } else { // importSpec is assumed to be a fully qualified class name
          Class clazz;
          try {
            //clazz = Class.forName(importSpec, true, loader);
            clazz = loader.loadClass(importSpec);
          } catch (ClassNotFoundException e) {
            continue;
          }
          addClass(clazz, classes, iface);
          continue;
        }
        
        for (ClassInfo info : classInfos) {
          Class clazz;
          try {
            clazz = info.load();
//            clazz = Class.forName(info.getName());
          } catch (NoClassDefFoundError e) {
            continue;
          } catch (ExceptionInInitializerError e) {
            continue;
          } catch (UnsatisfiedLinkError e) {
            continue;
          }
          addClass(clazz, classes, iface);
        }
      }
    }    
    return classes.values();
  }
  
  private <T> void addClass(Class clazz, HashMap<String,Class<T>> classes, Class<T> iface) {
    if (!classes.containsKey(clazz.getName()) 
        && iface.isAssignableFrom(clazz) 
        && !clazz.isInterface()
        && !Modifier.isAbstract(clazz.getModifiers())) {
      for (Constructor ctor : clazz.getConstructors()) { // all public ctors
        if (ctor.getParameterTypes().length == 0) { // is public zero-arg ctor?
          classes.put(clazz.getName(), clazz);                
        }
      }
    }    
  }

  private ClassLoader[] getClassLoaders() {
    ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader myLoader = getClass().getClassLoader();
    if (contextLoader == null) {
      return new ClassLoader[] { myLoader };
    } else if (contextLoader == myLoader || myLoader == null) {
      return new ClassLoader[] { contextLoader };
    } else {
      return new ClassLoader[] { contextLoader, myLoader };
    }
  }

  
  ///////////////////////////////////////////////////////////////////////////////
  // Nested classes:
  ///////////////////////////////////////////////////////////////////////////////
  /**
   * Helper to construct a {@link MorphlineContext} instance.
   * 
   * Example usage: 
   * 
   * <pre>
   * MorphlineContext context = new MorphlineContext.Builder().setMetricRegistry(new MetricRegistry()).build();
   * </pre>
   */
  public static class Builder {
    
    protected MorphlineContext context = create();
    private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();
    private MetricRegistry metricRegistry = new MetricRegistry();
    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
    
    public Builder() {}

    public Builder setExceptionHandler(ExceptionHandler exceptionHandler) {
      Preconditions.checkNotNull(exceptionHandler);
      this.exceptionHandler = exceptionHandler;
      return this;
    }    

    public Builder setMetricRegistry(MetricRegistry metricRegistry) {
      Preconditions.checkNotNull(metricRegistry);
      this.metricRegistry = metricRegistry;
      return this;
    }

    public Builder setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
      Preconditions.checkNotNull(healthCheckRegistry);
      this.healthCheckRegistry = healthCheckRegistry;
      return this;
    }

    public MorphlineContext build() {
      context.exceptionHandler = exceptionHandler;
      context.metricRegistry = metricRegistry;
      context.healthCheckRegistry = healthCheckRegistry;
      return context;
    }

    protected MorphlineContext create() {
      return new MorphlineContext();
    }
    
  }

  ///////////////////////////////////////////////////////////////////////////////
  // Nested classes:
  ///////////////////////////////////////////////////////////////////////////////
  private static final class DefaultExceptionHandler implements ExceptionHandler {
    
    @Override
    public void handleException(Throwable t, Record record) {
      if (t instanceof Error) {
        throw (Error) t;
      }
      throw new MorphlineRuntimeException(t);
    }
    
  }
}
