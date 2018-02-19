package no.nav.melosys.tjenester.gui.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * {@code MessageBodyReader} som konverterer en JSON Object Patch fra requesten til en {@link JsonObjectPatch}
 */
public class JsonPatchReader implements MessageBodyReader<ObjectPatch> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ObjectPatch.class && MediaType.APPLICATION_JSON_PATCH_JSON_TYPE.isCompatible(mediaType);
    }

    @Override
    public ObjectPatch readFrom(Class<ObjectPatch> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

        JsonNode patch = OBJECT_MAPPER.readTree(entityStream);

        return new JsonObjectPatch(OBJECT_MAPPER, patch);
    }
}
