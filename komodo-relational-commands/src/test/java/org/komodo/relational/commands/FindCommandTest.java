/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.komodo.relational.commands;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.komodo.shell.api.CommandResult;

/**
 * Test Class to test FindCommand
 *
 */
@SuppressWarnings( { "javadoc", "nls" } )
public final class FindCommandTest extends AbstractCommandTest {

    @Test
    public void testFind1() throws Exception {
        final String[] commands = { "set-auto-commit false",
                                    "create-vdb testVdb1 vdbPath",
                                    "create-vdb testVdb2 vdbPath",
                                    "commit",
                                    "find Vdb" };
        final CommandResult result = execute( commands );
        assertCommandResultOk(result);

        // Make sure the two VDBs are found
        String writerOutput = getCommandOutput();
        assertTrue(writerOutput.contains("/workspace/testVdb1"));
        assertTrue(writerOutput.contains("/workspace/testVdb2"));
    }

}
