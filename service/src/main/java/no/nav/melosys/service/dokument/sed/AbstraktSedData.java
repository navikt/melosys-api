package no.nav.melosys.service.dokument.sed;

import java.util.List;

import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

/**
 * AbstraktSedData vil inneholde all Melosys-informasjon som trengs for å kunne mappe til Nav-objekt i en SED.
 * Subklasser vil inneholde unik Melosys-informasjon for hver SED.
 */
public abstract class AbstraktSedData {

    private SoeknadDokument søknadDokument;
    private PersonDokument personDokument;

    private Bostedsadresse bostedsadresse;

    private List<Virksomhet> arbeidsgivendeVirksomheter;
    private List<Virksomhet> selvstendigeVirksomheter;
    private List<Arbeidssted> arbeidssteder; //Utenlandske arbeidsSTED
    private List<Virksomhet> utenlandskeVirksomheter;

    public SoeknadDokument getSøknadDokument() {
        return søknadDokument;
    }

    public void setSøknadDokument(SoeknadDokument søknadDokument) {
        this.søknadDokument = søknadDokument;
    }

    public PersonDokument getPersonDokument() {
        return personDokument;
    }

    public void setPersonDokument(PersonDokument personDokument) {
        this.personDokument = personDokument;
    }

    public Bostedsadresse getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    public List<Virksomhet> getArbeidsgivendeVirksomheter() {
        return arbeidsgivendeVirksomheter;
    }

    public void setArbeidsgivendeVirksomheter(
        List<Virksomhet> arbeidsgivendeVirksomheter) {
        this.arbeidsgivendeVirksomheter = arbeidsgivendeVirksomheter;
    }

    public List<Virksomhet> getSelvstendigeVirksomheter() {
        return selvstendigeVirksomheter;
    }

    public void setSelvstendigeVirksomheter(
        List<Virksomhet> selvstendigeVirksomheter) {
        this.selvstendigeVirksomheter = selvstendigeVirksomheter;
    }

    public List<Arbeidssted> getArbeidssteder() {
        return arbeidssteder;
    }

    public void setArbeidssteder(
        List<Arbeidssted> arbeidssteder) {
        this.arbeidssteder = arbeidssteder;
    }

    public List<Virksomhet> getUtenlandskeVirksomheter() {
        return utenlandskeVirksomheter;
    }

    public void setUtenlandskeVirksomheter(
        List<Virksomhet> utenlandskeVirksomheter) {
        this.utenlandskeVirksomheter = utenlandskeVirksomheter;
    }
}
