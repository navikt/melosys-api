package no.nav.melosys.service.altinn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.ArbeidPaaLand;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Flyvningstyper;
import no.nav.melosys.domain.kodeverk.Innretningstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.soknad_altinn.*;

public final class SoeknadMapper {
    private SoeknadMapper() {
        throw new UnsupportedOperationException();
    }

    static Soeknad lagSoeknad(MedlemskapArbeidEOSM søknad) {
        final Innhold innhold = søknad.getInnhold();
        final Soeknad soeknad = new Soeknad();
        soeknad.soeknadsland = hentsoeknadsland(innhold);
        soeknad.periode = lagPeriode(innhold);
        if (innhold.getArbeidstaker().getUtenlandskIDnummer() != null) {
            soeknad.personOpplysninger.utenlandskIdent.add(lagUtenlandskIdent(innhold));
        }
        soeknad.personOpplysninger.medfolgendeFamilie = hentMedfølgendeBarn(innhold);
        lagArbeidssteder(innhold, soeknad);
        soeknad.loennOgGodtgjoerelse = lagLoennOgGodtgjoerelse(innhold.getMidlertidigUtsendt());
        soeknad.juridiskArbeidsgiverNorge = lagJuridiskArbeidsgiverNorge(innhold.getArbeidsgiver());
        return soeknad;
    }

    private static Soeknadsland hentsoeknadsland(Innhold innhold) {
        Collection<String> landFraAltinn = List.of(innhold.getMidlertidigUtsendt().getArbeidsland());
        return Soeknadsland.av(landFraAltinn);
    }

    private static Periode lagPeriode(Innhold innhold) {
        Tidsrom utsendingsperiode = innhold.getMidlertidigUtsendt().getUtenlandsoppdraget()
            .getPeriodeUtland();
        LocalDate periodeFra = xmlCalTilLocalDate(utsendingsperiode.getPeriodeFra());
        LocalDate periodeTil = xmlCalTilLocalDate(utsendingsperiode.getPeriodeTil());
        return new Periode(periodeFra, periodeTil);
    }

