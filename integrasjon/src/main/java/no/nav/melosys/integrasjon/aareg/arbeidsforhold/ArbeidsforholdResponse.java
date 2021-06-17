package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.arbeidsforhold.PermisjonOgPermittering;
import no.nav.melosys.exception.TekniskException;

public class ArbeidsforholdResponse {
    private final Arbeidsforhold[] arbeidsforhold;

    public ArbeidsforholdResponse(Arbeidsforhold[] arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public Arbeidsforhold[] getArbeidsforhold() {
        return arbeidsforhold;
    }

    public String getJsonDocument() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            // serialize to json or find out if we want to convert to xml doc?
            return objectMapper.writeValueAsString(arbeidsforhold);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Kunne ikke konvertere arbeidsforhold til json string", e);
        }
    }

    public static class Arbeidsforhold {
        private Map<String, Object> unknownProperties = new HashMap<>();

        private Integer navArbeidsforholdId;
        private String type;
        private Arbeidstaker arbeidstaker;
        private List<Arbeidsavtaler> arbeidsavtaler;

        @JsonIgnore // TODO: legg til innhold i PermisjonPermitteringer
        private List<PermisjonPermitteringer> permisjonPermitteringer = List.of();

        public Arbeidstaker getArbeidstaker() {
            return arbeidstaker;
        }

        public Integer getNavArbeidsforholdId() {
            return navArbeidsforholdId;
        }

        public List<Arbeidsavtaler> getArbeidsavtaler() {
            return arbeidsavtaler;
        }

        public List<PermisjonPermitteringer> getPermisjonPermitteringer() {
            return permisjonPermitteringer;
        }

        @JsonAnySetter
        void setUnknownProperty(String key, Object value) {
            unknownProperties.put(key, value);
        }

        @JsonAnyGetter
        Object getUnknownProperty(String key) {
            return unknownProperties.get(key);
        }

        public String getType() {
            return type;
        }
    }

    public static class Arbeidstaker {
        @JsonProperty
        String type;

        @JsonProperty
        String offentligIdent;

        @JsonProperty
        String aktoerId;
    }

    public static class PermisjonPermitteringer {

    }

    public static class Arbeidsgiver {
    }

    public static class Opplysningspliktig {
    }

    public static class Arbeidsavtaler {
        @JsonProperty
        String type;

        @JsonProperty
        String arbeidstidsordning;

        @JsonProperty
        String yrke;

        @JsonProperty
        String stillingsprosent;

        @JsonProperty
        BigDecimal beregnetAntallTimerPrUke;
    }
}
