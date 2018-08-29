package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;

@JsonPropertyOrder({"person", "arbeidsforhold", "organisasjoner", "medlemskap", "inntekt", "sakOgBehandling"})
public class SaksopplysningerDto {

    private PersonDto person;

    private ArbeidsforholdDokument arbeidsforhold;

    private List<OrganisasjonDokument> organisasjoner;

    private MedlemskapDokument medlemskap;

    private InntektDokument inntekt;

    private SobSakDokument sakOgBehandling;

    public SaksopplysningerDto() {
        // Frontend ønsker å motta et objekt, selv når saksopplysninger ikke finnes.
        this.person = new PersonDto();
        this.arbeidsforhold = new ArbeidsforholdDokument();
        this.organisasjoner = new ArrayList<>();
        this.medlemskap = new MedlemskapDokument();
        this.inntekt = new InntektDokument();
        this.sakOgBehandling = new SobSakDokument();
    }

    public PersonDto getPerson() {
        return person;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }

    public ArbeidsforholdDokument getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(ArbeidsforholdDokument arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<OrganisasjonDokument> getOrganisasjoner() {
        return organisasjoner;
    }

    public void setOrganisasjoner(List<OrganisasjonDokument> organisasjoner) {
        this.organisasjoner = organisasjoner;
    }

    public MedlemskapDokument getMedlemskap() {
        return medlemskap;
    }

    public void setMedlemskap(MedlemskapDokument medlemskap) {
        this.medlemskap = medlemskap;
    }

    public InntektDokument getInntekt() {
        return inntekt;
    }

    public void setInntekt(InntektDokument inntekt) {
        this.inntekt = inntekt;
    }

    public SobSakDokument getSakOgBehandling() {
        return sakOgBehandling;
    }

    public void setSakOgBehandling(SobSakDokument sakOgBehandling) {
        this.sakOgBehandling = sakOgBehandling;
    }
}
