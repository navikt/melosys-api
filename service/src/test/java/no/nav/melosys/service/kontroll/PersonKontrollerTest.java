package no.nav.melosys.service.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Bosted;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PersonKontrollerTest {

    private PersonDokument personDokument = new PersonDokument();
    private BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    @Test
    public void personDød_personErDød_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        assertThat(PersonKontroller.personDød(personDokument)).isTrue();
    }

    @Test
    public void personDød_ingenDødsdato_false() {
        assertThat(PersonKontroller.personDød(new PersonDokument())).isFalse();
    }

    @Test
    public void personBosattINorge_bosattINorge_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land(Land.NORGE));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isTrue();
    }

    @Test
    public void personBosattINorge_ikkeBosattINorge_false() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land(Land.SVEITS));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isFalse();
    }

    @Test
    public void personBosattINorge_ingenBostedsadresse_false() {
        assertThat(PersonKontroller.personBosattINorge(new PersonDokument())).isFalse();
    }

    @Test
    public void harBostedsadresse_oppgittBostedsadresseIBehandlingsgrunnnlag_true() {
        Bosted bosted = behandlingsgrunnlagData.bosted;
        bosted.oppgittAdresse.gatenavn = "gate";
        bosted.oppgittAdresse.landkode = "SE";

        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    public void harRegistrertBostedsadresse_oppgittBostedsadresseITPS_true() {
        personDokument.bostedsadresse.getGateadresse().setGatenavn("gate 123");
        personDokument.bostedsadresse.getLand().setKode("SWE");

        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    public void harRegistrertBostedsadresse_ikkeOppgittBostedsadresseITPSEllerSøknad_false() {
        assertThat(PersonKontroller.harRegistrertBostedsadresse(personDokument, behandlingsgrunnlagData)).isFalse();
    }
}