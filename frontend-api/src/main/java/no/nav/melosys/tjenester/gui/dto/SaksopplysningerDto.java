package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.tjenester.gui.dto.dokument.PersonhistorikkDto;
import no.nav.melosys.tjenester.gui.dto.inntekt.InntektDto;

@JsonPropertyOrder({"person", "arbeidsforhold", "organisasjoner", "medlemskap", "inntekt", "sakOgBehandling", "sed"})
public class SaksopplysningerDto {

    private PersonDokument person;

    private PersonhistorikkDto personhistorikk;

    private ArbeidsforholdDokument arbeidsforhold;

    private List<OrganisasjonDokument> organisasjoner;

    private MedlemskapDokument medlemskap;

    private InntektDto inntekt;

    private SobSakDokument sakOgBehandling;

    private SedDokument sed;

    public SaksopplysningerDto() {
        // Frontend ønsker å motta et objekt, selv når saksopplysninger ikke finnes.
        this.person = new PersonDokument();
        this.personhistorikk = new PersonhistorikkDto();
        this.arbeidsforhold = new ArbeidsforholdDokument();
        this.organisasjoner = new ArrayList<>();
        this.medlemskap = new MedlemskapDokument();
        this.inntekt = new InntektDto();
        this.sakOgBehandling = new SobSakDokument();
        this.sed = new SedDokument();
    }

    public PersonDokument getPerson() {
        return person;
    }

    public void setPerson(PersonDokument person) {
        this.person = person;
    }

    public PersonhistorikkDto getPersonhistorikk() {
        return personhistorikk;
    }
    public void setPersonhistorikk(PersonhistorikkDto personhistorikk) {
        this.personhistorikk = personhistorikk;
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

    public InntektDto getInntekt() {
        return inntekt;
    }

    public void setInntekt(InntektDto inntekt) {
        this.inntekt = inntekt;
    }

    public SobSakDokument getSakOgBehandling() {
        return sakOgBehandling;
    }

    public void setSakOgBehandling(SobSakDokument sakOgBehandling) {
        this.sakOgBehandling = sakOgBehandling;
    }

    public SedDokument getSed() {
        return sed;
    }

    public void setSed(SedDokument sed) {
        this.sed = sed;
    }
}
