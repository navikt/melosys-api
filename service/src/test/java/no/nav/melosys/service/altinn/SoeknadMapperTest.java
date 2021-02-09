package no.nav.melosys.service.altinn;

import java.math.BigDecimal;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.ArbeidsstedType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Innretningstyper;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.ObjectFactory;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.Flyvningstyper.INTERNASJONAL;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader.INNENRIKS;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader.UTENRIKS;
import static org.assertj.core.api.Assertions.assertThat;

class SoeknadMapperTest {
    @Test
    void testSøknadMapping() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.soeknadsland.landkoder).contains("FI");
        assertThat(soeknad.periode.getFom()).isEqualTo("2019-08-05");
        assertThat(soeknad.periode.getTom()).isEqualTo("2019-08-06");
        assertThat(soeknad.personOpplysninger.utenlandskIdent)
            .contains(new UtenlandskIdent("utenlandskIDnummer", "FI"));
        assertThat(soeknad.arbeidPaaLand.fysiskeArbeidssteder).isNotEmpty();
        assertThat(soeknad.arbeidPaaLand.erFastArbeidssted).isFalse();
        assertThat(soeknad.arbeidPaaLand.erHjemmekontor).isTrue();
        final FysiskArbeidssted fysiskArbeidssted = soeknad.arbeidPaaLand.fysiskeArbeidssteder.get(0);
        assertThat(fysiskArbeidssted.virksomhetNavn).isEqualTo("Firmaet");
        assertThat(fysiskArbeidssted.adresse.gatenavn).isEqualTo("Gaten 1");
        assertThat(fysiskArbeidssted.adresse.husnummer).isNull();
        assertThat(fysiskArbeidssted.adresse.postnummer).isEqualTo("1234");
        assertThat(fysiskArbeidssted.adresse.poststed).isEqualTo("Stedet");
        assertThat(fysiskArbeidssted.adresse.region).isEqualTo("Region");
        assertThat(fysiskArbeidssted.adresse.landkode).isEqualTo("BE");
        final var loennOgGodtgjoerelse = soeknad.loennOgGodtgjoerelse;
        assertThat(loennOgGodtgjoerelse.norskArbgUtbetalerLoenn).isTrue();
        assertThat(loennOgGodtgjoerelse.erArbeidstakerAnsattHelePerioden).isTrue();
        assertThat(loennOgGodtgjoerelse.utlArbgUtbetalerLoenn).isTrue();
        assertThat(loennOgGodtgjoerelse.utlArbTilhoererSammeKonsern).isFalse();
        assertThat(loennOgGodtgjoerelse.bruttoLoennPerMnd).isEqualTo(new BigDecimal("2000.00"));
        assertThat(loennOgGodtgjoerelse.bruttoLoennUtlandPerMnd).isEqualTo(new BigDecimal("1000.00"));
        assertThat(loennOgGodtgjoerelse.mottarNaturalytelser).isFalse();
        assertThat(loennOgGodtgjoerelse.samletVerdiNaturalytelser).isEqualTo(new BigDecimal("10000.50"));
        assertThat(loennOgGodtgjoerelse.erArbeidsgiveravgiftHelePerioden).isTrue();
        assertThat(loennOgGodtgjoerelse.erTrukketTrygdeavgift).isTrue();
        final var foretakUtland = soeknad.foretakUtland.get(0);
        assertThat(foretakUtland.navn).isEqualTo("Virskomheten i utlandet");
        assertThat(foretakUtland.orgnr).isEqualTo("XYZ123456789");
        assertThat(foretakUtland.adresse.gatenavn).isEqualTo("gatenavn med mer");
        assertThat(foretakUtland.adresse.poststed).isEqualTo("testbyen");
        assertThat(foretakUtland.adresse.postnummer).isEqualTo("UTLAND-1234");
        assertThat(foretakUtland.adresse.landkode).isEqualTo("BE");
    }

    @Test
    void testMappingOffshoreArbeidssteder() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.OFFSHORE.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.maritimtArbeid).isNotEmpty();
        final MaritimtArbeid maritimtArbeid = soeknad.maritimtArbeid.get(0);
        assertThat(maritimtArbeid.enhetNavn).isEqualTo("Landplattform");
        assertThat(maritimtArbeid.innretningstype).isEqualTo(Innretningstyper.PLATTFORM);
        assertThat(maritimtArbeid.innretningLandkode).isEqualTo("CH");
    }

    @Test
    void testMappingSkipsfart() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.SKIPSFART.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.maritimtArbeid).isNotEmpty();
        final MaritimtArbeid maritimtArbeidInnenriks = soeknad.maritimtArbeid.get(0);
        assertThat(maritimtArbeidInnenriks.enhetNavn).isEqualTo("abcd");
        assertThat(maritimtArbeidInnenriks.fartsomradeKode).isEqualTo(INNENRIKS);
        assertThat(maritimtArbeidInnenriks.territorialfarvann).isEqualTo("BG");
        final MaritimtArbeid maritimtArbeidUtenriks = soeknad.maritimtArbeid.get(1);
        assertThat(maritimtArbeidUtenriks.fartsomradeKode).isEqualTo(UTENRIKS);
        assertThat(maritimtArbeidUtenriks.flaggLandkode).isEqualTo("FO");
    }

    @Test
    void testMappingLuftfartBaser() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.LUFTFART.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.luftfartBaser).isNotEmpty();
        final LuftfartBase luftfartBase = soeknad.luftfartBaser.get(0);
        assertThat(luftfartBase.hjemmebaseNavn).isEqualTo("koti");
        assertThat(luftfartBase.hjemmebaseLand).isEqualTo("FI");
        assertThat(luftfartBase.typeFlyvninger).isEqualTo(INTERNASJONAL);
    }

    private MedlemskapArbeidEOSM parseSøknadXML() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        URL url = getClass().getClassLoader().getResource("altinn/NAV_MedlemskapArbeidEOS.xml");
        MedlemskapArbeidEOSM medlemskapArbeidEOSM =
            ((JAXBElement<MedlemskapArbeidEOSM>) jaxbContext.createUnmarshaller().unmarshal(url)).getValue();
        return medlemskapArbeidEOSM;
    }
}