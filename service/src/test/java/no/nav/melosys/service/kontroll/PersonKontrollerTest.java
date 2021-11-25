package no.nav.melosys.service.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PersonKontrollerTest {

    private final PersonDokument personDokument = new PersonDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    @Test
    void personDød_personErDød_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        assertThat(PersonKontroller.erPersonDød(personDokument)).isTrue();
    }

    @Test
    void personDød_ingenDødsdato_false() {
        assertThat(PersonKontroller.erPersonDød(new PersonDokument())).isFalse();
    }

    @Test
    void personBosattINorge_bosattINorge_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.NORGE));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isTrue();
    }

    @Test
    void personBosattINorge_ikkeBosattINorge_false() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.SVEITS));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isFalse();
    }

    @Test
    void personBosattINorge_ingenBostedsadresse_false() {
        assertThat(PersonKontroller.personBosattINorge(new PersonDokument())).isFalse();
    }

    @Test
    void harBostedsadresse_oppgittBostedsadresseIBehandlingsgrunnnlag_true() {
        Bosted bosted = behandlingsgrunnlagData.bosted;
        bosted.oppgittAdresse.setGatenavn("gate");
        bosted.oppgittAdresse.setLandkode("SE");

        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    void harRegistrertBostedsadresse_oppgittBostedsadresseITPS_true() {
        personDokument.getBostedsadresse().getGateadresse().setGatenavn("gate 123");
        personDokument.getBostedsadresse().getLand().setKode("SWE");

        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    void harRegistrertBostedsadresse_ikkeOppgittBostedsadresseITPSEllerSøknad_false() {
        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isFalse();
    }
}
