package no.nav.melosys.domain.dokument.medlemskap;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket PeriodetypeMedl.
 */
public enum Periodetype implements KodeverkEnum<Periodetype> {
    PMMEDSKP("Periode med medlemskap"),
    PUMEDSKP("Periode uten medlemskap"),
    E500INFO("Utenlandsk id");

    private String navn;

    Periodetype(String navn) {
        this.navn = navn;
    }

    @JsonValue
    public Map<String, String> getJson() {
        Map<String, String> periodetypeMap = new HashMap<>();
        periodetypeMap.put("kode", name());
        periodetypeMap.put("term", navn);
        return periodetypeMap;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
