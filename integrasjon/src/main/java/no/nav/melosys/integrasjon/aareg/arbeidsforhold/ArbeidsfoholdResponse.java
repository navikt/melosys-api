package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class ArbeidsfoholdResponse {
    private Map<String,Object>  unknownProperties = new HashMap<>();

    private Integer navArbeidsforholdId;

    public Integer getNavArbeidsforholdId() {
        return navArbeidsforholdId;
    }

    @JsonAnySetter
    void setUnknownProperty(String key, Object value) {
        unknownProperties.put(key,value);
    }

    @JsonAnyGetter
    Object getUnknownProperty(String key) {
        return unknownProperties.get(key);
    }

}
