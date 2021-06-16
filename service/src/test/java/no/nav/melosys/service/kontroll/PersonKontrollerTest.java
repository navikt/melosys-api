package no.nav.melosys.service.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.Persondata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PersonKontrollerTest {

    private Persondata persondata = new PersonDokument();
    private BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    @Test
    public void personDød_personErDød_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        assertThat(PersonKontroller.personDød(personDokument)).isTrue();
    }

    @Test
    public void personDød_ingenDødsdato_false() {
        assertThat(PersonKontroller.personDød(new PersonDokument())).isFalse();
    }

    @Test
    public void personBosattINorge_bosattINorge_true() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.NORGE));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isTrue();
    }

    @Test
    public void personBosattINorge_ikkeBosattINorge_false() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land(Land.SVEITS));
        assertThat(PersonKontroller.personBosattINorge(personDokument)).isFalse();
    }

    @Test
    public void personBosattINorge_ingenBostedsadresse_false() {
        assertThat(PersonKontroller.personBosattINorge(new PersonDokument())).isFalse();
    }

    @Test
    public void harBostedsadresse_oppgittBostedsadresseIBehandlingsgrunnnlag_true() {
        Bosted bosted = behandlingsgrunnlagData.bosted;
        bosted.oppgittAdresse.setGatenavn("gate");
        bosted.oppgittAdresse.setLandkode("SE");

        assertThat(PersonKontroller.harRegistrertBostedsadresse(persondata, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    public void harRegistrertBostedsadresse_oppgittBostedsadresseITPS_true() {
        persondata.getBostedsadresse().getGateadresse().setGatenavn("gate 123");
        persondata.getBostedsadresse().getLand().setKode("SWE");

        assertThat(PersonKontroller.harRegistrertBostedsadresse(persondata, behandlingsgrunnlagData)).isTrue();
    }

    @Test
    public void harRegistrertBostedsadresse_ikkeOppgittBostedsadresseITPSEllerSøknad_false() {
        assertThat(PersonKontroller.harRegistrertBostedsadresse(persondata, behandlingsgrunnlagData)).isFalse();
    }
}
