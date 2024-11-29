package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_au
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.exception.TekniskException
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
import java.time.LocalDate

object FerdigbehandlingKontroll {

    fun harOverlappendePeriodeMedForskuddsvisFaktureringIAnnenSak(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapsperiodeData = kontrollData.medlemskapsperiodeData
        if (medlemskapsperiodeData != null && medlemskapsperiodeData.harNyeMedlemskapsperioderMedAvgift()) {

            if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriodeMedForskuddsvisFaktureringIAndreFagsaker(medlemskapsperiodeData, kontrollData.fagsak?.saksnummer)
            ) {
                return Kontrollfeil(
                    Kontroll_begrunnelser.OVERLAPPENDE_PERIODE_MED_FORSKUDDSVIS_FAKTURERUNG,
                    KontrolldataFeilType.ADVARSEL
                )
            }
        }

        return null
    }


    fun overlappendePeriode(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapDokument = kontrollData.medlemskapDokument
        val medlemskapsperiodeData = kontrollData.medlemskapsperiodeData

        if (medlemskapsperiodeData != null && medlemskapsperiodeData.harNyeMedlemskapsperioder()) {
             if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, medlemskapsperiodeData))
                return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL)

            return null
        }

        val kontrollPeriode = kontrollData.lovvalgsperiode
        val opprinneligLovvalgsperiode = kontrollData.opprinneligLovvalgsperiode

        if (harBehandlingstemaMedUnntakForOverlappendePeriode(kontrollPeriode, kontrollData.behandlingstema)
            && OverlappendeMedlemskapsperioderRegler.harOverlappendeUnntaksperiode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER, KontrolldataFeilType.ADVARSEL)
        }

        if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
                medlemskapDokument,
                kontrollPeriode,
                opprinneligLovvalgsperiode
            ) && kontrollPeriode?.behandlingsresultat?.behandling?.status == Behandlingsstatus.SVAR_ANMODNING_MOTTATT
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.ADVARSEL)
        }



        if (OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(medlemskapDokument, kontrollPeriode, opprinneligLovvalgsperiode)) {
            return Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, KontrolldataFeilType.FEIL)
        }

        return null
    }

    private fun harBehandlingstemaMedUnntakForOverlappendePeriode(lovvalgsperiode: Lovvalgsperiode?, behandlingstema: Behandlingstema?): Boolean =
        behandlingstema in listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstema.UTSENDT_SELVSTENDIG) && lovvalgsperiode!!.erAvslått()

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
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()

        if (!lovvalgsperiode.erAvslått() && erBestemmelseDerInnvilgetMedlemskapsperiodeIkkeKanOverskride24mnd(lovvalgsperiode?.bestemmelse) &&
            PeriodeRegler.periodeOver24Måneder(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
        }

        return null
    }

    fun periodeManglerSluttdato(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()

        if (lovvalgsperiode.tom != null) {
            return null
        }

        val type = if (lovvalgsperiode.erAvslått()) KontrolldataFeilType.ADVARSEL else KontrolldataFeilType.FEIL
        return Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO, type)
    }

    fun periodeOverTreÅr(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1, Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3)
            && PeriodeRegler.periodeOver3År(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR)
        }

        return null
    }

    fun periodeOverFemÅr(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2, Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7)
            && PeriodeRegler.periodeOver5År(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
        }

        return null
    }

    fun periodeOver12Måneder(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()

        if (lovvalgsperiode.bestemmelse in listOf(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4, Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2)
            && PeriodeRegler.periodeOver12Måneder(lovvalgsperiode.fom, lovvalgsperiode.tom)
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.MER_ENN_12_MD)
        }

        return null
    }

    fun arbeidsstedLandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        arbeidsstedLandManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun arbeidsstedMaritimtManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        maritimtArbeidsstedManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun arbeidsstedOffshoreManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        offshoreArbeidsstedManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun arbeidsstedLuftfartManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        luftfartArbeidsstedManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun foretakUtlandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        foretakUtlandManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun selvstendigUtlandManglerFelter(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        selvstendigUtlandManglerFelter(kontrollData.hentMottatteOpplysningerData())

    fun representantIUtlandetMangler(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()
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

    fun direkteForutgåendePeriode(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val medlemskapsperiodeData = kontrollData.medlemskapsperiodeData ?: return null
        val gjeldendeSaksnummer = kontrollData.fagsak?.saksnummer
        val tidligereMedlemskapsperioderIandreFagsaker = medlemskapsperiodeData
            .tidligereMedlemskapsperioderForBukerMedAvgift
            .filter { it.behandlingsresultat.behandling.fagsak.saksnummer != gjeldendeSaksnummer }

        medlemskapsperiodeData.nyeMedlemskapsperioderMedAvgift.forEach { nyPeriode ->
            if (tidligereMedlemskapsperioderIandreFagsaker.any { it.tom?.plusDays(1) == nyPeriode.fom }) {
                return Kontrollfeil(
                    Kontroll_begrunnelser.DIREKTE_FORUTGÅENDE_PERIODE,
                    KontrolldataFeilType.ADVARSEL
                )
            }
        }
        return null
    }

    fun åpentUtkastFinnes(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? =
        if (kontrollData.brevUtkast.isEmpty()) null else Kontrollfeil(Kontroll_begrunnelser.ÅPENT_UTKAST)

    fun storbritanniaKonvensjonBruktForTidlig(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val lovvalgsperiode = kontrollData.hentLovvalgsperiode()
        val JANUAR_2024 = LocalDate.of(2024, 1, 1)
        val storbritanniaBestemmelser = Lovvalgbestemmelser_konv_efta_storbritannia.values()

        if (lovvalgsperiode.bestemmelse in storbritanniaBestemmelser && lovvalgsperiode.fom?.isBefore(JANUAR_2024) == true) {
            return Kontrollfeil(Kontroll_begrunnelser.STORBRITANNIA_KONV_BRUKT_FOR_TIDLIG)
        }

        return null
    }

    fun kunEnAvklartVirksomhet(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        if (kontrollData.antallArbeidsgivere != 1 && kontrollData.behandlingstema in listOf(
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Behandlingstema.UTSENDT_SELVSTENDIG,
                Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            )
        ) {
            return Kontrollfeil(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET_BREV)
        }

        return null
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
        val persondataTilFullmektig = kontrollData.persondataTilFullmektig ?: throw TekniskException("Persondata til fullmektig kan ikke være null")
        if (!harRegistrertAdresse(persondataTilFullmektig, kontrollData.mottatteOpplysningerData)) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
        }

        return null
    }

    private fun erOrganisasjonAdresseRegistrert(kontrollData: FerdigbehandlingKontrollData): Kontrollfeil? {
        val organisasjon = kontrollData.organisasjonDokument ?: throw TekniskException("OrganisasjonDokument kan ikke være null")

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

    private fun erBestemmelseDerInnvilgetMedlemskapsperiodeIkkeKanOverskride24mnd(bestemmelse: LovvalgBestemmelse?): Boolean =
        bestemmelse === Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1 || bestemmelse === Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2 ||
            bestemmelse === Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1 || bestemmelse ===
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2

    private fun FerdigbehandlingKontrollData.hentLovvalgsperiode() =
        this.lovvalgsperiode ?: throw TekniskException("Lovvalgsperiode kan ikke være null")

    private fun FerdigbehandlingKontrollData.hentMottatteOpplysningerData() =
        this.mottatteOpplysningerData ?: throw TekniskException("MottatteOpplysningerData kan ikke være null")
}
