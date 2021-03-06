/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.komodo.rest.json;

import java.io.IOException;
import org.komodo.rest.RestProperty;
import org.komodo.rest.Messages;
import org.komodo.utils.StringUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 *
 */
public class RestPropertySerializer extends TypeAdapter<RestProperty> implements JsonConstants {

    protected boolean isComplete(final RestProperty property) {
        return !StringUtils.isBlank(property.getName());
    }

    @Override
    public RestProperty read(JsonReader in) throws IOException {

        String propName = null;
        String propValue = null;

        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            if (RestProperty.NAME_LABEL.equals(name))
                propName = in.nextString();
            else if (RestProperty.VALUE_LABEL.equals(name))
                propValue = in.nextString();
        }

        in.endObject();

        RestProperty property = new RestProperty(propName, propValue);
        if (!isComplete(property))
            throw new IOException(Messages.getString(Messages.Error.INCOMPLETE_JSON, RestProperty.class.getSimpleName()));

        return property;
    }

    @Override
    public void write(JsonWriter out, RestProperty value) throws IOException {
        out.beginObject();

        out.name(RestProperty.NAME_LABEL);
        out.value(value.getName());

        out.name(RestProperty.VALUE_LABEL);
        out.value(value.getValue());

        out.endObject();
    }
}
