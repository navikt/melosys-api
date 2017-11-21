package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import java.util.List;

@JsonPropertyOrder({"person", "arbeidsforhold", "organisasjoner", "medlemskap", "inntekt" })
public class SaksopplysningerDto {

    private PersonDokument person;

    private List<ArbeidsforholdDokument> arbeidsforhold;

    private List<OrganisasjonDokument> organisasjoner;

    private List<MedlemskapDokument> medlemskap;

    private InntektDokument inntekt;

    public PersonDokument getPerson() {
        return person;
    }

    public void setPerson(PersonDokument person) {
        this.person = person;
    }

    public List<ArbeidsforholdDokument> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<ArbeidsforholdDokument> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<OrganisasjonDokument> getOrganisasjoner() {
        return organisasjoner;
    }

    public void setOrganisasjoner(List<OrganisasjonDokument> organisasjoner) {
        this.organisasjoner = organisasjoner;
    }

    public List<MedlemskapDokument> getMedlemskap() {
        return medlemskap;
    }

    public void setMedlemskap(List<MedlemskapDokument> medlemskap) {
        this.medlemskap = medlemskap;
    }

    public InntektDokument getInntekt() {
        return inntekt;
    }

    public void setInntekt(InntektDokument inntekt) {
        this.inntekt = inntekt;
    }
}