    private static UtenlandskIdent lagUtenlandskIdent(Innhold innhold) {
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.ident = innhold.getArbeidstaker().getUtenlandskIDnummer();
        utenlandskIdent.landkode = innhold.getMidlertidigUtsendt().getArbeidsland();
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
            case LAND:
                soeknad.arbeidPaaLand = lagArbeidPåLand(arbeidssted.getArbeidPaaLand());
                break;
            case OFFSHORE:
                soeknad.maritimtArbeid = lagOffshoreArbeid(arbeidssted.getOffshoreEnheter());
                break;
            case SKIPSFART:
                soeknad.maritimtArbeid = lagArbeidPåSkip(arbeidssted.getSkipListe());
                break;
            case LUFTFART:
                soeknad.luftfartBaser = lagLuftfartBaser(arbeidssted.getLuftfart());
                break;
            default:
                throw new IllegalArgumentException("ArbeidsstedType ikke støttet: " + arbeidsstedType);
        }
    }

    private static ArbeidPaaLand lagArbeidPåLand(no.nav.melosys.soknad_altinn.ArbeidPaaLand arbeidPaaLandAltinn) {
        ArbeidPaaLand arbeidPaaLand = new ArbeidPaaLand();
        arbeidPaaLand.fysiskeArbeidssteder = arbeidPaaLandAltinn.getFysiskeArbeidssteder().getFysiskArbeidssted()
            .stream().map(SoeknadMapper::lagFysiskArbeidssted).collect(Collectors.toList());
        arbeidPaaLand.erFastArbeidssted = arbeidPaaLandAltinn.isFastArbeidssted();
        arbeidPaaLand.erHjemmekontor = arbeidPaaLandAltinn.isHjemmekontor();
        return arbeidPaaLand;
    }

    private static FysiskArbeidssted lagFysiskArbeidssted(no.nav.melosys.soknad_altinn.FysiskArbeidssted fa) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.virksomhetNavn = fa.getFirmanavn();
        fysiskArbeidssted.adresse = new StrukturertAdresse(
            fa.getGatenavn(), null, fa.getPostkode(), fa.getBy(), fa.getRegion(), fa.getLand()
        );
        return fysiskArbeidssted;
    }

    private static List<MaritimtArbeid> lagOffshoreArbeid(OffshoreEnheter offshoreEnheter) {
        return offshoreEnheter.getOffshoreEnhet().stream().map(SoeknadMapper::lagOffshoreArbeidssted)
            .collect(Collectors.toList());
    }

    private static MaritimtArbeid lagOffshoreArbeidssted(OffshoreEnheter.OffshoreEnhet offshoreEnhet) {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.enhetNavn = offshoreEnhet.getEnhetsNavn();
        maritimtArbeid.innretningstype = mapInnretningstyper(offshoreEnhet.getEnhetsType());
        maritimtArbeid.innretningLandkode = offshoreEnhet.getSokkelLand();
        return maritimtArbeid;
    }

    private static Innretningstyper mapInnretningstyper(OffshoreEnhetstype offshoreEnhetstype) {
        switch (offshoreEnhetstype) {
            case BORESKIP:
                return Innretningstyper.BORESKIP;
            case PLATTFORM:
            case ANNEN_STASJONAER_ENHET:
                return Innretningstyper.PLATTFORM;
            default:
                return Innretningstyper.valueOf(offshoreEnhetstype.toString().toUpperCase());
        }
    }

    private static List<MaritimtArbeid> lagArbeidPåSkip(SkipListe skipListe) {
        return skipListe.getSkip().stream().map(SoeknadMapper::lagArbeidsstedPåSkip).collect(Collectors.toList());
    }

    private static MaritimtArbeid lagArbeidsstedPåSkip(SkipListe.Skip skip) {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.enhetNavn = skip.getSkipNavn();
        maritimtArbeid.fartsomradeKode = Fartsomrader.valueOf(skip.getFartsomraade().toString().toUpperCase());
        maritimtArbeid.flaggLandkode = skip.getFlaggland();
        maritimtArbeid.territorialfarvann = skip.getTerritorialEllerHavnLand();
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

    private static JuridiskArbeidsgiverNorge lagJuridiskArbeidsgiverNorge(Arbeidsgiver arbeidsgiver) {
        JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        if (arbeidsgiver != null && !arbeidsgiver.isOffentligVirksomhet() && arbeidsgiver.getSamletVirksomhetINorge() != null) {
            SamletVirksomhetINorge samletVirksomhetINorge = arbeidsgiver.getSamletVirksomhetINorge();
            juridiskArbeidsgiverNorge.antallAnsatte = samletVirksomhetINorge.getAntallAnsatte().intValue();
            juridiskArbeidsgiverNorge.antallAdmAnsatte = samletVirksomhetINorge.getAntallAdministrativeAnsatteINorge().intValue();
            juridiskArbeidsgiverNorge.antallUtsendte = samletVirksomhetINorge.getAntallUtsendte().intValue();
            juridiskArbeidsgiverNorge.andelOmsetningINorge = new BigDecimal(samletVirksomhetINorge.getAndelOmsetningINorge());
            juridiskArbeidsgiverNorge.andelOppdragINorge = new BigDecimal(samletVirksomhetINorge.getAndelOppdragINorge());
            juridiskArbeidsgiverNorge.andelKontrakterINorge = new BigDecimal(samletVirksomhetINorge.getAndelKontrakterInngaasINorge());
            juridiskArbeidsgiverNorge.andelRekruttertINorge = new BigDecimal(samletVirksomhetINorge.getAndelRekrutteresINorge());
            juridiskArbeidsgiverNorge.ekstraArbeidsgivere = List.of(arbeidsgiver.getVirksomhetsnummer());
        }
        return juridiskArbeidsgiverNorge;
    }

    private static LocalDate xmlCalTilLocalDate(XMLGregorianCalendar calendar) {
        return calendar == null ? null : LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }

    private static final Function<Barnet, MedfolgendeFamilie> mapBarnTilMedfølgendeFamilie
        = barnet -> MedfolgendeFamilie.tilBarnFraFnrOgNavn(barnet.getFoedselsnummer(), barnet.getNavn());
}
