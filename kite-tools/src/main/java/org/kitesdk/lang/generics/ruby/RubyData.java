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

package org.kitesdk.lang.generics.ruby;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.IndexedRecord;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.javasupport.JavaUtil;
import org.kitesdk.lang.generics.CustomData;

/**
 * Ruby-specific alterations to CustomData.
 */
public class RubyData extends CustomData {
  private final Ruby runtime;

  public RubyData() {
    this(Ruby.getGlobalRuntime());
  }

  public static RubyData get() {
    return new RubyData();
  }

  public RubyData(Ruby runtime) {
    super(RubyDataFactory.get(runtime), true /* reuse containers */);
    this.runtime = runtime;
  }

  @Override
  protected boolean isRecord(Object datum) {
    return (datum instanceof RubyStruct);
  }

  @Override
  protected boolean isEnum(Object datum) {
    // symbols are part of an enum if they have an known schema
    return (datum instanceof RubySymbol) && hasSchema(datum);
  }

  @Override
  protected boolean isString(Object datum) {
    // if a symbol does not have a known schema, then it is a string
    return (datum instanceof RubyString) || (datum instanceof CharSequence) ||
        ((datum instanceof RubySymbol) && !hasSchema(datum));
  }

  @Override
  protected boolean isNull(Object datum) {
    return (datum instanceof RubyNil);
  }

  @Override
  public void setField(Object record, String name, int pos, Object value) {
    if (record instanceof RubyStruct) {
      RubyStruct struct = (RubyStruct) record; // problems => ClassCastException
      struct.set(JavaUtil.convertJavaToUsableRubyObject(runtime, value), pos);
    } else if (record instanceof IndexedRecord) {
      ((IndexedRecord) record).put(pos, value);
    } else {
      throw new AvroRuntimeException(
          "Unsupported record type: " + record.getClass());
    }
  }

  @Override
  public Object getField(Object record, String name, int position) {
    if (record instanceof RubyStruct) {
      RubyStruct struct = (RubyStruct) record; // problems => ClassCastException
      return struct.get(position).toJava(Object.class);
    } else if (record instanceof IndexedRecord) {
      return ((IndexedRecord) record).get(position);
    } else {
      throw new AvroRuntimeException(
          "Unsupported record type: " + record.getClass());
    }
  }

}
