package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.*;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.persondata.PdlObjectFactory.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontaktadresseOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversettVegadresse() {
        var kontaktadressePDL = new Kontaktadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            null,
            null,
            new Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            metadata()
        );
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"))).thenReturn("Bergen");

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(kontaktadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(kontaktadresse.strukturertAdresse().getGatenavn()).isEqualTo("Kirkegata");
        assertThat(kontaktadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("12 B");
        assertThat(kontaktadresse.strukturertAdresse().getPostnummer()).isEqualTo("1234");
        assertThat(kontaktadresse.strukturertAdresse().getPoststed()).isEqualTo("Bergen");
        assertThat(kontaktadresse.strukturertAdresse().getRegion()).isNull();
        assertThat(kontaktadresse.strukturertAdresse().getLandkode()).isEqualTo("NO");
        assertThat(kontaktadresse.registrertDato()).isEqualTo(kontaktadressePDL.metadata().datoSistRegistrert());
        assertThat(kontaktadresse.master()).isEqualTo("PDL");
        assertThat(kontaktadresse.kilde()).isEqualTo("Dolly");
    }

    @Test
    void oversettPostadresseIFrittFormat() {
        var kontaktadressePDL = new Kontaktadresse(
            null,
            null,
            null,
            null,
            new PostadresseIFrittFormat("1", "2", "3", "1234"),
            null,
            null,
            null,
            metadata()
        );
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"))).thenReturn("Enby");

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje1()).isEqualTo("1");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje2()).isEqualTo("2");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje3()).isEqualTo("3");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje4()).isNull();
        assertThat(kontaktadresse.semistrukturertAdresse().postnr()).isEqualTo("1234");
        assertThat(kontaktadresse.semistrukturertAdresse().poststed()).isEqualTo("Enby");
        assertThat(kontaktadresse.semistrukturertAdresse().landkode()).isEqualTo("NO");
    }

    @Test
    void oversettUtenlandskAdresse() {
        var kontaktadressePDL = new Kontaktadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            new UtenlandskAdresse(
                "adressenavnNummer",
                "bygningEtasjeLeilighet",
                "P.O.Box 1234 Place",
                "SE-12345",
                "Haworth",
                "Yorkshire",
                "SWE"
            ),
            null,
            null,
            metadata()
        );

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.strukturertAdresse().getGatenavn()).isEqualTo("adressenavnNummer");
        assertThat(kontaktadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("bygningEtasjeLeilighet");
        assertThat(kontaktadresse.strukturertAdresse().getPostboks()).isEqualTo("P.O.Box 1234 Place");
        assertThat(kontaktadresse.strukturertAdresse().getPostnummer()).isEqualTo("SE-12345");
        assertThat(kontaktadresse.strukturertAdresse().getPoststed()).isEqualTo("Haworth");
        assertThat(kontaktadresse.strukturertAdresse().getRegion()).isEqualTo("Yorkshire");
        assertThat(kontaktadresse.strukturertAdresse().getLandkode()).isEqualTo("SE");
    }

    @Test
    void oversettUtenlandskAdresseIFrittFormat() {
        var kontaktadressePDL = new Kontaktadresse(
            null,
            null,
            null,
            null,
            null,
            null,
            new UtenlandskAdresseIFrittFormat(
                "1",
                "2",
                "3",
                "postkode",
                "by",
                "FRA"
            ),
            null,
            metadata()
        );

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje1()).isEqualTo("1");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje2()).isEqualTo("2");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje3()).isEqualTo("3");
        assertThat(kontaktadresse.semistrukturertAdresse().adresselinje4()).isNull();
        assertThat(kontaktadresse.semistrukturertAdresse().postnr()).isEqualTo("postkode");
        assertThat(kontaktadresse.semistrukturertAdresse().poststed()).isEqualTo("by");
        assertThat(kontaktadresse.semistrukturertAdresse().landkode()).isEqualTo("FR");
    }
}
