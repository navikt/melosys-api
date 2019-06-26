package no.nav.melosys.service.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PersonKontrollerTest {

    @Test
    public void personDød_personErDød_registrerTreff() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        assertThat(PersonKontroller.personDød(personDokument)).isTrue();
    }

    @Test
    public void personDød_ingenDødsdato_ingenTreff() {
        assertThat(PersonKontroller.personDød(new PersonDokument())).isFalse();
    }

    @Test
    public void personBosattINorge_bosattINorge_registrerTreff() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land(Land.NORGE));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isTrue();
    }

    @Test
    public void personBosattINorge_ikkeBosattINorge_ingenTreff() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land(Land.SVEITS));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isFalse();
    }

    @Test
    public void personBosattINorge_ingenBostedsadresse_ingenTreff() {
        assertThat(PersonKontroller.personBosattINorge(new PersonDokument())).isFalse();
    }
}