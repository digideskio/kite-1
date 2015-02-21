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

package org.kitesdk.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import org.apache.avro.Schema;
import org.kitesdk.data.spi.Compatibility;
import org.kitesdk.data.spi.SchemaUtil;
import org.slf4j.Logger;

@Parameters(commandDescription = "Show the schema for a Dataset")
public class MergeSchemasCommand extends BaseDatasetCommand {

  @Parameter(description = "<uris-or-files>")
  List<String> schemas;

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(
      value="UWF_NULL_FIELD",
      justification = "Field set by JCommander")
  @Parameter(names={"-o", "--output"}, description="Save schema avsc to path")
  String outputPath = null;

  @Parameter(names="--minimize",
      description="Minimize schema file size by eliminating white space")
  boolean minimize=false;

  public MergeSchemasCommand(Logger console) {
    super(console);
  }

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="NP_NULL_ON_SOME_PATH",
      justification="Null case checked by precondition")
  public int run() throws IOException {
    Preconditions.checkArgument(
        schemas != null && !schemas.isEmpty(),
        "Missing schemas to merge");

    Schema mergedSchema = null;
    for (String uriOrPath : schemas) {
      mergedSchema = merge(mergedSchema, schema(uriOrPath));
    }

    Preconditions.checkNotNull(mergedSchema, "No valid schema found");

    output(mergedSchema.toString(!minimize), console, outputPath);

    return 0;
  }

  @Override
  public List<String> getExamples() {
    return Lists.newArrayList(
        "# Print the schema for dataset \"users\" to standard out:",
        "users",
        "# Merge user.avsc with the schema for dataset \"users\":",
        "user.avsc users"
    );
  }

  private Schema schema(String uriOrPath) throws IOException {
    if (!isDatasetOrViewUri(uriOrPath) &&
        !Compatibility.isCompatibleName(uriOrPath)) {
      return new Schema.Parser().parse(open(uriOrPath));
    } else {
      return load(uriOrPath, Object.class)
          .getDataset().getDescriptor().getSchema();
    }
  }

  private static Schema merge(Schema left, Schema right) {
    if (left == null) {
      return right;
    } else if (right == null) {
      return left;
    } else {
      return SchemaUtil.merge(left, right);
    }
  }

}
