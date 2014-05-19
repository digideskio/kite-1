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

package org.kitesdk.lang.carriers;

import org.apache.crunch.Pair;
import org.kitesdk.lang.Script;
import org.kitesdk.lang.Stage;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
    value="SE_NO_SERIALVERSIONID",
    justification="Purposely not compatible with other versions")
public class FromGroupedTable<KI, VI, T> extends FromTable<KI, Iterable<VI>, T> {
  public FromGroupedTable(String name, Script script,
                             Stage<Pair<KI, Iterable<VI>>, T> stage) {
    super(name, script, stage);
  }

  // TODO: Add Iterable wrapper here and in Combiner
}
