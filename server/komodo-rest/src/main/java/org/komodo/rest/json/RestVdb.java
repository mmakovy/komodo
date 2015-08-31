/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.komodo.rest.json;

import java.util.Objects;
import org.komodo.utils.ArgCheck;
import com.google.gson.annotations.SerializedName;

/**
 * A VDB that can be used by GSON to build a JSON document representation.
 */
public final class RestVdb extends KomodoRestEntity {

    @SerializedName( "id" )
    private String name;
    private String description;
    private String originalFilePath;

    // data roles
    // *entries
    // *imports
    // models
    // *translators

    /**
     * Constructor for use <strong>only</strong> when deserializing.
     */
    public RestVdb() {
        // nothing to do
    }

    /**
     * @param vdbName
     *        the name of the VDB (cannot be empty)
     */
    public RestVdb( final String vdbName ) {
        ArgCheck.isNotEmpty( vdbName, "vdbName" ); //$NON-NLS-1$
        this.name = vdbName;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object other ) {
        if ( !super.equals( other ) ) {
            return false;
        }

        assert( other != null );
        assert( getClass().equals( other.getClass() ) );

        final RestVdb that = ( RestVdb )other;

        // check name
        if ( this.name == null ) {
            if ( that.name != null ) {
                return false;
            }
        } else if ( !this.name.equals( that.name ) ) {
            return false;
        }

        // check description
        if ( this.description == null ) {
            if ( that.description != null ) {
                return false;
            }
        } else if ( !this.description.equals( that.description ) ) {
            return false;
        }

        // check file path
        if ( this.originalFilePath == null ) {
            if ( that.originalFilePath != null ) {
                return false;
            }
        } else if ( !this.originalFilePath.equals( that.originalFilePath ) ) {
            return false;
        }

        return true;
    }

    /**
     * @return the VDB description (can be empty)
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the VDB name (can be empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the external file path of the VDB (can be empty)
     */
    public String getOriginalFilePath() {
        return this.originalFilePath;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash( this.name, this.description, this.originalFilePath, super.hashCode() );
    }

    /**
     * @param newDescription
     *        the new description (can be empty)
     */
    public void setDescription( final String newDescription ) {
        this.description = newDescription;
    }

    /**
     * @param newName
     *        the new VDB name (can be empty)
     */
    public void setName( final String newName ) {
        this.name = newName;
    }

    /**
     * @param newOriginalFilePath
     *        the new VDB external file path (can be empty)
     */
    public void setOriginalFilePath( final String newOriginalFilePath ) {
        this.originalFilePath = newOriginalFilePath;
    }

}
