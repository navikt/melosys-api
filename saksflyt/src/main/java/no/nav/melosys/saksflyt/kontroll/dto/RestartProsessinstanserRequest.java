package no.nav.melosys.saksflyt.kontroll.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestartProsessinstanserRequest {
    private final List<UUID> uuids;

    @JsonCreator
    public RestartProsessinstanserRequest(@JsonProperty("uuids") List<UUID> uuids) {
        this.uuids = uuids;
    }

    public List<UUID> getUuids() {
        return uuids;
    }
}
