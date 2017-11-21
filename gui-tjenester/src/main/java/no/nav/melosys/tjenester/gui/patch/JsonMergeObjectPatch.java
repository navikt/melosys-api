package no.nav.melosys.tjenester.gui.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

import java.io.IOException;

/**
 * JSON Merge Patch implementasjon for PATCH (RFC 7396)
 */
public class JsonMergeObjectPatch implements ObjectPatch {

    private final ObjectMapper objectMapper;

    private final JsonNode patchNode;

    public JsonMergeObjectPatch(ObjectMapper objectMapper, JsonNode patchNode) {
        this.objectMapper = objectMapper;
        this.patchNode = patchNode;
    }

    @Override
    public <T> T apply(T target) throws ObjectPatchException {

        JsonNode source = objectMapper.valueToTree(target);

        JsonNode result;
        try {
            JsonMergePatch patch = JsonMergePatch.fromJson(patchNode);
            result = patch.apply(source);
        } catch (JsonPatchException e) {
            throw new ObjectPatchException(e);
        }

        ObjectReader reader = objectMapper.readerForUpdating(target);

        try {
            return reader.readValue(result);
        } catch (JsonProcessingException e) {
            throw new ObjectPatchException(e);
        } catch (IOException e) {
            throw new ObjectPatchException(e);
        }
    }
}
