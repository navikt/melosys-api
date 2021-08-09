package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.exception.TekniskException;

public class ArbeidsforholdResponse {
    private final List<Arbeidsforhold> arbeidsforhold;

    public ArbeidsforholdResponse(List<Arbeidsforhold> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<Arbeidsforhold> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public String tilSaksopplysning() {
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
        String arbeidsforholdId; // Arbeidsforhold-id fra opplysningspliktig

        @JsonProperty
        Integer navArbeidsforholdId; // Arbeidsforhold-id i AAREG

        @JsonProperty
        Ansettelsesperiode ansettelsesperiode;

        @JsonProperty
        String type;

        @JsonProperty
        Arbeidstaker arbeidstaker;

        @JsonProperty
        List<Arbeidsavtale> arbeidsavtaler;

        @JsonProperty
        List<PermisjonPermittering> permisjonPermitteringer;

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

        Periode getPeriode() {
            if (ansettelsesperiode == null) return null;
            return ansettelsesperiode.periode;
        }

        String getOpplysningspliktigtype() {
            if (opplysningspliktig == null) return null;
            if (opplysningspliktig.type == null) return null;
            return opplysningspliktig.type.toUpperCase();
        }

        public Arbeidstaker getArbeidstaker() {
            return arbeidstaker;
        }

        public List<Arbeidsavtale> getArbeidsavtaler() {
            return arbeidsavtaler;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public List<PermisjonPermittering> getPermisjonPermitteringer() {
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

    public static record AntallTimerForTimeloennet(
        BigDecimal antallTimer,
        Periode periode,
        String rapporteringsperiode) {
    }

    public static record Opplysningspliktig(
        String type, //  Organisasjon eller Person
        String organisasjonsnummer // Ligger i respons fra service, men ikke i swagger doc.
    ) {
    }

    public static record Arbeidsgiver(
        String type,
        String organisasjonsnummer) {
    }

    public static record Utenlandsopphold(String landkode, Periode periode, String rapporteringsperiode) {
    }

    public static record Ansettelsesperiode(Periode periode) {
    }

    public static record Arbeidstaker(String type, String offentligIdent, String aktoerId) {
    }

    public static record Periode(LocalDate fom, LocalDate tom) {
    }

    public static record PermisjonPermittering(
        Periode periode,
        String permisjonPermitteringId,
        BigDecimal prosent,
        String type, // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PermisjonsOgPermitteringsBeskrivelse
        String varslingskode
    ) {
    }

    public static record Arbeidsavtale (
        String type, // Type for arbeidsavtale - Forenklet, Frilanser, Maritim, Ordinaer
        String arbeidstidsordning, // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Arbeidstidsordninger
        String yrke, // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Yrker
        String ansettelsesform, // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/AnsettelsesformAareg
        BigDecimal stillingsprosent,
        BigDecimal beregnetAntallTimerPrUke,
        Periode gyldighetsperiode,
        LocalDate sistStillingsendring,
        LocalDate sistLoennsendring,
        BigDecimal antallTimerPrUke
    ) {}
}
