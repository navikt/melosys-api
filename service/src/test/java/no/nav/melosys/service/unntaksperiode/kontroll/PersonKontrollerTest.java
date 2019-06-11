package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PersonKontrollerTest {

    @Test
    public void personDød_personErDød_registrerTreff() {
        KontrollData kontrollData = hentKontrollData();
        kontrollData.personDokument.dødsdato = LocalDate.now();
        assertThat(PersonKontroller.personDød(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.PERSON_DOD);
    }

    @Test
    public void personDød_personLever_ingenTreff() {
        assertThat(PersonKontroller.personDød(hentKontrollData())).isNull();
    }

    @Test
    public void personBosattINorge_bosattINorge_registrerTreff() {
        KontrollData kontrollData = hentKontrollData();
        kontrollData.personDokument.dødsdato = LocalDate.now();
        kontrollData.personDokument.bostedsadresse = new Bostedsadresse();
        kontrollData.personDokument.bostedsadresse.setLand(new Land(Land.NORGE));
        assertThat(PersonKontroller.personBosattINorge(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.BOSATT_I_NORGE);
    }

    @Test
    public void personBosattINorge_ikkeBosattINorge_ingenTreff() {
        KontrollData kontrollData = hentKontrollData();
        kontrollData.personDokument.dødsdato = LocalDate.now();
        kontrollData.personDokument.bostedsadresse = new Bostedsadresse();
        kontrollData.personDokument.bostedsadresse.setLand(new Land(Land.SVEITS));
        assertThat(PersonKontroller.personBosattINorge(kontrollData)).isNull();
    }

    @Test
    public void personBosattINorge_ingenBostedsadresse_ingenTreff() {
        assertThat(PersonKontroller.personBosattINorge(hentKontrollData())).isNull();
    }

    private KontrollData hentKontrollData() {
        return new KontrollData(null, new PersonDokument(), null, null);
    }

}