package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PersonReglerTest {

    private final PersonDokument personDokument = new PersonDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    @Test
    void personDød_personErDød_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        assertThat(PersonRegler.erPersonDød(personDokument)).isTrue();
    }

    @Test
    void personDød_ingenDødsdato_false() {
        assertThat(PersonRegler.erPersonDød(new PersonDokument())).isFalse();
    }

    @Test
    void personBosattINorge_bosattINorge_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.NORGE));
        assertThat(PersonRegler.personBosattINorge(personDokument)).isTrue();
    }

    @Test
    void personBosattINorge_ikkeBosattINorge_false() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.SVEITS));
        assertThat(PersonRegler.personBosattINorge(personDokument)).isFalse();
    }

    @Test
    void personBosattINorge_ingenBostedsadresse_false() {
        assertThat(PersonRegler.personBosattINorge(new PersonDokument())).isFalse();
    }
}
