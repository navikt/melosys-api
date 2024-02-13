package no.nav.melosys.service.altinn;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidsstedType;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidssituasjonOgOevrig;
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

        assertThat(soeknad.soeknadsland.getLandkoder()).contains("FI");
        assertThat(soeknad.periode.getFom()).isEqualTo("2019-08-05");
        assertThat(soeknad.periode.getTom()).isEqualTo("2019-08-06");
        assertThat(soeknad.personOpplysninger.getUtenlandskIdent())
            .contains(new UtenlandskIdent("utenlandskIDnummer", "FI"));
        assertThat(soeknad.personOpplysninger.getFoedestedOgLand().getFoedested()).isEqualTo("Oslo");
        assertThat(soeknad.personOpplysninger.getFoedestedOgLand().getFoedeland()).isEqualTo("NO");
        assertThat(soeknad.arbeidPaaLand.getFysiskeArbeidssteder()).isNotEmpty();
        assertThat(soeknad.arbeidPaaLand.getErFastArbeidssted()).isFalse();
        assertThat(soeknad.arbeidPaaLand.getErHjemmekontor()).isTrue();
        final FysiskArbeidssted fysiskArbeidssted = soeknad.arbeidPaaLand.getFysiskeArbeidssteder().get(0);
        assertThat(fysiskArbeidssted.getVirksomhetNavn()).isEqualTo("Firmaet");
        assertThat(fysiskArbeidssted.getAdresse().getGatenavn()).isEqualTo("Gaten 1");
        assertThat(fysiskArbeidssted.getAdresse().getHusnummerEtasjeLeilighet()).isNull();
        assertThat(fysiskArbeidssted.getAdresse().getPostnummer()).isEqualTo("1234");
        assertThat(fysiskArbeidssted.getAdresse().getPoststed()).isEqualTo("Stedet");
        assertThat(fysiskArbeidssted.getAdresse().getRegion()).isEqualTo("Region");
        assertThat(fysiskArbeidssted.getAdresse().getLandkode()).isEqualTo("BE");
        final var loennOgGodtgjoerelse = soeknad.getLoennOgGodtgjoerelse();
        assertThat(loennOgGodtgjoerelse.getNorskArbgUtbetalerLoenn()).isTrue();
        assertThat(loennOgGodtgjoerelse.getErArbeidstakerAnsattHelePerioden()).isTrue();
        assertThat(loennOgGodtgjoerelse.getUtlArbgUtbetalerLoenn()).isTrue();
        assertThat(loennOgGodtgjoerelse.getUtlArbTilhoererSammeKonsern()).isFalse();
        assertThat(loennOgGodtgjoerelse.getBruttoLoennPerMnd()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(loennOgGodtgjoerelse.getBruttoLoennUtlandPerMnd()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(loennOgGodtgjoerelse.getMottarNaturalytelser()).isFalse();
        assertThat(loennOgGodtgjoerelse.getSamletVerdiNaturalytelser()).isEqualTo(new BigDecimal("10000.50"));
        assertThat(loennOgGodtgjoerelse.getErArbeidsgiveravgiftHelePerioden()).isTrue();
        assertThat(loennOgGodtgjoerelse.getErTrukketTrygdeavgift()).isTrue();
        final var foretakUtland = soeknad.foretakUtland.get(0);
        assertThat(foretakUtland.getNavn()).isEqualTo("Virskomheten i utlandet");
        assertThat(foretakUtland.getOrgnr()).isEqualTo("XYZ123456789");
        assertThat(foretakUtland.getAdresse().getGatenavn()).isEqualTo("gatenavn med mer");
        assertThat(foretakUtland.getAdresse().getPoststed()).isEqualTo("testbyen");
        assertThat(foretakUtland.getAdresse().getPostnummer()).isEqualTo("UTLAND-1234");
        assertThat(foretakUtland.getAdresse().getLandkode()).isEqualTo("BE");
        final var utenlandsoppdraget = soeknad.getUtenlandsoppdraget();
        assertThat(utenlandsoppdraget.getErErstatningTidligereUtsendte()).isFalse();
        assertThat(utenlandsoppdraget.getSamletUtsendingsperiode()).isNotNull();
        assertThat(utenlandsoppdraget.getSamletUtsendingsperiode().getFom()).isNull();
        assertThat(utenlandsoppdraget.getErUtsendelseForOppdragIUtlandet()).isFalse();
        assertThat(utenlandsoppdraget.getErFortsattAnsattEtterOppdraget()).isNull();
        assertThat(utenlandsoppdraget.getErAnsattForOppdragIUtlandet()).isFalse();
        assertThat(utenlandsoppdraget.getErDrattPaaEgetInitiativ()).isFalse();
    }

    @Test
    void testMappingArbeidsgiver() throws JAXBException {
        final MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getArbeidsgiver().setOffentligVirksomhet(true);

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        var juridiskArbeidsgiverNorge = soeknad.juridiskArbeidsgiverNorge;
        assertThat(juridiskArbeidsgiverNorge.getErOffentligVirksomhet()).isEqualTo(true);
        assertThat(juridiskArbeidsgiverNorge.getAntallAnsatte()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAntallAdmAnsatte()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAntallUtsendte()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAndelOmsetningINorge()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAndelOppdragINorge()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAndelKontrakterINorge()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getAndelRekruttertINorge()).isNull();
        assertThat(juridiskArbeidsgiverNorge.getEkstraArbeidsgivere()).isEmpty();

        medlemskapArbeidEOSM.getInnhold().getArbeidsgiver().setOffentligVirksomhet(false);

        soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        juridiskArbeidsgiverNorge = soeknad.juridiskArbeidsgiverNorge;
        assertThat(juridiskArbeidsgiverNorge.getErOffentligVirksomhet()).isEqualTo(false);
        assertThat(juridiskArbeidsgiverNorge.getAntallAnsatte()).isEqualTo(100);
        assertThat(juridiskArbeidsgiverNorge.getAntallAdmAnsatte()).isEqualTo(10);
        assertThat(juridiskArbeidsgiverNorge.getAntallUtsendte()).isEqualTo(10);
        assertThat(juridiskArbeidsgiverNorge.getAndelOmsetningINorge()).isEqualTo(new BigDecimal(90));
        assertThat(juridiskArbeidsgiverNorge.getAndelOppdragINorge()).isEqualTo(new BigDecimal(90));
        assertThat(juridiskArbeidsgiverNorge.getAndelKontrakterINorge()).isEqualTo(new BigDecimal(90));
        assertThat(juridiskArbeidsgiverNorge.getAndelRekruttertINorge()).isEqualTo(new BigDecimal(90));
        assertThat(juridiskArbeidsgiverNorge.getEkstraArbeidsgivere()).contains("910825569");
    }

    @Test
    void testMappingOffshoreArbeidssteder() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.OFFSHORE.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.maritimtArbeid).isNotEmpty();
        final MaritimtArbeid maritimtArbeid = soeknad.maritimtArbeid.get(0);
        assertThat(maritimtArbeid.getEnhetNavn()).isEqualTo("Landplattform");
        assertThat(maritimtArbeid.getInnretningstype()).isEqualTo(Innretningstyper.PLATTFORM);
        assertThat(maritimtArbeid.getInnretningLandkode()).isEqualTo("CH");
    }

    @Test
    void testMappingSkipsfart() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.SKIPSFART.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.maritimtArbeid).isNotEmpty();
        final MaritimtArbeid maritimtArbeidInnenriks = soeknad.maritimtArbeid.get(0);
        assertThat(maritimtArbeidInnenriks.getEnhetNavn()).isEqualTo("abcd");
        assertThat(maritimtArbeidInnenriks.getFartsomradeKode()).isEqualTo(INNENRIKS);
        assertThat(maritimtArbeidInnenriks.getTerritorialfarvannLandkode()).isEqualTo("BG");
        final MaritimtArbeid maritimtArbeidUtenriks = soeknad.maritimtArbeid.get(1);
        assertThat(maritimtArbeidUtenriks.getFartsomradeKode()).isEqualTo(UTENRIKS);
        assertThat(maritimtArbeidUtenriks.getFlaggLandkode()).isEqualTo("FO");
    }

    @Test
    void testMappingLuftfartBaser() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getArbeidssted()
            .setTypeArbeidssted(ArbeidsstedType.LUFTFART.toString());

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.luftfartBaser).isNotEmpty();
        final LuftfartBase luftfartBase = soeknad.luftfartBaser.get(0);
        assertThat(luftfartBase.getHjemmebaseNavn()).isEqualTo("koti");
        assertThat(luftfartBase.getHjemmebaseLand()).isEqualTo("FI");
        assertThat(luftfartBase.getTypeFlyvninger()).isEqualTo(INTERNASJONAL);
    }

    @Test
    void testMappingSamletUtsendingsperiode() throws JAXBException {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();
        medlemskapArbeidEOSM.getInnhold().getMidlertidigUtsendt().getUtenlandsoppdraget()
            .setErstatterTidligereUtsendte(Boolean.TRUE);

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        final var utenlandsoppdraget = soeknad.getUtenlandsoppdraget();
        assertThat(utenlandsoppdraget.getErErstatningTidligereUtsendte()).isTrue();
        assertThat(utenlandsoppdraget.getSamletUtsendingsperiode())
            .extracting(Periode::getFom, Periode::getTom)
            .containsExactly(
                LocalDate.of(2019, 8, 1),
                LocalDate.of(2019, 8, 6)
            );
    }

    @Test
    void testArbeidssituasjonOgOevrig() throws JAXBException {
        final MedlemskapArbeidEOSM medlemskapArbeidEOSM = parseSøknadXML();

        final Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        final ArbeidssituasjonOgOevrig arbeidssituasjonOgOevrig = soeknad.getArbeidssituasjonOgOevrig();
        assertThat(arbeidssituasjonOgOevrig.getHarLoennetArbeidMinstEnMndFoerUtsending()).isTrue();
        assertThat(arbeidssituasjonOgOevrig.getBeskrivelseArbeidSisteMnd()).isEqualTo("Arbeid siste mnd");
        assertThat(arbeidssituasjonOgOevrig.getHarAndreArbeidsgivereIUtsendingsperioden()).isFalse();
        assertThat(arbeidssituasjonOgOevrig.getBeskrivelseAnnetArbeid()).isEqualTo("Annet arbeid");
        assertThat(arbeidssituasjonOgOevrig.getErSkattepliktig()).isTrue();
        assertThat(arbeidssituasjonOgOevrig.getMottarYtelserNorge()).isFalse();
        assertThat(arbeidssituasjonOgOevrig.getMottarYtelserUtlandet()).isFalse();
    }

    private MedlemskapArbeidEOSM parseSøknadXML() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        URL url = getClass().getClassLoader().getResource("altinn/NAV_MedlemskapArbeidEOS.xml");
        MedlemskapArbeidEOSM medlemskapArbeidEOSM =
            ((JAXBElement<MedlemskapArbeidEOSM>) jaxbContext.createUnmarshaller().unmarshal(url)).getValue();
        return medlemskapArbeidEOSM;
    }
}
