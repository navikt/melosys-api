package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Oppholdsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;
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
class OppholdsadresseOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversettVegadresse() {
        var OppholdsadressePDL = new Oppholdsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            new Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            null,
            metadata()
        );
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"), any())).thenReturn("Bergen");

        final var Oppholdsadresse = OppholdsadresseOversetter.oversett(OppholdsadressePDL, kodeverkService);

        assertThat(Oppholdsadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(Oppholdsadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(Oppholdsadresse.strukturertAdresse().getGatenavn()).isEqualTo("Kirkegata");
        assertThat(Oppholdsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("12 B");
        assertThat(Oppholdsadresse.strukturertAdresse().getTillegsnavn()).isEqualTo("Storgården");
        assertThat(Oppholdsadresse.strukturertAdresse().getPostnummer()).isEqualTo("1234");
        assertThat(Oppholdsadresse.strukturertAdresse().getPoststed()).isEqualTo("Bergen");
        assertThat(Oppholdsadresse.strukturertAdresse().getRegion()).isNull();
        assertThat(Oppholdsadresse.strukturertAdresse().getLandkode()).isEqualTo("NO");
        assertThat(Oppholdsadresse.master()).isEqualTo("PDL");
        assertThat(Oppholdsadresse.kilde()).isEqualTo("Dolly");
    }

    @Test
    void oversettUtenlandskAdresse() {
        var OppholdsadressePDL = new Oppholdsadresse(
            null,
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

        final var Oppholdsadresse = OppholdsadresseOversetter.oversett(OppholdsadressePDL, kodeverkService);

        assertThat(Oppholdsadresse.strukturertAdresse().getGatenavn()).isEqualTo("adressenavnNummer");
        assertThat(Oppholdsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("bygningEtasjeLeilighet");
        assertThat(Oppholdsadresse.strukturertAdresse().getPostboks()).isEqualTo("P.O.Box 1234 Place");
        assertThat(Oppholdsadresse.strukturertAdresse().getPostnummer()).isEqualTo("SE-12345");
        assertThat(Oppholdsadresse.strukturertAdresse().getPoststed()).isEqualTo("Haworth");
        assertThat(Oppholdsadresse.strukturertAdresse().getRegion()).isEqualTo("Yorkshire");
        assertThat(Oppholdsadresse.strukturertAdresse().getLandkode()).isEqualTo("SE");
    }
}
