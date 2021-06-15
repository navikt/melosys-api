package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.model.Arbeidsforhold;

import java.util.ArrayList;
import java.util.List;

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

        // TODO: change name to avoid conflicting names
        List<no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold> arbeidsforholdList = new ArrayList<>();

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
            throw new TekniskException("Kunne konvertere arbeidsforhold til json string");
        }
    }
}
