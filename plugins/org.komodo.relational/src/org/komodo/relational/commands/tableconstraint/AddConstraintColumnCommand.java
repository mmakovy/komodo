/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.commands.tableconstraint;

import static org.komodo.relational.commands.tableconstraint.TableConstraintCommandMessages.AddConstraintColumnCommand.COLUMN_PATH_NOT_FOUND;
import static org.komodo.relational.commands.tableconstraint.TableConstraintCommandMessages.AddConstraintColumnCommand.COLUMN_REF_ADDED;
import static org.komodo.relational.commands.tableconstraint.TableConstraintCommandMessages.AddConstraintColumnCommand.INVALID_COLUMN;
import static org.komodo.relational.commands.tableconstraint.TableConstraintCommandMessages.AddConstraintColumnCommand.INVALID_COLUMN_PATH;
import static org.komodo.relational.commands.tableconstraint.TableConstraintCommandMessages.AddConstraintColumnCommand.MISSING_COLUMN_PATH;
import java.util.Arrays;
import java.util.List;
import org.komodo.relational.commands.FindCommand;
import org.komodo.relational.model.Column;
import org.komodo.relational.model.TableConstraint;
import org.komodo.shell.CommandResultImpl;
import org.komodo.shell.api.CommandResult;
import org.komodo.shell.api.WorkspaceStatus;
import org.komodo.shell.util.ContextUtils;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.Repository;
import org.komodo.utils.StringUtils;

/**
 * A shell command to add a column to a {@link TableConstraint}.
 */
public final class AddConstraintColumnCommand extends TableConstraintShellCommand {

    static final String NAME = "add-column"; //$NON-NLS-1$

    /**
     * @param status
     *        the shell's workspace status (cannot be <code>null</code>)
     */
    public AddConstraintColumnCommand( final WorkspaceStatus status ) {
        super( status, NAME );
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
            final String columnPath = requiredArgument( 0, getMessage( MISSING_COLUMN_PATH ) );

            // get reference of the column at the specified path
            final KomodoObject column = ContextUtils.getContextForPath( getWorkspaceStatus(), columnPath );

            if ( column == null ) {
                result = new CommandResultImpl( false, getMessage( COLUMN_PATH_NOT_FOUND, columnPath ), null );
            } else if ( column instanceof Column ) {
                final TableConstraint constraint = getTableConstraint();

                // must be a column in the parent of the table constraint
                final Repository.UnitOfWork transaction = getWorkspaceStatus().getTransaction();
                final KomodoObject parentTable = constraint.getParent( transaction );

                if ( parentTable.equals( column.getParent( transaction ) ) ) {
                    constraint.addColumn( transaction, ( Column )column );
                    result = new CommandResultImpl( getMessage( COLUMN_REF_ADDED, columnPath, getContext().getAbsolutePath() ) );
                } else {
                    result = new CommandResultImpl( false,
                                                    getMessage( INVALID_COLUMN,
                                                                getWorkspaceStatus().getLabelProvider()
                                                                                    .getDisplayPath( column.getAbsolutePath() ),
                                                                constraint.getName( transaction ) ),
                                                    null );
                }
            } else {
                result = new CommandResultImpl( false, getMessage( INVALID_COLUMN_PATH, columnPath ), null );
            }
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

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.BuiltInShellCommand#tabCompletion(java.lang.String, java.util.List)
     */
    @Override
    public int tabCompletion( final String lastArgument,
                              final List< CharSequence > candidates ) throws Exception {
        if ( getArguments().isEmpty() ) {

            // find columns
            final KomodoObject parent = getContext().getParent( getTransaction() );
            final String[] columnPaths = FindCommand.query( getWorkspaceStatus(),
                                                            KomodoType.COLUMN,
                                                            parent.getAbsolutePath(),
                                                            null );

            if ( columnPaths.length == 0 ) {
                return -1;
            }

            if ( StringUtils.isBlank( lastArgument ) ) {
                candidates.addAll( Arrays.asList( columnPaths ) );
            } else {
                for ( final String item : Arrays.asList( columnPaths ) ) {
                    if ( item.startsWith( lastArgument ) ) {
                        candidates.add( item );
                    }
                }
            }

            return ( candidates.isEmpty() ? -1 : ( toString().length() + 1 ) );
        }

        // no completions if more than one arg
        return -1;
    }

}
