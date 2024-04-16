package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.exception.KontrolldataFeilType;
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData;
import no.nav.melosys.service.kontroll.regler.ArbeidsstedRegler;
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

import java.util.Arrays;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_2;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us.*;
import static no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.harOverlappendeUnntaksperiode;


final class FerdigbehandlingKontroll {

    private FerdigbehandlingKontroll() {
    }

    static Kontrollfeil overlappendePeriode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        MedlemskapsperiodeData medlemskapsperiodeData = kontrollData.getMedlemskapsperiodeData();
        Lovvalgsperiode kontrollPeriode = kontrollData.getLovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.getOpprinneligLovvalgsperiode();

        if (medlemskapsperiodeData != null && medlemskapsperiodeData.harNyeMedlemskapsperioder()) {
            return OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, medlemskapsperiodeData)
                ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL)
                : null;
        } else if (harBehandlingstemaMedUnntakForOverlappendePeriode(kontrollPeriode, kontrollData.getBehandlingstema())
            && harOverlappendeUnntaksperiode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)) {
            return new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER, KontrolldataFeilType.ADVARSEL);
        }

        return OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)
            ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL)
            : null;
    }

    static boolean harBehandlingstemaMedUnntakForOverlappendePeriode(Lovvalgsperiode lovvalgsperiode, Behandlingstema behandlingstema) {
        return (behandlingstema == Behandlingstema.UTSENDT_ARBEIDSTAKER || behandlingstema == Behandlingstema.UTSENDT_SELVSTENDIG)
            && lovvalgsperiode.erAvslått();
    }

    static Kontrollfeil overlappendeUnntaksperiode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode kontrollPeriode = kontrollData.getLovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.getOpprinneligLovvalgsperiode();

        if (!harOverlappendeUnntaksperiode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)) {
            return null;
        }

        return new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER, KontrolldataFeilType.ADVARSEL);
    }


    static Kontrollfeil overlappendeMedlemskapsperiode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.getOpprinneligLovvalgsperiode();

        return OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(medlemskapDokument,
            lovvalgsperiode,
            opprinneligLovvalgsperiode)
            ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDLEMSKAPSPERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.erArtikkel12()
            && PeriodeRegler.periodeOver24Måneder(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();
        if (lovvalgsperiode.getTom() != null) {
            return null;
        } else {
            return lovvalgsperiode.erAvslått()
                ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO, KontrolldataFeilType.ADVARSEL)
                : new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO, KontrolldataFeilType.FEIL);
        }
    }

    static Kontrollfeil periodeOverTreÅr(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return (lovvalgsperiode.getBestemmelse() == UK_ART6_1 || lovvalgsperiode.getBestemmelse() == AUS_ART9_3)
            && PeriodeRegler.periodeOver3År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR) : null;
    }

    static Kontrollfeil periodeOverFemÅr(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return (lovvalgsperiode.getBestemmelse() == USA_ART5_2 || lovvalgsperiode.getBestemmelse() == CAN_ART7)
            && PeriodeRegler.periodeOver5År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_FEM_ÅR) : null;
    }

    static Kontrollfeil periodeOver12Måneder(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return (lovvalgsperiode.getBestemmelse() == USA_ART5_4 || lovvalgsperiode.getBestemmelse() == CAN_ART6_2)
            && PeriodeRegler.periodeOver12Måneder(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_12_MD) : null;
    }

    static Kontrollfeil arbeidsstedLandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.arbeidsstedLandManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil arbeidsstedMaritimtManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.maritimtArbeidsstedManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil arbeidsstedOffshoreManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.offshoreArbeidsstedManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil arbeidsstedLuftfartManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.luftfartArbeidsstedManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.foretakUtlandManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil selvstendigUtlandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.selvstendigUtlandManglerFelter(kontrollData.getMottatteOpplysningerData());
    }

    static Kontrollfeil representantIUtlandetMangler(FerdigbehandlingKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.getLovvalgsperiode();
        var søknad = (SøknadNorgeEllerUtenforEØS) kontrollData.getMottatteOpplysningerData();

        return erBestemmelseDerTrygdeavtaleAttestSendes(lovvalgsperiode.getBestemmelse())
            && ArbeidsstedRegler.representantIUtlandetMangler(søknad.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED) : null;
    }

    public static Kontrollfeil orgnrErOpphørt(FerdigbehandlingKontrollData kontrollData) {
        var behandlingstema = kontrollData.getBehandlingstema();
        return (behandlingstema == Behandlingstema.UTSENDT_ARBEIDSTAKER || behandlingstema == Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
            && kontrollData.getSaksopplysningerData().harOpphørtAvklartVirksomhet()
            ? new Kontrollfeil(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER)
            : null;
    }

    static Kontrollfeil åpentUtkastFinnes(FerdigbehandlingKontrollData kontrollData) {
        var åpneUtkast = kontrollData.getBrevUtkast();
        return åpneUtkast == null || åpneUtkast.isEmpty() ? null : new Kontrollfeil(Kontroll_begrunnelser.ÅPENT_UTKAST);
    }

    static Kontrollfeil adresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        var representant = kontrollData.getFullmektig();
        boolean harRepresentant = representant != null;

        var brukerKontrollfeil = new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);

        if (harRepresentant) {
            return håndterRepresentantBrukerOgOrganisasjon(kontrollData, representant.erOrganisasjon());
        }

        return PersonRegler.harRegistrertAdresse(kontrollData.getPersondata(), kontrollData.getMottatteOpplysningerData()) ? null : brukerKontrollfeil;
    }

    private static Kontrollfeil håndterRepresentantBrukerOgOrganisasjon(FerdigbehandlingKontrollData kontrollData, boolean representantErOrganisasjon) {
        if (representantErOrganisasjon) return erOrganisasjonAdresseRegistrert(kontrollData);

        return erPersonAdresseRegistrert(kontrollData);
    }

    private static Kontrollfeil erPersonAdresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        Kontrollfeil representantKontrollfeil = new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT);

        return PersonRegler.harRegistrertAdresse(kontrollData.getPersondataTilFullmektig(), kontrollData.getMottatteOpplysningerData()) ? null : representantKontrollfeil;
    }

    private static Kontrollfeil erOrganisasjonAdresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        Kontrollfeil representatKontrollfeil = new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT);

        OrganisasjonDokument organisasjon = kontrollData.getOrganisasjonDokument();
        boolean organisasjonHarRegistrertPostadresse = organisasjon.harRegistrertPostadresse();
        boolean organisasjonHarRegistrertForretningsadresse = organisasjon.harRegistrertForretningsadresse();

        return (organisasjonHarRegistrertPostadresse || organisasjonHarRegistrertForretningsadresse) ? null : representatKontrollfeil;
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendes(LovvalgBestemmelse bestemmelse) {
        return erBestemmelseDerTrygdeavtaleAttestSendesUK(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesUSA(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesCanada(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesAU(bestemmelse);
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesUK(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == UK_ART6_1
            || bestemmelse == UK_ART6_5
            || bestemmelse == UK_ART7_3;
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesUSA(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == USA_ART5_2
            || bestemmelse == USA_ART5_3
            || bestemmelse == USA_ART5_4
            || bestemmelse == USA_ART5_5
            || bestemmelse == USA_ART5_6
            || bestemmelse == USA_ART5_9;
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesAU(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == AUS_ART9_2
            || bestemmelse == AUS_ART9_3;
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesCanada(LovvalgBestemmelse bestemmelse) {
        return Arrays.stream(Lovvalgsbestemmelser_trygdeavtale_ca.values()).anyMatch(p -> p.equals(bestemmelse));
    }
}
