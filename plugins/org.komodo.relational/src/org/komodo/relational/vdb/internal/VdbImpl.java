/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.vdb.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.komodo.modeshape.visitor.VdbNodeVisitor;
import org.komodo.relational.Messages;
import org.komodo.relational.Messages.Relational;
import org.komodo.relational.RelationalProperties;
import org.komodo.relational.internal.RelationalModelFactory;
import org.komodo.relational.internal.RelationalObjectImpl;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.relational.model.Model;
import org.komodo.relational.model.internal.ModelImpl;
import org.komodo.relational.vdb.DataRole;
import org.komodo.relational.vdb.Entry;
import org.komodo.relational.vdb.Translator;
import org.komodo.relational.vdb.Vdb;
import org.komodo.relational.vdb.VdbImport;
import org.komodo.repository.ObjectImpl;
import org.komodo.spi.KException;
import org.komodo.spi.constants.ExportConstants;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.PropertyValueType;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.spi.repository.Repository.UnitOfWork.State;
import org.komodo.spi.runtime.version.TeiidVersionProvider;
import org.komodo.utils.ArgCheck;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * An implementation of a virtual database manifest.
 */
public final class VdbImpl extends RelationalObjectImpl implements Vdb {

    private class VdbManifestImpl implements VdbManifest {

        private final String xml;

