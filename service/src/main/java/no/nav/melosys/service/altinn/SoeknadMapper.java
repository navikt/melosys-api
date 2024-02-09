package no.nav.melosys.service.altinn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.mottatteopplysninger.data.Utenlandsoppdraget;
import no.nav.melosys.domain.mottatteopplysninger.data.*;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidPaaLand;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.*;
import no.nav.melosys.domain.kodeverk.Flyvningstyper;
import no.nav.melosys.domain.kodeverk.Innretningstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.soknad_altinn.*;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso2FraEuEosLandnavn;

public final class SoeknadMapper {
    private SoeknadMapper() {
        throw new UnsupportedOperationException();
    }

    static Soeknad lagSoeknad(MedlemskapArbeidEOSM søknad) {
        final Innhold innhold = søknad.getInnhold();
        final Soeknad soeknad = new Soeknad();
        soeknad.soeknadsland = hentsoeknadsland(innhold);
        soeknad.periode = lagPeriode(innhold);
        soeknad.personOpplysninger = lagPersonopplysninger(innhold);
        lagArbeidssteder(innhold, soeknad);
        soeknad.setLoennOgGodtgjoerelse(lagLoennOgGodtgjoerelse(innhold.getMidlertidigUtsendt()));
        final var virksomhetIUtlandet = innhold.getMidlertidigUtsendt().getVirksomhetIUtlandet();
        if (virksomhetIUtlandet != null
            && StringUtils.isNotBlank(virksomhetIUtlandet.getNavn())) {
            soeknad.foretakUtland.add(lagUtenlandskVirksomhet(virksomhetIUtlandet));
        }
        soeknad.juridiskArbeidsgiverNorge = lagJuridiskArbeidsgiverNorge(innhold.getArbeidsgiver());
        soeknad.setUtenlandsoppdraget(lagUtenlandsoppdraget(innhold.getMidlertidigUtsendt().getUtenlandsoppdraget()));
        soeknad.setArbeidssituasjonOgOevrig(lagArbeidssituasjonOgOevrig(innhold.getMidlertidigUtsendt()));
        return soeknad;
    }

    private static Soeknadsland hentsoeknadsland(Innhold innhold) {
        List<String> landFraAltinn = List.of(innhold.getMidlertidigUtsendt().getArbeidsland());
        return new Soeknadsland(landFraAltinn, false);
    }

    private static Periode lagPeriode(Innhold innhold) {
        Tidsrom utsendingsperiode = innhold.getMidlertidigUtsendt().getUtenlandsoppdraget()
            .getPeriodeUtland();
        return lagPeriode(utsendingsperiode);
    }

    private static Periode lagPeriode(Tidsrom tidsrom) {
        LocalDate periodeFra = xmlCalTilLocalDate(tidsrom.getPeriodeFra());
        LocalDate periodeTil = xmlCalTilLocalDate(tidsrom.getPeriodeTil());
        return new Periode(periodeFra, periodeTil);
    }

    private static OpplysningerOmBrukeren lagPersonopplysninger(Innhold innhold) {
        OpplysningerOmBrukeren personopplysninger = new OpplysningerOmBrukeren();
        if (innhold.getArbeidstaker().getUtenlandskIDnummer() != null) {
            personopplysninger.getUtenlandskIdent().add(lagUtenlandskIdent(innhold));
        }
        personopplysninger.setFoedestedOgLand(new FoedestedOgLand(
            innhold.getArbeidstaker().getFoedested(),
            innhold.getArbeidstaker().getFoedeland()
        ));
        personopplysninger.setMedfolgendeFamilie(hentMedfølgendeBarn(innhold));
        return personopplysninger;
    }

    private static UtenlandskIdent lagUtenlandskIdent(Innhold innhold) {
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.setIdent(innhold.getArbeidstaker().getUtenlandskIDnummer());
        utenlandskIdent.setLandkode(innhold.getMidlertidigUtsendt().getArbeidsland());
        return utenlandskIdent;
    }

    private static List<MedfolgendeFamilie> hentMedfølgendeBarn(Innhold innhold) {
        Barn barn = innhold.getArbeidstaker().getBarn();
        List<MedfolgendeFamilie> medfølgendeBarn = new ArrayList<>();

        if (barn != null && barn.getBarnet() != null) {
            medfølgendeBarn = barn.getBarnet().stream()
                .map(mapBarnTilMedfølgendeFamilie)
                .collect(Collectors.toList());
        }
        return medfølgendeBarn;
    }

