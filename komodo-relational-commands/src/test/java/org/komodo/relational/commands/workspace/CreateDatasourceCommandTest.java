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
package org.komodo.relational.commands.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.komodo.relational.commands.AbstractCommandTest;
import org.komodo.relational.datasource.Datasource;
import org.komodo.relational.workspace.WorkspaceManager;
import org.komodo.shell.api.CommandResult;

/**
 * Test Class to test {@link CreateDatasourceCommand}.
 */
@SuppressWarnings( { "javadoc", "nls" } )
public final class CreateDatasourceCommandTest extends AbstractCommandTest {

    @Test
    public void shouldCreateJdbcDatasource() throws Exception {
        final String[] commands = { "create-datasource testSource" };
        final CommandResult result = execute( commands );
        assertCommandResultOk(result);

        WorkspaceManager wkspMgr = WorkspaceManager.getInstance(_repo);
        Datasource[] datasources = wkspMgr.findDatasources(getTransaction());

        assertEquals(1, datasources.length);
        assertEquals("testSource", datasources[0].getName(getTransaction()));
        assertTrue(datasources[0].isJdbc(getTransaction()));
    }

    @Test
    public void shouldCreateNonJdbcDatasource() throws Exception {
        final String[] commands = { "create-datasource testSource false" };
        final CommandResult result = execute( commands );
        assertCommandResultOk(result);

        WorkspaceManager wkspMgr = WorkspaceManager.getInstance(_repo);
        Datasource[] datasources = wkspMgr.findDatasources(getTransaction());

        assertEquals(1, datasources.length);
        assertEquals("testSource", datasources[0].getName(getTransaction()));
        assertFalse(datasources[0].isJdbc(getTransaction()));
    }

}
