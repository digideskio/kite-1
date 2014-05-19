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

package org.kitesdk.lang.utils;

import com.google.common.base.Preconditions;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.PCollection;
import org.apache.crunch.Pair;
import org.apache.crunch.types.PTableType;
import org.apache.crunch.types.PType;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
    value="SE_NO_SERIALVERSIONID",
    justification="Purposely not compatible with other versions")
public class ToKeyValueTable<K, V> extends DoFn<Pair<K, V>, Pair<K, V>> {

  private final PTableType<K, V> outType;

  @SuppressWarnings("unchecked")
  public ToKeyValueTable(PCollection<Pair<K, V>> collection) {
    PType<Pair<K, V>> inType = collection.getPType();
    Preconditions.checkArgument(inType instanceof Pair, "ToTableShim is needed");
    PType<K> keyType = (PType<K>) inType.getSubTypes().get(0);
    PType<V> valueType = (PType<V>) inType.getSubTypes().get(1);
    this.outType = inType.getFamily().tableOf(keyType, valueType);
  }

  public PTableType<K, V> getTableType() {
    return outType;
  }

  @Override
  public void process(Pair<K, V> input, Emitter<Pair<K, V>> emitter) {
    emitter.emit(input);
  }

}
