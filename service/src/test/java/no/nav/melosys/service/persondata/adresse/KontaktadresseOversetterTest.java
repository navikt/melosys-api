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
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"), any())).thenReturn("Bergen");

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(kontaktadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(kontaktadresse.strukturertAdresse().gatenavn).isEqualTo("Kirkegata");
        assertThat(kontaktadresse.strukturertAdresse().husnummerEtasjeLeilighet).isEqualTo("12 B");
        assertThat(kontaktadresse.strukturertAdresse().postnummer).isEqualTo("1234");
        assertThat(kontaktadresse.strukturertAdresse().poststed).isEqualTo("Bergen");
        assertThat(kontaktadresse.strukturertAdresse().region).isNull();
        assertThat(kontaktadresse.strukturertAdresse().landkode).isEqualTo("NO");
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
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"), any())).thenReturn("Enby");

        final var kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService);

        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(1)).isEqualTo("1");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(2)).isEqualTo("2");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(3)).isEqualTo("3");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(4)).isEqualTo("1234 Enby");
        assertThat(kontaktadresse.ustrukturertAdresse().getLandkode()).isEqualTo("NO");
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

        assertThat(kontaktadresse.strukturertAdresse().gatenavn).isEqualTo("adressenavnNummer");
        assertThat(kontaktadresse.strukturertAdresse().husnummerEtasjeLeilighet).isEqualTo("bygningEtasjeLeilighet");
        assertThat(kontaktadresse.strukturertAdresse().postboks).isEqualTo("P.O.Box 1234 Place");
        assertThat(kontaktadresse.strukturertAdresse().postnummer).isEqualTo("SE-12345");
        assertThat(kontaktadresse.strukturertAdresse().poststed).isEqualTo("Haworth");
        assertThat(kontaktadresse.strukturertAdresse().region).isEqualTo("Yorkshire");
        assertThat(kontaktadresse.strukturertAdresse().landkode).isEqualTo("SE");
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

        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(1)).isEqualTo("1");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(2)).isEqualTo("2");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(3)).isEqualTo("3");
        assertThat(kontaktadresse.ustrukturertAdresse().getAdresselinje(4)).isEqualTo("postkode by");
        assertThat(kontaktadresse.ustrukturertAdresse().getLandkode()).isEqualTo("FR");
    }
}