    private static void lagArbeidssteder(Innhold innhold, Soeknad soeknad) {
        final Arbeidssted arbeidssted = innhold.getMidlertidigUtsendt().getArbeidssted();
        final ArbeidsstedType arbeidsstedType = ArbeidsstedType.valueOf(arbeidssted.getTypeArbeidssted().toUpperCase());

        switch (arbeidsstedType) {
            case LAND -> soeknad.arbeidPaaLand = lagArbeidPåLand(arbeidssted.getArbeidPaaLand());
            case OFFSHORE -> soeknad.maritimtArbeid = lagOffshoreArbeid(arbeidssted.getOffshoreEnheter());
            case SKIPSFART -> soeknad.maritimtArbeid = lagArbeidPåSkip(arbeidssted.getSkipListe());
            case LUFTFART -> soeknad.luftfartBaser = lagLuftfartBaser(arbeidssted.getLuftfart());
            default -> throw new IllegalArgumentException("ArbeidsstedType ikke støttet: " + arbeidsstedType);
        }
    }

    private static ArbeidPaaLand lagArbeidPåLand(no.nav.melosys.soknad_altinn.ArbeidPaaLand arbeidPaaLandAltinn) {
        ArbeidPaaLand arbeidPaaLand = new ArbeidPaaLand();
        arbeidPaaLand.setFysiskeArbeidssteder(arbeidPaaLandAltinn.getFysiskeArbeidssteder().getFysiskArbeidssted()
            .stream().map(SoeknadMapper::lagFysiskArbeidssted).collect(Collectors.toList()));
        arbeidPaaLand.setErFastArbeidssted(arbeidPaaLandAltinn.isFastArbeidssted());
        arbeidPaaLand.setErHjemmekontor(arbeidPaaLandAltinn.isHjemmekontor());
        return arbeidPaaLand;
    }

