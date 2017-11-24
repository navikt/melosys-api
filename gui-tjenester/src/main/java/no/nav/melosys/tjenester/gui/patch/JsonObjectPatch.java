package no.nav.melosys.tjenester.gui.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import java.io.IOException;

/**
 * JSON Object Patch implementasjon for PATCH (RFC 6902)
 */
public class JsonObjectPatch implements ObjectPatch {

    private final ObjectMapper objectMapper;

    private final JsonNode patchNode;

    public JsonObjectPatch(ObjectMapper objectMapper, JsonNode patchNode) {
        this.objectMapper = objectMapper;
        this.patchNode = patchNode;
    }

    @Override
    public <T> T apply(T target) throws ObjectPatchException {

        JsonNode source = objectMapper.valueToTree(target);

        JsonNode result;
        try {
            JsonPatch patch = JsonPatch.fromJson(patchNode);
            result = patch.apply(source);
        } catch (JsonPatchException e) {
            throw new ObjectPatchException(e);
        } catch (IOException e) {
            throw new ObjectPatchException(e);
        }

        ObjectReader reader = objectMapper.readerForUpdating(target);

        try {
            return reader.readValue(result);
        } catch (IOException e) {
            throw new ObjectPatchException(e);
        }
    }

}