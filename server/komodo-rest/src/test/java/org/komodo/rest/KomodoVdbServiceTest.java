/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.komodo.rest;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.komodo.rest.json.RestVdb;
import org.komodo.rest.json.RestVdbDescriptor;
import org.komodo.rest.json.RestVdbDirectory;
import org.komodo.spi.constants.SystemConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings( { "javadoc", "nls" } )
public final class KomodoVdbServiceTest {

    private static Path _kengineDataDir;
    private static KomodoRestV1Application _restApp;
    private static UndertowJaxrsServer _server;
    private static final Gson GSON = new GsonBuilder().create();

    @AfterClass
    public static void afterAll() throws Exception {
        _server.stop();
        _restApp.stop();
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        _kengineDataDir = Files.createTempDirectory( null, new FileAttribute[ 0 ] );
        System.setProperty( SystemConstants.ENGINE_DATA_DIR, _kengineDataDir.toString() );

        _server = new UndertowJaxrsServer().start();

        _restApp = new KomodoRestV1Application();
        _server.deploy( _restApp );
    }

    private Client client;
    private Response response;

    @Rule
    public TestName testName = new TestName();

    @After
    public void afterEach() throws Exception {
        if ( this.response != null ) {
            this.response.close();
        }

        this.client.close();

        _restApp.clearRepository();
    }

    @Before
    public void beforeEach() {
        this.client = ClientBuilder.newClient();
    }

    private RestVdb[] createVdbs( final int numVdbsToCreate ) {
        final RestVdb[] result = new RestVdb[ numVdbsToCreate ];

        for ( int i = 0; i < numVdbsToCreate; ++i ) {
            final String vdbName = ( this.testName.getMethodName() + '_' + ( i + 1 ) );
            final String description = vdbName + " description goes here";
            final String extPath = "/vdbs/" + vdbName + ".xml";

            final RestVdb restVdb = new RestVdb( vdbName );
            restVdb.setDescription( description );
            restVdb.setOriginalFilePath( extPath );
            result[ i ] = restVdb;

            final String input = restVdb.toJson();
            this.response = request( "/komodo/v1/workspace/vdbs" ).post( Entity.json( input ) );
            this.response.close(); // must close before making another request
        }

        return result;
    }

    protected Invocation.Builder request( final String url ) {
        return this.client.target( TestPortProvider.generateURL( url ) ).request();
    }

    @Test
    public void shouldCreateVdb() throws Exception {
        final String vdbName = this.testName.getMethodName();
        final String description = "this is your VDB description";
        final String extPath = "c:/vdbs/YourVdb.xml";
        final RestVdb restVdb = new RestVdb( vdbName );
        restVdb.setDescription( description );
        restVdb.setOriginalFilePath( extPath );

        final String input = restVdb.toJson();
        this.response = request( "/komodo/v1/workspace/vdbs" ).post( Entity.json( input ) );
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( notNullValue() ) );

        // make sure the VDB descriptor JSON document is returned
        final RestVdbDescriptor descriptor = GSON.fromJson( entity, RestVdbDescriptor.class );
        assertThat( descriptor.getName(), is( vdbName ) );
        assertThat( descriptor.getDescription(), is( description ) );
        assertThat( descriptor.getLinks().length, is( 4 ) );
    }

    @Test
    public void shouldGetVdb() throws Exception {
        final RestVdb[] vdbs = createVdbs( 1 );

        // get
        this.response = request( "/komodo/v1/workspace/vdbs/" + vdbs[ 0 ].getName() ).get();
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( notNullValue() ) );

        // make sure the VDB descriptor JSON document is returned
        final RestVdbDescriptor descriptor = GSON.fromJson( entity, RestVdbDescriptor.class );
        assertThat( descriptor.getName(), is( vdbs[ 0 ].getName() ) );
        assertThat( descriptor.getDescription(), is( vdbs[ 0 ].getDescription() ) );
        assertThat( descriptor.getLinks().length, is( 4 ) );
    }

    @Test
    public void shouldGetVdbWhenPatternMatches() throws Exception {
        final RestVdb[] vdbs = createVdbs( 1 );

        this.response = request( "/komodo/v1/workspace/vdbs/?pattern=" + vdbs[ 0 ].getName().charAt( 0 ) + '*' ).get();
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( notNullValue() ) );

        final RestVdbDirectory vdbDir = GSON.fromJson( entity, RestVdbDirectory.class );
        assertThat( vdbDir.getDescriptors().length, is( 1 ) );
    }

    @Test
    public void shouldLimitNumberOfVdbsReturnedWhenUsingSizeQueryParameter() throws Exception {
        createVdbs( 10 );
        final int resultSize = 5;

        this.response = request( "/komodo/v1/workspace/vdbs/?size=" + resultSize ).get();
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( notNullValue() ) );

        final RestVdbDirectory vdbDir = GSON.fromJson( entity, RestVdbDirectory.class );
        assertThat( vdbDir.getDescriptors().length, is( resultSize ) );
    }

    @Test
    public void shouldLimitNumberOfVdbsReturnedWhenUsingStartQueryParameter() throws Exception {
        final int numToCreate = 10;
        final RestVdb[] vdbs = createVdbs( numToCreate );
        final int start = 8;

        this.response = request( "/komodo/v1/workspace/vdbs/?start=" + start ).get();
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( notNullValue() ) );

        final RestVdbDirectory vdbDir = GSON.fromJson( entity, RestVdbDirectory.class );
        assertThat( vdbDir.getDescriptors().length, is( numToCreate - start ) );

        // check content
        assertThat( vdbs[ start - 1 ].getName(), is( vdbDir.getDescriptors()[ 0 ].getName() ) );
    }

    @Test
    public void shouldNotFindVdb() throws Exception {
        this.response = request( "/komodo/v1/workspace/vdbs/blah" ).get();
        assertThat( this.response.getStatus(), is( Status.NOT_FOUND.getStatusCode() ) );
    }

    @Test
    public void shouldReturnEmptyResponseWhenNoVdbsInWorkspace() {
        this.response = request( "/komodo/v1/workspace/vdbs" ).get();
        final String entity = this.response.readEntity( String.class );
        assertThat( entity, is( "{\"vdbs\":[]}" ) );
    }

}