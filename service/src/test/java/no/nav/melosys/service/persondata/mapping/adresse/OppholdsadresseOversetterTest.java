package no.nav.melosys.service.persondata.mapping.adresse;

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
        var oppholdsadressePDL = new Oppholdsadresse(
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
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"))).thenReturn("Bergen");

        final var oppholdsadresse = OppholdsadresseOversetter.oversett(oppholdsadressePDL, kodeverkService);

        assertThat(oppholdsadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(oppholdsadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(oppholdsadresse.strukturertAdresse().getGatenavn()).isEqualTo("Kirkegata");
        assertThat(oppholdsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("12 B");
        assertThat(oppholdsadresse.strukturertAdresse().getTilleggsnavn()).isEqualTo("Storgården");
        assertThat(oppholdsadresse.strukturertAdresse().getPostnummer()).isEqualTo("1234");
        assertThat(oppholdsadresse.strukturertAdresse().getPoststed()).isEqualTo("Bergen");
        assertThat(oppholdsadresse.strukturertAdresse().getRegion()).isNull();
        assertThat(oppholdsadresse.strukturertAdresse().getLandkode()).isEqualTo("NO");
        assertThat(oppholdsadresse.registrertDato()).isEqualTo(oppholdsadressePDL.metadata().datoSistRegistrert());
        assertThat(oppholdsadresse.master()).isEqualTo("PDL");
        assertThat(oppholdsadresse.kilde()).isEqualTo("Dolly");
    }

    @Test
    void oversettUtenlandskAdresse() {
        var oppholdsadressePDL = new Oppholdsadresse(
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

        final var oppholdsadresse = OppholdsadresseOversetter.oversett(oppholdsadressePDL, kodeverkService);

        assertThat(oppholdsadresse.strukturertAdresse().getGatenavn()).isEqualTo("adressenavnNummer");
        assertThat(oppholdsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("bygningEtasjeLeilighet");
        assertThat(oppholdsadresse.strukturertAdresse().getPostboks()).isEqualTo("P.O.Box 1234 Place");
        assertThat(oppholdsadresse.strukturertAdresse().getPostnummer()).isEqualTo("SE-12345");
        assertThat(oppholdsadresse.strukturertAdresse().getPoststed()).isEqualTo("Haworth");
        assertThat(oppholdsadresse.strukturertAdresse().getRegion()).isEqualTo("Yorkshire");
        assertThat(oppholdsadresse.strukturertAdresse().getLandkode()).isEqualTo("SE");
    }
}
