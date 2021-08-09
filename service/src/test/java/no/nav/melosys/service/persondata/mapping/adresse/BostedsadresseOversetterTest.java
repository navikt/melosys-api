package no.nav.melosys.service.persondata.mapping.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.persondata.PdlObjectFactory.lagNorskBostedsadresse;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagUtenlandskBostedsadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BostedsadresseOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversettVegadresse() {
        var bostedsadressePDL = lagNorskBostedsadresse();
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234"))).thenReturn("Bergen");

        final var bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService);

        assertThat(bostedsadresseOptional).isPresent();
        final var bostedsadresse = bostedsadresseOptional.get();
        assertThat(bostedsadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(bostedsadresse.gyldigFraOgMed()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(bostedsadresse.strukturertAdresse().getGatenavn()).isEqualTo("Kirkegata");
        assertThat(bostedsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("12 B");
        assertThat(bostedsadresse.strukturertAdresse().getTilleggsnavn()).isEqualTo("Storgården");
        assertThat(bostedsadresse.strukturertAdresse().getPostnummer()).isEqualTo("1234");
        assertThat(bostedsadresse.strukturertAdresse().getPoststed()).isEqualTo("Bergen");
        assertThat(bostedsadresse.strukturertAdresse().getRegion()).isNull();
        assertThat(bostedsadresse.strukturertAdresse().getLandkode()).isEqualTo("NO");
        assertThat(bostedsadresse.master()).isEqualTo("PDL");
        assertThat(bostedsadresse.kilde()).isEqualTo("Dolly");
    }

    @Test
    void oversettUtenlandskAdresse() {
        var bostedsadressePDL = lagUtenlandskBostedsadresse();

        final var bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService);

        assertThat(bostedsadresseOptional).isPresent();
        final var bostedsadresse = bostedsadresseOptional.get();
        assertThat(bostedsadresse.strukturertAdresse().getGatenavn()).isEqualTo("adressenavnNummer");
        assertThat(bostedsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isEqualTo("bygningEtasjeLeilighet");
        assertThat(bostedsadresse.strukturertAdresse().getPostboks()).isEqualTo("P.O.Box 1234 Place");
        assertThat(bostedsadresse.strukturertAdresse().getPostnummer()).isEqualTo("SE-12345");
        assertThat(bostedsadresse.strukturertAdresse().getPoststed()).isEqualTo("Haworth");
        assertThat(bostedsadresse.strukturertAdresse().getRegion()).isEqualTo("Yorkshire");
        assertThat(bostedsadresse.strukturertAdresse().getLandkode()).isEqualTo("SE");
    }
}
