package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_au
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.arbeidsstedLandManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.foretakUtlandManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.luftfartArbeidsstedManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.maritimtArbeidsstedManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.offshoreArbeidsstedManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.selvstendigUtlandManglerFelter
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import no.nav.melosys.service.kontroll.regler.ArbeidsstedRegler
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import no.nav.melosys.service.kontroll.regler.PersonRegler.harRegistrertAdresse
import no.nav.melosys.service.validering.Kontrollfeil

object FerdigbehandlingKontroll {
    fun overlappendePeriode(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapDokument = kontrollData.medlemskapDokument
        val medlemskapsperiodeData = kontrollData.medlemskapsperiodeData

        if (medlemskapsperiodeData != null && medlemskapsperiodeData.harNyeMedlemskapsperioder()) {
            return if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, medlemskapsperiodeData))
                Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL) else null
        }

        val kontrollPeriode = kontrollData.lovvalgsperiode!!
        val opprinneligLovvalgsperiode = kontrollData.opprinneligLovvalgsperiode

        if (harBehandlingstemaMedUnntakForOverlappendePeriode(kontrollPeriode, kontrollData.behandlingstema)
            && OverlappendeMedlemskapsperioderRegler.harOverlappendeUnntaksperiode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER, KontrolldataFeilType.ADVARSEL)
        }

        if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL)
        }

        return null
    }

    private fun harBehandlingstemaMedUnntakForOverlappendePeriode(lovvalgsperiode: Lovvalgsperiode, behandlingstema: Behandlingstema?): Boolean =
        behandlingstema in listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstema.UTSENDT_SELVSTENDIG) && lovvalgsperiode.erAvslått()

    fun overlappendeUnntaksperiode(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapDokument = kontrollData.medlemskapDokument
        val kontrollPeriode = kontrollData.lovvalgsperiode
        val opprinneligLovvalgsperiode = kontrollData.opprinneligLovvalgsperiode

        if (OverlappendeMedlemskapsperioderRegler.harOverlappendeUnntaksperiode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER, KontrolldataFeilType.ADVARSEL)
        }

        return null
    }

    fun overlappendeMedlemskapsperiode(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapDokument = kontrollData.medlemskapDokument
        val lovvalgsperiode = kontrollData.lovvalgsperiode
        val opprinneligLovvalgsperiode = kontrollData.opprinneligLovvalgsperiode

        if (OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(medlemskapDokument, lovvalgsperiode, opprinneligLovvalgsperiode)) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDLEMSKAPSPERIODER)
        }

        return null
    }

    fun periodeOver24Mnd(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!

        if (lovvalgsperiode.erArtikkel12() && PeriodeRegler.periodeOver24Måneder(lovvalgsperiode.fom, lovvalgsperiode.tom)) {
            return Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
        }

        return null
    }

    fun periodeManglerSluttdato(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!

        if (lovvalgsperiode.tom != null) {
            return null
        }

        val type = if (lovvalgsperiode.erAvslått()) KontrolldataFeilType.ADVARSEL else KontrolldataFeilType.FEIL
        return Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO, type)
    }


    fun periodeOverTreÅr(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1, Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3)
            && PeriodeRegler.periodeOver3År(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR)
        }

        return null
    }

    fun periodeOverFemÅr(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2, Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7)
            && PeriodeRegler.periodeOver5År(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
        }

        return null
    }

    fun periodeOver12Måneder(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4, Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2)
            && PeriodeRegler.periodeOver12Måneder(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_12_MD)
        }

        return null
    }

    fun arbeidsstedLandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return arbeidsstedLandManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun arbeidsstedMaritimtManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return maritimtArbeidsstedManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun arbeidsstedOffshoreManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return offshoreArbeidsstedManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun arbeidsstedLuftfartManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return luftfartArbeidsstedManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun foretakUtlandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return foretakUtlandManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun selvstendigUtlandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return selvstendigUtlandManglerFelter(kontrollData.mottatteOpplysningerData!!)
    }

    fun representantIUtlandetMangler(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.lovvalgsperiode!!
        val søknad = kontrollData.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        if (erBestemmelseDerTrygdeavtaleAttestSendes(lovvalgsperiode.bestemmelse)
            && ArbeidsstedRegler.representantIUtlandetMangler(søknad.representantIUtlandet)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED)
        }

        return null
    }

    fun orgnrErOpphørt(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val behandlingstema = kontrollData.behandlingstema

        if (behandlingstema in listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
            && kontrollData.saksopplysningerData!!.harOpphørtAvklartVirksomhet
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER)
        }

        return null
    }

    fun åpentUtkastFinnes(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        return if (kontrollData.brevUtkast.isEmpty()) null else Kontrollfeil(Kontroll_begrunnelser.ÅPENT_UTKAST)
    }

    fun adresseRegistrert(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val fullmektig = kontrollData.fullmektig

        if (fullmektig != null) {
            return if (fullmektig.erOrganisasjon()) erOrganisasjonAdresseRegistrert(kontrollData) else erPersonAdresseRegistrert(kontrollData)
        }

        if (!harRegistrertAdresse(kontrollData.persondata, kontrollData.mottatteOpplysningerData)) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
        }

        return null
    }

    private fun erPersonAdresseRegistrert(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        if (!harRegistrertAdresse(kontrollData.persondataTilFullmektig!!, kontrollData.mottatteOpplysningerData)) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
        }

        return null
    }

    private fun erOrganisasjonAdresseRegistrert(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val organisasjon = kontrollData.organisasjonDokument!!

        if (!organisasjon.harRegistrertPostadresse() && !organisasjon.harRegistrertForretningsadresse()) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
        }

        return null
    }

    private fun erBestemmelseDerTrygdeavtaleAttestSendes(bestemmelse: LovvalgBestemmelse): Boolean =
        erBestemmelseDerTrygdeavtaleAttestSendesUK(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesUSA(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesCanada(bestemmelse)
            || erBestemmelseDerTrygdeavtaleAttestSendesAU(bestemmelse)


    private fun erBestemmelseDerTrygdeavtaleAttestSendesUK(bestemmelse: LovvalgBestemmelse): Boolean =
        bestemmelse in listOf(
            Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
            Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_5,
            Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3
        )

    private fun erBestemmelseDerTrygdeavtaleAttestSendesUSA(bestemmelse: LovvalgBestemmelse): Boolean =
        bestemmelse in listOf(
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_3,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_5,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9
        )

    private fun erBestemmelseDerTrygdeavtaleAttestSendesAU(bestemmelse: LovvalgBestemmelse): Boolean =
        bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_2, Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3)

    private fun erBestemmelseDerTrygdeavtaleAttestSendesCanada(bestemmelse: LovvalgBestemmelse): Boolean =
        bestemmelse in Lovvalgsbestemmelser_trygdeavtale_ca.values()
}
