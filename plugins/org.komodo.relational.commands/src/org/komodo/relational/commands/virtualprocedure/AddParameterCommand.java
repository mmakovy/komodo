/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.commands.virtualprocedure;

import static org.komodo.relational.commands.virtualprocedure.VirtualProcedureCommandMessages.AddParameterCommand.PARAMETER_ADDED;
import static org.komodo.relational.commands.virtualprocedure.VirtualProcedureCommandMessages.General.MISSING_PARAMETER_NAME;
import org.komodo.relational.model.VirtualProcedure;
import org.komodo.shell.CommandResultImpl;
import org.komodo.shell.api.CommandResult;
import org.komodo.shell.api.WorkspaceStatus;

/**
 * A shell command to add a Parameter to a VirtualProcedure.
 */
public final class AddParameterCommand extends VirtualProcedureShellCommand {

    static final String NAME = "add-parameter"; //$NON-NLS-1$

    /**
     * @param status
     *        the shell's workspace status (cannot be <code>null</code>)
     */
    public AddParameterCommand( final WorkspaceStatus status ) {
        super( NAME, status );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.BuiltInShellCommand#doExecute()
     */
    @Override
    protected CommandResult doExecute() {
        CommandResult result = null;

        try {
            final String paramName = requiredArgument( 0, getMessage( MISSING_PARAMETER_NAME ) );

            final VirtualProcedure proc = getVirtualProcedure();
            proc.addParameter( getTransaction(), paramName );

            result = new CommandResultImpl( getMessage( PARAMETER_ADDED, paramName ) );
        } catch ( final Exception e ) {
            result = new CommandResultImpl( e );
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.BuiltInShellCommand#getMaxArgCount()
     */
    @Override
    protected int getMaxArgCount() {
        return 1;
    }

}