        VdbManifestImpl( final UnitOfWork transaction,
                         final VdbImpl vdb, final Properties exportProperties ) throws KException {
            final StringWriter writer = new StringWriter();

            try {
                final XMLOutputFactory xof = XMLOutputFactory.newInstance();
                final XMLStreamWriter xsw = xof.createXMLStreamWriter(writer);

                final VdbNodeVisitor visitor = new VdbNodeVisitor(TeiidVersionProvider.getInstance().getTeiidVersion(), xsw);
                if( exportProperties != null && !exportProperties.isEmpty() ) {
                	boolean useTabs = exportProperties.containsKey(ExportConstants.USE_TABS_PROP_KEY);
                	visitor.setShowTabs(useTabs);
                }
                visitor.visit(vdb.node(transaction));
            } catch (final Exception e) {
                throw new KException(e);
            }

            // Create an XML Document from the filled writer
            this.xml = writer.toString();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("VdbImpl#VdbManifestImpl: transaction = {0}, xml = {1}", //$NON-NLS-1$
                             transaction.getName(),
                             this.xml);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.vdb.Vdb.VdbManifest#asDocument()
         */
        @Override
        public Document asDocument() throws KException {
            String xmlText = this.xml.replaceAll(NEW_LINE, SPACE);
            xmlText = xmlText.replaceAll(">[\\s]+<", CLOSE_ANGLE_BRACKET + OPEN_ANGLE_BRACKET); //$NON-NLS-1$
            xmlText = xmlText.replaceAll("[\\s]+", SPACE); //$NON-NLS-1$
            xmlText = xmlText.replaceAll("CDATA\\[[\\s]+", "CDATA["); //$NON-NLS-1$ //$NON-NLS-2$
            xmlText = xmlText.replaceAll("; \\]\\]", ";]]"); //$NON-NLS-1$ //$NON-NLS-2$

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);

            try {
                final DocumentBuilder db = dbf.newDocumentBuilder();
                final Document doc = db.parse(new InputSource(new StringReader(xmlText)));
                doc.setXmlStandalone(true);
                doc.normalizeDocument();

                return doc;
            } catch (final Exception e) {
                throw new KException(e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.spi.repository.Exportable#export(org.komodo.spi.repository.Repository.UnitOfWork, java.util.Properties)
         */
        @Override
        public String export( final UnitOfWork transaction, Properties properties) {
            return this.xml;
        }

    }

    /**
     * The resolver of a {@link Vdb}.
     */
    public static final TypeResolver< Vdb > RESOLVER = new TypeResolver< Vdb >() {

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#create(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.Repository, org.komodo.spi.repository.KomodoObject, java.lang.String,
         *      org.komodo.relational.RelationalProperties)
         */
        @Override
        public Vdb create( final UnitOfWork transaction,
                           final Repository repository,
                           final KomodoObject parent,
                           final String id,
                           final RelationalProperties properties ) throws KException {
            final Object origFilePathValue = properties.getValue( VdbLexicon.Vdb.ORIGINAL_FILE );
            final String origFilePath = origFilePathValue == null ? null : origFilePathValue.toString();
            final String workspacePath = ( ( parent == null ) ? null : parent.getAbsolutePath() );
            return RelationalModelFactory.createVdb( transaction, repository, workspacePath, id, origFilePath );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#identifier()
         */
        @Override
        public KomodoType identifier() {
            return IDENTIFIER;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#owningClass()
         */
        @Override
        public Class< VdbImpl > owningClass() {
            return VdbImpl.class;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolvable(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public boolean resolvable( final UnitOfWork transaction,
                                   final KomodoObject kobject ) {
            try {
                ObjectImpl.validateType( transaction, kobject.getRepository(), kobject, VdbLexicon.Vdb.VIRTUAL_DATABASE );
                return true;
            } catch (final Exception e) {
                // not resolvable
            }

            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public Vdb resolve( final UnitOfWork transaction,
                            final KomodoObject kobject ) throws KException {
            return new VdbImpl( transaction, kobject.getRepository(), kobject.getAbsolutePath() );
        }

    };

    /**
     * @param uow
     *        the transaction (cannot be <code>null</code> or have a state that is not {@link State#NOT_STARTED})
     * @param repository
     *        the repository where the relational object exists (cannot be <code>null</code>)
     * @param workspacePath
     *        the workspace path (cannot be empty)
     * @throws KException
     *         if an error occurs
     */
    public VdbImpl( final UnitOfWork uow,
                    final Repository repository,
                    final String workspacePath ) throws KException {
        super(uow, repository, workspacePath);
        setVdbName(uow, getName(uow));
    }

    @Override
    public KomodoType getTypeIdentifier(UnitOfWork uow) {
        return KomodoType.VDB;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#addDataRole(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public DataRole addDataRole( final UnitOfWork transaction,
                                 final String dataRoleName ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final DataRole result = RelationalModelFactory.createDataRole( transaction, getRepository(), this, dataRoleName );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#addEntry(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Entry addEntry( final UnitOfWork transaction,
                           final String entryName,
                           final String entryPath ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final Entry result = RelationalModelFactory.createEntry( transaction, getRepository(), this, entryName, entryPath );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#addImport(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public VdbImport addImport( final UnitOfWork transaction,
                                final String vdbName ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final VdbImport result = RelationalModelFactory.createVdbImport( transaction, getRepository(), this, vdbName );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#addModel(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public Model addModel( final UnitOfWork transaction,
                           final String modelName ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final Model result = RelationalModelFactory.createModel( transaction, getRepository(), this, modelName );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#addTranslator(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Translator addTranslator( final UnitOfWork transaction,
                                     final String translatorName,
                                     final String translatorType ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final Translator result = RelationalModelFactory.createTranslator( transaction,
                                                                           getRepository(),
                                                                           this,
                                                                           translatorName,
                                                                           translatorType );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#createManifest(org.komodo.spi.repository.Repository.UnitOfWork, java.util.Properties)
     */
    @Override
    public VdbManifest createManifest( final UnitOfWork transaction,
                                       final Properties properties ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final VdbManifest result = new VdbManifestImpl( transaction, this, properties );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.Exportable#export(org.komodo.spi.repository.Repository.UnitOfWork, java.util.Properties)
     */
    @Override
    public String export( final UnitOfWork transaction,
                          final Properties properties ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final String result = createManifest( transaction, properties ).export( transaction, properties );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#getChildren(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public KomodoObject[] getChildren( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final DataRole[] dataRoles = getDataRoles( transaction );
        final Entry[] entries = getEntries( transaction );
        final VdbImport[] imports = getImports( transaction );
        final Model[] models = getModels( transaction );
        final Translator[] translators = getTranslators( transaction );

        final KomodoObject[] result = new KomodoObject[ dataRoles.length + entries.length + imports.length + models.length
                                                        + translators.length ];
        System.arraycopy( dataRoles, 0, result, 0, dataRoles.length );
        System.arraycopy( entries, 0, result, dataRoles.length, entries.length );
        System.arraycopy( imports, 0, result, dataRoles.length + entries.length, imports.length );
        System.arraycopy( models, 0, result, dataRoles.length + entries.length + imports.length, models.length );
        System.arraycopy( translators,
                          0,
                          result,
                          dataRoles.length + entries.length + imports.length + models.length,
                          translators.length );

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#getChildrenOfType(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public KomodoObject[] getChildrenOfType( final UnitOfWork transaction,
                                             final String type ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        KomodoObject[] result = null;

        if ( VdbLexicon.DataRole.DATA_ROLE.equals( type ) ) {
            result = getDataRoles( transaction );
        } else if ( VdbLexicon.Entry.ENTRY.equals( type ) ) {
            result = getEntries( transaction );
        } else if ( VdbLexicon.ImportVdb.IMPORT_VDB.equals( type ) ) {
            result = getImports( transaction );
        } else if ( VdbLexicon.Vdb.DECLARATIVE_MODEL.equals( type ) ) {
            result = getModels( transaction );
        } else if ( VdbLexicon.Translator.TRANSLATOR.equals( type ) ) {
            result = getTranslators( transaction );
        } else {
            result = KomodoObject.EMPTY_ARRAY;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getConnectionType(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getConnectionType( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.STRING, "getConnectionType", VdbLexicon.Vdb.CONNECTION_TYPE); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getDataRoles(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public DataRole[] getDataRoles( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        DataRole[] result = null;

        if ( hasChild( transaction, VdbLexicon.Vdb.DATA_ROLES, VdbLexicon.Vdb.DATA_ROLES ) ) {
            final KomodoObject grouping = getChild( transaction, VdbLexicon.Vdb.DATA_ROLES, VdbLexicon.Vdb.DATA_ROLES );
            final List< DataRole > temp = new ArrayList<>();

            for ( final KomodoObject kobject : grouping.getChildrenOfType( transaction, VdbLexicon.DataRole.DATA_ROLE ) ) {
                final DataRole dataRole = new DataRoleImpl( transaction, getRepository(), kobject.getAbsolutePath() );
                temp.add( dataRole );
            }

            result = temp.toArray( new DataRole[ temp.size() ] );
        } else {
            result = DataRole.NO_DATA_ROLES;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getDescription(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getDescription( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.STRING, "getDescription", VdbLexicon.Vdb.DESCRIPTION); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getEntries(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Entry[] getEntries( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        Entry[] result = null;

        if ( hasChild( transaction, VdbLexicon.Vdb.ENTRIES, VdbLexicon.Vdb.ENTRIES ) ) {
            final KomodoObject grouping = getChild( transaction, VdbLexicon.Vdb.ENTRIES, VdbLexicon.Vdb.ENTRIES );
            final List< Entry > temp = new ArrayList<>();

            for ( final KomodoObject kobject : grouping.getChildrenOfType( transaction, VdbLexicon.Entry.ENTRY ) ) {
                final Entry entry = new EntryImpl( transaction, getRepository(), kobject.getAbsolutePath() );
                temp.add( entry );
            }

            result = temp.toArray( new Entry[ temp.size() ] );
        } else {
            result = Entry.NO_ENTRIES;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getImports(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public VdbImport[] getImports( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        VdbImport[] result = null;

        if ( hasChild( transaction, VdbLexicon.Vdb.IMPORT_VDBS, VdbLexicon.Vdb.IMPORT_VDBS ) ) {
            final KomodoObject grouping = getChild( transaction, VdbLexicon.Vdb.IMPORT_VDBS, VdbLexicon.Vdb.IMPORT_VDBS );
            final List< VdbImport > temp = new ArrayList<>();

            for ( final KomodoObject kobject : grouping.getChildrenOfType( transaction, VdbLexicon.ImportVdb.IMPORT_VDB ) ) {
                final VdbImport vdbImport = new VdbImportImpl( transaction, getRepository(), kobject.getAbsolutePath() );
                temp.add( vdbImport );
            }

            result = temp.toArray( new VdbImport[ temp.size() ] );
        } else {
            result = VdbImport.NO_IMPORTS;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getModels(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Model[] getModels( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final List< Model > result = new ArrayList<>();

        for ( final KomodoObject kobject : super.getChildrenOfType( transaction, VdbLexicon.Vdb.DECLARATIVE_MODEL ) ) {
            final Model model = new ModelImpl( transaction, getRepository(), kobject.getAbsolutePath() );
            result.add( model );
        }

        if ( result.isEmpty() ) {
            return Model.NO_MODELS;
        }

        return result.toArray( new Model[ result.size() ] );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getOriginalFilePath(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getOriginalFilePath( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.STRING, "getOriginalFilePath", VdbLexicon.Vdb.ORIGINAL_FILE); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getTranslators(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Translator[] getTranslators( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        Translator[] result = null;

        if ( hasChild( transaction, VdbLexicon.Vdb.TRANSLATORS, VdbLexicon.Vdb.TRANSLATORS ) ) {
            final KomodoObject grouping = getChild( transaction, VdbLexicon.Vdb.TRANSLATORS, VdbLexicon.Vdb.TRANSLATORS );
            final List< Translator > temp = new ArrayList<>();

            for ( final KomodoObject kobject : grouping.getChildrenOfType( transaction, VdbLexicon.Translator.TRANSLATOR ) ) {
                final Translator translator = new TranslatorImpl( transaction, getRepository(), kobject.getAbsolutePath() );
                temp.add( translator );
            }

            result = temp.toArray( new Translator[ temp.size() ] );
        } else {
            result = Translator.NO_TRANSLATORS;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.KomodoObject#getTypeId()
     */
    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getVdbName(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getVdbName( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.STRING, "getVdbName", VdbLexicon.Vdb.NAME); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#getVersion(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public int getVersion( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.INTEGER, "getVersion", VdbLexicon.Vdb.VERSION); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#isPreview(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public boolean isPreview( final UnitOfWork uow ) throws KException {
        return getObjectProperty(uow, PropertyValueType.BOOLEAN, "isPreview", VdbLexicon.Vdb.PREVIEW); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#removeDataRole(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void removeDataRole( final UnitOfWork transaction,
                                final String dataRoleToRemove ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( dataRoleToRemove, "dataRoleToRemove" ); //$NON-NLS-1$

        boolean found = false;
        final DataRole[] dataRoles = getDataRoles( transaction );

        if ( dataRoles.length != 0 ) {
            for ( final DataRole dataRole : dataRoles ) {
                if ( dataRoleToRemove.equals( dataRole.getName( transaction ) ) ) {
                    dataRole.remove( transaction );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KException( Messages.getString( Relational.DATA_ROLE_NOT_FOUND_TO_REMOVE, dataRoleToRemove ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#removeEntry(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void removeEntry( final UnitOfWork transaction,
                             final String entryToRemove ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( entryToRemove, "entryToRemove" ); //$NON-NLS-1$

        boolean found = false;
        final Entry[] entries = getEntries( transaction );

        if ( entries.length != 0 ) {
            for ( final Entry entry : entries ) {
                if ( entryToRemove.equals( entry.getName( transaction ) ) ) {
                    entry.remove( transaction );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KException( Messages.getString( Relational.ENTRY_NOT_FOUND_TO_REMOVE, entryToRemove ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#removeImport(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void removeImport( final UnitOfWork transaction,
                              final String importToRemove ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( importToRemove, "importToRemove" ); //$NON-NLS-1$

        boolean found = false;
        final VdbImport[] vdbImports = getImports( transaction );

        if ( vdbImports.length != 0 ) {
            for ( final VdbImport vdbImport : vdbImports ) {
                if ( importToRemove.equals( vdbImport.getName( transaction ) ) ) {
                    vdbImport.remove( transaction );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KException( Messages.getString( Relational.VDB_IMPORT_NOT_FOUND_TO_REMOVE, importToRemove ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#removeModel(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void removeModel( final UnitOfWork transaction,
                             final String modelToRemove ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( modelToRemove, "modelToRemove" ); //$NON-NLS-1$

        boolean found = false;
        final Model[] models = getModels( transaction );

        if ( models.length != 0 ) {
            for ( final Model model : models ) {
                if ( modelToRemove.equals( model.getName( transaction ) ) ) {
                    model.remove( transaction );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KException( Messages.getString( Relational.MODEL_NOT_FOUND_TO_REMOVE, modelToRemove ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#removeTranslator(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void removeTranslator( final UnitOfWork transaction,
                                  final String translatorToRemove ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( translatorToRemove, "translatorToRemove" ); //$NON-NLS-1$

        boolean found = false;
        final Translator[] translators = getTranslators( transaction );

        if ( translators.length != 0 ) {
            for ( final Translator translator : translators ) {
                if ( translatorToRemove.equals( translator.getName( transaction ) ) ) {
                    translator.remove( transaction );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KException( Messages.getString( Relational.TRANSLATOR_NOT_FOUND_TO_REMOVE, translatorToRemove ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#rename(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void rename( final UnitOfWork transaction,
                        final String newName ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( newName, "newName" ); //$NON-NLS-1$
        ArgCheck.doesNotContain( newName, "/", "newName" ); //$NON-NLS-1$ //$NON-NLS-2$

        super.rename( transaction, newName );
        setVdbName( transaction, newName );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setConnectionType(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setConnectionType( final UnitOfWork uow,
                                   final String newConnectionType ) throws KException {
        setObjectProperty(uow, "setConnectionType", VdbLexicon.Vdb.CONNECTION_TYPE, newConnectionType); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setDescription(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setDescription( final UnitOfWork uow,
                                final String newDescription ) throws KException {
        setObjectProperty(uow, "setDescription", VdbLexicon.Vdb.DESCRIPTION, newDescription); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setOriginalFilePath(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setOriginalFilePath( final UnitOfWork uow,
                                     final String newOriginalFilePath ) throws KException {
        ArgCheck.isNotEmpty(newOriginalFilePath, "newOriginalFilePath"); //$NON-NLS-1$
        setObjectProperty(uow, "setOriginalFilePath", VdbLexicon.Vdb.ORIGINAL_FILE, newOriginalFilePath); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setPreview(org.komodo.spi.repository.Repository.UnitOfWork, boolean)
     */
    @Override
    public void setPreview( final UnitOfWork uow,
                            final boolean newPreview ) throws KException {
        setObjectProperty(uow, "setPreview", VdbLexicon.Vdb.PREVIEW, newPreview); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setVdbName(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setVdbName( final UnitOfWork uow,
                            final String newVdbName ) throws KException {
        setObjectProperty(uow, "setVdbName", VdbLexicon.Vdb.NAME, newVdbName); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.Vdb#setVersion(org.komodo.spi.repository.Repository.UnitOfWork, int)
     */
    @Override
    public void setVersion( final UnitOfWork uow,
                            final int newVersion ) throws KException {
        setObjectProperty(uow, "setVersion", VdbLexicon.Vdb.VERSION, newVersion); //$NON-NLS-1$
    }

}