    private static FysiskArbeidssted lagFysiskArbeidssted(no.nav.melosys.soknad_altinn.FysiskArbeidssted fa) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.setVirksomhetNavn(fa.getFirmanavn());
        fysiskArbeidssted.setAdresse(new StrukturertAdresse(
            fa.getGatenavn(), null, fa.getPostkode(), fa.getBy(), fa.getRegion(), fa.getLand()
        ));
        return fysiskArbeidssted;
    }

    private static List<MaritimtArbeid> lagOffshoreArbeid(OffshoreEnheter offshoreEnheter) {
        return offshoreEnheter.getOffshoreEnhet().stream().map(SoeknadMapper::lagOffshoreArbeidssted)
            .collect(Collectors.toList());
    }

    private static MaritimtArbeid lagOffshoreArbeidssted(OffshoreEnheter.OffshoreEnhet offshoreEnhet) {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.setEnhetNavn(offshoreEnhet.getEnhetsNavn());
        maritimtArbeid.setInnretningstype(mapInnretningstyper(offshoreEnhet.getEnhetsType()));
        maritimtArbeid.setInnretningLandkode(offshoreEnhet.getSokkelLand());
        return maritimtArbeid;
    }

    private static Innretningstyper mapInnretningstyper(OffshoreEnhetstype offshoreEnhetstype) {
        return switch (offshoreEnhetstype) {
            case BORESKIP -> Innretningstyper.BORESKIP;
            case PLATTFORM, ANNEN_STASJONAER_ENHET -> Innretningstyper.PLATTFORM;
        };
    }

    private static List<MaritimtArbeid> lagArbeidPåSkip(SkipListe skipListe) {
        return skipListe.getSkip().stream().map(SoeknadMapper::lagArbeidsstedPåSkip).collect(Collectors.toList());
    }

    private static MaritimtArbeid lagArbeidsstedPåSkip(SkipListe.Skip skip) {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.setEnhetNavn(skip.getSkipNavn());
        maritimtArbeid.setFartsomradeKode(Fartsomrader.valueOf(skip.getFartsomraade().toString().toUpperCase()));
        maritimtArbeid.setFlaggLandkode(skip.getFlaggland());
        maritimtArbeid.setTerritorialfarvannLandkode(skip.getTerritorialEllerHavnLand());
        return maritimtArbeid;
    }

    private static List<LuftfartBase> lagLuftfartBaser(Luftfart luftfart) {
        return luftfart.getLuftfartBaser().getLuftfartbase().stream().map(SoeknadMapper::lagLuftfartBase)
            .collect(Collectors.toList());
    }

    private static LuftfartBase lagLuftfartBase(Luftfartbaser.Luftfartbase luftfartbase) {
        return new LuftfartBase(
            luftfartbase.getHjemmebaseNavn(),
            luftfartbase.getHjemmebaseLand(),
            Flyvningstyper.valueOf(luftfartbase.getTypeFlyvninger().toString().toUpperCase())
        );
    }

    private static LoennOgGodtgjoerelse lagLoennOgGodtgjoerelse(MidlertidigUtsendt midlertidigUtsendt) {
        no.nav.melosys.soknad_altinn.LoennOgGodtgjoerelse loennOgGodtgjoerelseAltinn =
            midlertidigUtsendt.getLoennOgGodtgjoerelse();
        return new LoennOgGodtgjoerelse(
            loennOgGodtgjoerelseAltinn.isNorskArbgUtbetalerLoenn(),
            midlertidigUtsendt.getUtenlandsoppdraget().isErArbeidstakerAnsattHelePerioden(),
            loennOgGodtgjoerelseAltinn.isUtlArbgUtbetalerLoenn(),
            loennOgGodtgjoerelseAltinn.isUtlArbTilhorerSammeKonsern(),
            hentNorskBruttoLoennPerMnd(loennOgGodtgjoerelseAltinn),
            loennOgGodtgjoerelseAltinn.getLoennUtlArbg(),
            loennOgGodtgjoerelseAltinn.isMottarNaturalytelser(),
            loennOgGodtgjoerelseAltinn.getSamletVerdiNaturalytelser(),
            loennOgGodtgjoerelseAltinn.isBetalerArbeidsgiveravgift(),
            loennOgGodtgjoerelseAltinn.isTrukketTrygdeavgift()
        );
    }

    private static BigDecimal hentNorskBruttoLoennPerMnd(
        no.nav.melosys.soknad_altinn.LoennOgGodtgjoerelse loennOgGodtgjoerelseAltinn) {
        // Hvis norskArbgUtbetalerLoenn == true OG utlArbgUtbetalerLoenn == false kan man oppleve å motta både
        // <loennNorskArbg>0</loennNorskArbg> og <loennNorskArbg></loennNorskArbg> fra Altinn
        boolean harAltinnEtProblem = loennOgGodtgjoerelseAltinn.isNorskArbgUtbetalerLoenn()
            && !loennOgGodtgjoerelseAltinn.isUtlArbgUtbetalerLoenn()
            && BigDecimal.ZERO.equals(loennOgGodtgjoerelseAltinn.getLoennNorskArbg());
        return harAltinnEtProblem ? null : loennOgGodtgjoerelseAltinn.getLoennNorskArbg();
    }

    private static ForetakUtland lagUtenlandskVirksomhet(VirksomhetIUtlandet virksomhetIUtlandet) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.setNavn(virksomhetIUtlandet.getNavn());
        foretakUtland.setOrgnr(virksomhetIUtlandet.getRegistreringsnummer());
        final PostadresseUtland postadresseUtland = virksomhetIUtlandet.getAdresse();
        foretakUtland.getAdresse().setGatenavn(postadresseUtland.getGatenavn());
        foretakUtland.getAdresse().setPostnummer(postadresseUtland.getPostkode());
        foretakUtland.getAdresse().setPoststed(postadresseUtland.getBy());
        foretakUtland.getAdresse().setRegion(postadresseUtland.getRegion());
        foretakUtland.getAdresse().setLandkode(tilIso2FraEuEosLandnavn(postadresseUtland.getLand()));
        return foretakUtland;
    }

    private static JuridiskArbeidsgiverNorge lagJuridiskArbeidsgiverNorge(Arbeidsgiver arbeidsgiver) {
        JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        if (arbeidsgiver != null) {
            juridiskArbeidsgiverNorge.setErOffentligVirksomhet(arbeidsgiver.isOffentligVirksomhet());

            if (!arbeidsgiver.isOffentligVirksomhet() && arbeidsgiver.getSamletVirksomhetINorge() != null) {
                SamletVirksomhetINorge samletVirksomhetINorge = arbeidsgiver.getSamletVirksomhetINorge();
                juridiskArbeidsgiverNorge.setAntallAnsatte(samletVirksomhetINorge.getAntallAnsatte().intValue());
                juridiskArbeidsgiverNorge.setAntallAdmAnsatte(samletVirksomhetINorge.getAntallAdministrativeAnsatteINorge().intValue());
                juridiskArbeidsgiverNorge.setAntallUtsendte(samletVirksomhetINorge.getAntallUtsendte().intValue());
                juridiskArbeidsgiverNorge.setAndelOmsetningINorge(new BigDecimal(samletVirksomhetINorge.getAndelOmsetningINorge()));
                juridiskArbeidsgiverNorge.setAndelOppdragINorge(new BigDecimal(samletVirksomhetINorge.getAndelOppdragINorge()));
                juridiskArbeidsgiverNorge.setAndelKontrakterINorge(new BigDecimal(samletVirksomhetINorge.getAndelKontrakterInngaasINorge()));
                juridiskArbeidsgiverNorge.setAndelRekruttertINorge(new BigDecimal(samletVirksomhetINorge.getAndelRekrutteresINorge()));
                juridiskArbeidsgiverNorge.setEkstraArbeidsgivere(List.of(arbeidsgiver.getVirksomhetsnummer()));
            }
        }
        return juridiskArbeidsgiverNorge;
    }

    private static Utenlandsoppdraget lagUtenlandsoppdraget(no.nav.melosys.soknad_altinn.Utenlandsoppdraget utenlandsoppdraget) {
        Periode samletUtsendingsperiode = new Periode();
        if (Boolean.TRUE.equals(utenlandsoppdraget.isErstatterTidligereUtsendte())
            && utenlandsoppdraget.getSamletUtsendingsperiode() != null) {
            samletUtsendingsperiode = lagPeriode(utenlandsoppdraget.getSamletUtsendingsperiode());
        }

        return new Utenlandsoppdraget(
            samletUtsendingsperiode,
            utenlandsoppdraget.isSendesUtOppdragIUtlandet(),
            utenlandsoppdraget.isAnsattEtterOppdraget(),
            utenlandsoppdraget.isAnsattForOppdragIUtlandet(),
            utenlandsoppdraget.isDrattPaaEgetInitiativ(),
            utenlandsoppdraget.isErstatterTidligereUtsendte()
        );
    }

    private static ArbeidssituasjonOgOevrig lagArbeidssituasjonOgOevrig(MidlertidigUtsendt midlertidigUtsendt) {
        ArbeidssituasjonOgOevrig arbeidssituasjonOgOevrig = new ArbeidssituasjonOgOevrig();
        arbeidssituasjonOgOevrig.setHarLoennetArbeidMinstEnMndFoerUtsending(midlertidigUtsendt.isLoennetArbeidMinstEnMnd());
        arbeidssituasjonOgOevrig.setBeskrivelseArbeidSisteMnd(midlertidigUtsendt.getBeskrivArbeidSisteMnd());
        arbeidssituasjonOgOevrig.setHarAndreArbeidsgivereIUtsendingsperioden(midlertidigUtsendt.isAndreArbeidsgivereIUtsendingsperioden());
        arbeidssituasjonOgOevrig.setBeskrivelseAnnetArbeid(midlertidigUtsendt.getBeskrivelseAnnetArbeid());
        arbeidssituasjonOgOevrig.setErSkattepliktig(midlertidigUtsendt.isSkattepliktig());
        arbeidssituasjonOgOevrig.setMottarYtelserNorge(midlertidigUtsendt.isMottaYtelserNorge());
        arbeidssituasjonOgOevrig.setMottarYtelserUtlandet(midlertidigUtsendt.isMottaYtelserUtlandet());
        return arbeidssituasjonOgOevrig;
    }

    private static LocalDate xmlCalTilLocalDate(XMLGregorianCalendar calendar) {
        return calendar == null ? null : LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }

    private static final Function<Barnet, MedfolgendeFamilie> mapBarnTilMedfølgendeFamilie
        = barnet -> MedfolgendeFamilie.tilBarnFraFnrOgNavn(barnet.getFoedselsnummer(), barnet.getNavn());
}
