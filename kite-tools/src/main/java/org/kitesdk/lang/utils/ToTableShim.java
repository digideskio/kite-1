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
import org.apache.crunch.types.avro.Avros;

/**
 * Translates from a PCollection to a PTable.
 *
 * If the PCollection contained Pairs, then the resulting table is made from
 * those Pairs. Otherwise, the table is created with Pairs containing each
 * object from the PCollection as a key and null values.
 *
 * @param <I>
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(
    value="SE_NO_SERIALVERSIONID",
    justification="Purposely not compatible with other versions")
public class ToTableShim<I> extends DoFn<I, Pair<I, I>> {

  private final PTableType<I, I> outType;

  public ToTableShim(PCollection<I> collection) {
    PType<I> inType = collection.getPType();
    Preconditions.checkArgument(!(inType instanceof Pair),
        "ToTableShim is not needed, use PTables.asPTable(collection)");
    this.outType = inType.getFamily().tableOf(inType, inType);
  }

  public PTableType<I, I> getTableType() {
    return outType;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void process(I input, Emitter<Pair<I, I>> emitter) {
    if (input instanceof Pair) {
      emitter.emit(Pair.of(
          ((Pair<I, I>) input).first(),
          ((Pair<I, I>) input).second()));
    } else {
      emitter.emit(Pair.of(input, (I) null));
    }
  }

}
