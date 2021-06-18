package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.exception.TekniskException;

public class ArbeidsforholdResponse {
    private final List<Arbeidsforhold> arbeidsforhold;

    public ArbeidsforholdResponse(Arbeidsforhold[] arbeidsforhold) {
        this.arbeidsforhold = Arrays.asList(arbeidsforhold);
    }

    public List<Arbeidsforhold> getArbeidsforhold() {
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
        private final Map<String, Object> unknownProperties = new HashMap<>();

        @JsonProperty
        String arbeidsforholdId;

        @JsonProperty
        Integer navArbeidsforholdId;

        @JsonProperty
        Ansettelsesperiode ansettelsesperiode;

        @JsonProperty
        String type;

        @JsonProperty
        Arbeidstaker arbeidstaker;

        @JsonProperty
        List<Arbeidsavtaler> arbeidsavtaler;

        @JsonProperty
        List<PermisjonPermitteringer> permisjonPermitteringer;

        @JsonProperty
        List<Utenlandsopphold> utenlandsopphold;

        @JsonProperty
        Arbeidsgiver arbeidsgiver;

        @JsonProperty
        Opplysningspliktig opplysningspliktig;

        @JsonProperty
        Boolean innrapportertEtterAOrdningen;

        @JsonProperty
        String registrert;

        @JsonProperty
        String sistBekreftet;

        @JsonProperty
        List<AntallTimerForTimeloennet> antallTimerForTimeloennet;

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

    public static class AntallTimerForTimeloennet {
        @JsonProperty
        BigDecimal antallTimer;

        @JsonProperty
        Periode periode;

        @JsonProperty
        String rapporteringsperiode;
    }

    public static class Opplysningspliktig {
        @JsonProperty
        String type;
    }

    public static class Arbeidsgiver {
        @JsonProperty
        String type;
    }

    public static class Utenlandsopphold {
        @JsonProperty
        String landkode;

        @JsonProperty
        Periode periode;

        @JsonProperty
        String rapporteringsperiode;
    }

    public static class Ansettelsesperiode {
        @JsonProperty
        Periode periode;
    }

    public static class Arbeidstaker {
        @JsonProperty
        String type;

        @JsonProperty
        String offentligIdent;

        @JsonProperty
        String aktoerId;
    }

    public static class Periode {
        @JsonProperty
        String fom;

        @JsonProperty
        String tom;

        LocalDate getFom() {
            return LocalDate.parse(fom);
        }

        LocalDate getTom() {
            return LocalDate.parse(tom);
        }
    }

    public static class PermisjonPermitteringer {
        @JsonProperty
        Periode periode;

        @JsonProperty
        String permisjonPermitteringId;

        @JsonProperty
        BigDecimal prosent;

        @JsonProperty
        String type;

        @JsonProperty
        String varslingskode;
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
