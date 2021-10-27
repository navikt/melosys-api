package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.service.dokument.DokgenTestData;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonMedHistorikkOversetterTest {
    @Mock
    private KodeverkService kodeverkService;

    @Test
    void lagHistorikkFraTpsData() {
        no.nav.melosys.domain.dokument.person.Sivilstand sivilstand = spy(no.nav.melosys.domain.dokument.person.Sivilstand.class);
        when(sivilstand.getKode()).thenReturn("GLAD");
        when(kodeverkService.dekod(FellesKodeverk.PERSONSTATUSER, "ABNR")).thenReturn("Aktivt BOSTNR");
        when(kodeverkService.dekod(FellesKodeverk.SIVILSTANDER, "GLAD")).thenReturn("Gift, lever adskilt");
        final var personDokumentFraTps = lagPersonDokument(sivilstand);
        final var personMedHistorikk = PersonMedHistorikkOversetter.lagHistorikkFraTpsData(personDokumentFraTps, kodeverkService);

        assertThat(personMedHistorikk.navn()).isEqualTo(new Navn("Kari", "Mellom", "Nordmann"));
        assertThat(personMedHistorikk.kjønn()).isEqualTo(KjoennType.KVINNE);
        assertThat(personMedHistorikk.fødsel()).isEqualTo(new Foedsel(personDokumentFraTps.getFødselsdato(), 1989, null, null));
        assertThat(personMedHistorikk.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("123456789"));
        assertThat(personMedHistorikk.bostedsadresser().iterator().next()).usingRecursiveComparison().isEqualTo(
            new Bostedsadresse(personDokumentFraTps.getBostedsadresse().tilStrukturertAdresse(), null, null, null, Master.TPS.name(),
                               Master.TPS.name(), false));
        assertThat(personMedHistorikk.kontaktadresser()).isNotEmpty();
        assertThat(personMedHistorikk.oppholdsadresser()).isEmpty();
        assertThat(personMedHistorikk.folkeregisterpersonstatus()).isEqualTo(
            new Folkeregisterpersonstatus(Personstatuser.UDEFINERT, "Aktivt BOSTNR"));
        assertThat(personMedHistorikk.sivilstand()).containsExactly(
            new Sivilstand(Sivilstandstype.UDEFINERT, "Gift, lever adskilt", "", LocalDate.parse("2019-08-07"), null, Master.TPS.name(),
                           Master.TPS.name(), false));
        assertThat(personMedHistorikk.statsborgerskap()).containsExactly(
            new Statsborgerskap("NOR", null, LocalDate.parse("1989-08-07"), null, Master.TPS.name(), Master.TPS.name(), false));
        assertThat(personMedHistorikk.dødsfall()).isEqualTo(new Doedsfall(personDokumentFraTps.getDødsdato()));
    }

    private static PersonDokument lagPersonDokument(no.nav.melosys.domain.dokument.person.Sivilstand sivilstand) {
        PersonDokument person = new PersonDokument();
        person.setKjønn(new KjoennsType("K"));
        person.setFornavn("Kari");
        person.setMellomnavn("Mellom");
        person.setEtternavn("Nordmann");
        person.setFødselsdato(LocalDate.parse("1989-08-07"));
        person.setFnr("123456789");
        person.setBostedsadresse(BrevDataTestUtils.lagBostedsadresse());
        person.setPostadresse(DokgenTestData.lagAdresse());
        person.setPersonstatus(Personstatus.ABNR);
        person.setSivilstand(sivilstand);
        person.setSivilstandGyldighetsperiodeFom(LocalDate.parse("2019-08-07"));
        person.setStatsborgerskap(new Land(Land.NORGE));
        person.setStatsborgerskapDato(LocalDate.parse("1989-08-07"));
        person.setDødsdato(LocalDate.parse("2089-08-07"));
        return person;
    }
}
