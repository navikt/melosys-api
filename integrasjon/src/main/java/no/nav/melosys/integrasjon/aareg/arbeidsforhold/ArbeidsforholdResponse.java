package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.TekniskException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArbeidsforholdResponse {
    private final Arbeidsforhold[] arbeidsforhold;

    public ArbeidsforholdResponse(Arbeidsforhold[] arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public Arbeidsforhold[] getArbeidsforhold() {
        return arbeidsforhold;
    }

    public Saksopplysning createSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();

        List<Arbeidsforhold> arbeidsforholdList = new ArrayList<>();

        // TODO: map from jsopn to ArbeidsforholdDokument
        for(Arbeidsforhold item : arbeidsforhold) {

        }

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        saksopplysning.setDokument(arbeidsforholdDokument);
        return saksopplysning;
    }

    public String getJsonDocument() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            // serialize to json or find out if we want to convert to xml doc?
            return objectMapper.writeValueAsString(arbeidsforhold);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Kunne ikke konvertere arbeidsforhold til json string");
        }
    }

    public static class Arbeidsforhold {
        private Map<String,Object> unknownProperties = new HashMap<>();

        private Integer navArbeidsforholdId;

        private Arbeidstaker arbeidstaker;

        public Arbeidstaker getArbeidstaker() {
            return arbeidstaker;
        }

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

    public static class Arbeidstaker {
        private String type;
        private String offentligIdent;
        private String aktoerId;

        public String getType() {
            return type;
        }

        public String getOffentligIdent() {
            return offentligIdent;
        }

        public String getAktoerId() {
            return aktoerId;
        }
    }

    public static class Arbeidsgiver {
    }

    public static class Opplysningspliktig {
    }
}
