package no.nav.melosys.tjenester.gui.dto.view;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.tjenester.gui.dto.ArbeidsforholdDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import no.nav.melosys.tjenester.gui.dto.PersonDto;

public class ArbeidsforholdView {

    private PersonDto person;

    private List<ArbeidsforholdDto> arbeidsforhold = new ArrayList<>();

    private List<OrganisasjonDto> organisasjoner = new ArrayList<>();

    public ArbeidsforholdView() {
    }

    public PersonDto getPerson() {
        return person;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }

    public List<ArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<ArbeidsforholdDto> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<OrganisasjonDto> getOrganisasjoner() {
        return organisasjoner;
    }

    public void setOrganisasjoner(List<OrganisasjonDto> organisasjoner) {
        this.organisasjoner = organisasjoner;
    }
}