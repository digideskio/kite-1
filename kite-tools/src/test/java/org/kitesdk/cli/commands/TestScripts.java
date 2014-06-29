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

import com.beust.jcommander.internal.Lists;
import java.io.File;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestScripts {

  private Logger console = null;
  private RunScriptCommand command;

  @Before
  public void setup() {
    this.console = LoggerFactory.getLogger(TestScripts.class);
    this.command = new RunScriptCommand(console);
    command.setConf(new Configuration());
    command.mem = true;
  }

  @Test
  public void testWordCount() throws IOException {
    command.scripts = Lists.newArrayList("word_count.rb");
    command.run();
  }

  @Test
  public void testPyWordCount() throws IOException {
    command.scripts = Lists.newArrayList("word_count.py");
    command.run();
  }

  @After
  public void removeOutput() {
    File output = new File("target/output.text");
    if (output.exists()) {
      output.delete();
    }
  }

}
