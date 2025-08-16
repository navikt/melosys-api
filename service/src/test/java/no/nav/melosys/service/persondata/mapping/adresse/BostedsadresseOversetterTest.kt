package no.nav.melosys.service.persondata.mapping.adresse;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.melosys.service.persondata.PdlObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BostedsadresseOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void finnOgOversett() {
        Bostedsadresse ugyldigBostedsadresse = lagUgyldigBostedsadresse();
        Bostedsadresse gyldigBostedsadresse = lagNorskBostedsadresse();
        List<Bostedsadresse> addresser = List.of(ugyldigBostedsadresse, gyldigBostedsadresse);

        no.nav.melosys.domain.person.adresse.Bostedsadresse result = BostedsadresseOversetter.finnGjeldende(addresser, kodeverkService);

        assertThat(result.strukturertAdresse().getGatenavn()).isEqualTo(gyldigBostedsadresse.vegadresse().adressenavn());
    }

    @Test
    void oversettVegadresse() {
        var bostedsadressePDL = lagNorskBostedsadresse();
        when(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234")).thenReturn("Bergen");

        final var bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService);

        assertThat(bostedsadresseOptional).isPresent();
        final var bostedsadresse = bostedsadresseOptional.get();
        assertThat(bostedsadresse.coAdressenavn()).isEqualTo("Kari Hansen");
        assertThat(bostedsadresse.gyldigFraOgMed()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(bostedsadresse.gyldigTilOgMed()).isEqualTo(LocalDate.parse("2020-05-05"));
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
    void oversettMatrikkeladresse() {
        Bostedsadresse bostedsadresseMedMatrikkelAdresse = lagBostedsadresseMedMatrikkelAdresse();
        when(kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), any())).thenReturn("Asker");

        final var bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadresseMedMatrikkelAdresse, kodeverkService);

        assertThat(bostedsadresseOptional).isPresent();
        final var bostedsadresse = bostedsadresseOptional.get();
        assertThat(bostedsadresse.strukturertAdresse().getGatenavn()).isNull();
        assertThat(bostedsadresse.strukturertAdresse().getHusnummerEtasjeLeilighet()).isNull();
        assertThat(bostedsadresse.strukturertAdresse().getTilleggsnavn()).isEqualTo("tilleggsnavn");
        assertThat(bostedsadresse.strukturertAdresse().getPostnummer()).isEqualTo("4321");
        assertThat(bostedsadresse.strukturertAdresse().getPoststed()).isEqualTo("Asker");
        assertThat(bostedsadresse.strukturertAdresse().getRegion()).isNull();
        assertThat(bostedsadresse.strukturertAdresse().getLandkode()).isEqualTo("NO");
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
