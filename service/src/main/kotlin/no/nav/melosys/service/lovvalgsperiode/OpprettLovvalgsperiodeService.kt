package no.nav.melosys.service.lovvalgsperiode

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OpprettLovvalgsperiodeService(
    val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    val behandlingService: BehandlingService,
    val behandlingsresultatService: BehandlingsresultatService,
    val saksbehandlingRegler: SaksbehandlingRegler,
    val landvelgerService: LandvelgerService
) {

    @Transactional
    fun opprettLovvalgsperiode(
        behandlingId: Long,
        request: OpprettLovvalgsperiodeRequest
    ): Lovvalgsperiode {
        val lovvalgsperioder = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandlingId)
        if (lovvalgsperioder.size > 1) throw FunksjonellException("Fant ${lovvalgsperioder.size} lovvalgsperioder. Forventer maks én lovvalgsperiode")

        return opprettEllerOppdaterLovvalgsperiode(behandlingId, lovvalgsperioder.firstOrNull(), request)
    }

    private fun opprettEllerOppdaterLovvalgsperiode(
        behandlingId: Long,
        eksisterendeLovvalgsperiode: Lovvalgsperiode?,
        request: OpprettLovvalgsperiodeRequest
    ): Lovvalgsperiode {
        val behandling = behandlingService.hentBehandling(behandlingId)

        if (harUtsendtArbeidsTakerKunNorgeFlyt(behandling)) {
            oppdaterLovvalgsperiodeForUtsendtArbeidsTakerKunNorgeFlyt(
                behandling,
                eksisterendeLovvalgsperiode,
                request.lovvalgsbestemmelse,
                request.fomDato,
                request.tomDato
            )
        }

        if (saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)) {
            validerRequestForUnntaksregistrering(request)
            return oppdaterLovvalgsperiodeForUnntaksregistrering(
                behandling,
                eksisterendeLovvalgsperiode,
                request.fomDato!!,
                request.tomDato,
                request.lovvalgsbestemmelse!!,
                request.trygdedekning,
            )
        }

        if (saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)) {
            validerRequestForIkkeYrkesaktiv(request)
            return oppdaterLovvalgsperiodeForIkkeYrkesaktiv(
                behandling,
                eksisterendeLovvalgsperiode,
                request.lovvalgsbestemmelse,
                request.innvilgelsesResultat!!
            )
        }

        throw FunksjonellException("Støtter ikke opprettelse av lovvalgsperiode for denne flyten")
    }


    private fun validerRequestForUnntaksregistrering(request: OpprettLovvalgsperiodeRequest) {
        if (request.fomDato == null) throw FunksjonellException("Kan ikke opprette lovvalgsperiode for unntakregistrering uten fom-dato")
        if (request.tomDato != null && request.fomDato.isAfter(request.tomDato)) throw FunksjonellException("Fom-dato ${request.fomDato} er etter tom-dato ${request.tomDato}")
        if (request.lovvalgsbestemmelse == null) throw FunksjonellException("Kan ikke opprette lovvalgsperiode for unntakregistrering uten bestemmelse")
        if ((request.lovvalgsbestemmelse == Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11 || request.lovvalgsbestemmelse ==
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9) && request.trygdedekning == null) throw FunksjonellException("Kan ikke opprette " +
            "lovvalgsperiode for unntaksregistrering med lovvalgsbestemmelse: ${request.lovvalgsbestemmelse} uten manuelt registrert trygdedekning")
    }

    private fun validerRequestForIkkeYrkesaktiv(request: OpprettLovvalgsperiodeRequest) {
        if (request.innvilgelsesResultat == null) throw FunksjonellException("Kan ikke opprette lovvalgsperiode for ikke-yrkesaktive uten innvilgelsesresultat")
    }

    private fun oppdaterLovvalgsperiodeForUnntaksregistrering(
        behandling: Behandling,
        eksisterendeLovvalgsperiode: Lovvalgsperiode?,
        fom: LocalDate,
        tom: LocalDate?,
        bestemmelse: LovvalgBestemmelse,
        trygdedekning: Trygdedekninger?
    ): Lovvalgsperiode {
        val anmodningEllerAttest = behandling.mottatteOpplysninger.mottatteOpplysningerData as AnmodningEllerAttest
        val lovvalgsland = anmodningEllerAttest.lovvalgsland
        val utledetTrygdedekning = trygdedekning ?: utledTrygdedekning(bestemmelse)
        val medlemskapstype = utledMedlemskapstype(utledetTrygdedekning)

        val lovvalgsperiode = eksisterendeLovvalgsperiode ?: Lovvalgsperiode().apply {
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
            this.behandlingsresultat = behandlingsresultat
            behandlingsresultat.lovvalgsperioder.add(this)
        }

        lovvalgsperiode.apply {
            this.fom = fom
            this.tom = tom
            this.bestemmelse = bestemmelse
            this.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            this.lovvalgsland = if (lovvalgsland == Land_iso2.CA_QC) Land_iso2.CA else lovvalgsland
            this.medlemskapstype = medlemskapstype
            this.dekning = utledetTrygdedekning
        }

        return lovvalgsperiodeRepository.save(lovvalgsperiode)
    }

    private fun utledTrygdedekning(bestemmelse: LovvalgBestemmelse): Trygdedekninger {
        return when (bestemmelse) {
            Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7 -> Trygdedekninger.UNNTATT_CAN_7_5_B
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2 -> Trygdedekninger.UNNTATT_USA_5_2_G
            else -> Trygdedekninger.UTEN_DEKNING
        }
    }
    private fun utledMedlemskapstype(trygdedekning: Trygdedekninger): Medlemskapstyper {
        return if (listOf(Trygdedekninger.UNNTATT_USA_5_2_G, Trygdedekninger.UNNTATT_CAN_7_5_B).contains(trygdedekning))
            Medlemskapstyper.DELVIS_UNNTATT else Medlemskapstyper.UNNTATT
    }

    private fun oppdaterLovvalgsperiodeForIkkeYrkesaktiv(
        behandling: Behandling,
        eksisterendeLovvalgsperiode: Lovvalgsperiode?,
        bestemmelse: LovvalgBestemmelse?,
        innvilgelsesResultat: InnvilgelsesResultat
    ): Lovvalgsperiode {
        val mottatteOpplysningerData = behandling.mottatteOpplysninger.mottatteOpplysningerData

        val lovvalgsperiode = eksisterendeLovvalgsperiode ?: Lovvalgsperiode().apply {
            behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        }
        lovvalgsperiode.fom = mottatteOpplysningerData.periode.fom
        lovvalgsperiode.tom = mottatteOpplysningerData.periode.tom
        lovvalgsperiode.bestemmelse = bestemmelse
        lovvalgsperiode.innvilgelsesresultat = innvilgelsesResultat
        lovvalgsperiode.lovvalgsland = Land_iso2.NO
        lovvalgsperiode.medlemskapstype = Medlemskapstyper.PLIKTIG
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING
        return lovvalgsperiodeRepository.save(lovvalgsperiode)
    }

    private fun oppdaterLovvalgsperiodeForUtsendtArbeidsTakerKunNorgeFlyt(
        behandling: Behandling,
        eksisterendeLovvalgsperiode: Lovvalgsperiode?,
        bestemmelse: LovvalgBestemmelse?,
        fomDato: LocalDate?,
        tomDato: LocalDate?
    ): Lovvalgsperiode {
        val lovvalgsperiode = eksisterendeLovvalgsperiode ?: Lovvalgsperiode().apply {
            behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        }
        lovvalgsperiode.fom = fomDato
        lovvalgsperiode.tom = tomDato
        lovvalgsperiode.bestemmelse = bestemmelse
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        lovvalgsperiode.lovvalgsland = Land_iso2.NO
        return lovvalgsperiodeRepository.save(lovvalgsperiode)
    }

    private fun harUtsendtArbeidsTakerKunNorgeFlyt(behandling: Behandling): Boolean {
        return behandling.fagsak.erSakstypeEøs()
            && (behandling.tema.equals(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            || behandling.tema.equals(Behandlingstema.UTSENDT_SELVSTENDIG)
            || behandling.tema.equals(Behandlingstema.ARBEID_KUN_NORGE)
            && landvelgerService.hentArbeidsland(behandling.id).equals(Land_iso2.NO))
    }

}
