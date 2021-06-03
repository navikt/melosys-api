package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
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
class BostedsadresseOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversettVegadresse() {
        var bostedsadressePDL = new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            new Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            null,
            null,
            null,
            metadata()
        );
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"), any())).thenReturn("Bergen");

        final var bostedsadresse = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService);

        assertThat(bostedsadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(bostedsadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(bostedsadresse.strukturertAdresse().gatenavn).isEqualTo("Kirkegata");
        assertThat(bostedsadresse.strukturertAdresse().husnummerEtasjeLeilighet).isEqualTo("12 B");
        assertThat(bostedsadresse.strukturertAdresse().tillegsnavn).isEqualTo("Storgården");
        assertThat(bostedsadresse.strukturertAdresse().postnummer).isEqualTo("1234");
        assertThat(bostedsadresse.strukturertAdresse().poststed).isEqualTo("Bergen");
        assertThat(bostedsadresse.strukturertAdresse().region).isNull();
        assertThat(bostedsadresse.strukturertAdresse().landkode).isEqualTo("NO");
        assertThat(bostedsadresse.master()).isEqualTo("PDL");
        assertThat(bostedsadresse.kilde()).isEqualTo("Dolly");
    }

    @Test
    void oversettUtenlandskAdresse() {
        var bostedsadressePDL = new Bostedsadresse(
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
            metadata()
        );

        final var bostedsadresse = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService);

        assertThat(bostedsadresse.strukturertAdresse().gatenavn).isEqualTo("adressenavnNummer");
        assertThat(bostedsadresse.strukturertAdresse().husnummerEtasjeLeilighet).isEqualTo("bygningEtasjeLeilighet");
        assertThat(bostedsadresse.strukturertAdresse().postboks).isEqualTo("P.O.Box 1234 Place");
        assertThat(bostedsadresse.strukturertAdresse().postnummer).isEqualTo("SE-12345");
        assertThat(bostedsadresse.strukturertAdresse().poststed).isEqualTo("Haworth");
        assertThat(bostedsadresse.strukturertAdresse().region).isEqualTo("Yorkshire");
        assertThat(bostedsadresse.strukturertAdresse().landkode).isEqualTo("SE");
    }
}
