package no.nav.melosys.service.lovvalgsperiode

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OpprettLovvalgsperiodeService(
    val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    val behandlingService: BehandlingService,
    val behandlingsresultatService: BehandlingsresultatService,
    val saksbehandlingRegler: SaksbehandlingRegler
) {

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

        if (saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)) {
            validerRequestForUnntaksregistrering(request)
            return oppdaterLovvalgsperiodeForUnntaksregistrering(
                behandling,
                eksisterendeLovvalgsperiode,
                request.fomDato!!,
                request.tomDato,
                request.lovvalgsbestemmelse!!
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
    }

    private fun validerRequestForIkkeYrkesaktiv(request: OpprettLovvalgsperiodeRequest) {
        if (request.innvilgelsesResultat == null) throw FunksjonellException("Kan ikke opprette lovvalgsperiode for ikke-yrkesaktive uten innvilgelsesresultat")
    }

    private fun oppdaterLovvalgsperiodeForUnntaksregistrering(
        behandling: Behandling,
        eksisterendeLovvalgsperiode: Lovvalgsperiode?,
        fom: LocalDate,
        tom: LocalDate?,
        bestemmelse: LovvalgBestemmelse
    ): Lovvalgsperiode {
        val anmodningEllerAttest = behandling.mottatteOpplysninger.mottatteOpplysningerData as AnmodningEllerAttest
        val lovvalgsland = anmodningEllerAttest.lovvalgsland
        val medlemskapstype = utledMedlemskapstype(behandling.fagsak.type, bestemmelse)

        val lovvalgsperiode = eksisterendeLovvalgsperiode ?: Lovvalgsperiode().apply {
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
            this.behandlingsresultat = behandlingsresultat
            behandlingsresultat.lovvalgsperioder.add(this)
        }
        lovvalgsperiode.fom = fom
        lovvalgsperiode.tom = tom
        lovvalgsperiode.bestemmelse = bestemmelse
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        lovvalgsperiode.lovvalgsland = if (lovvalgsland == Land_iso2.CA_QC) Land_iso2.CA else lovvalgsland
        lovvalgsperiode.medlemskapstype = medlemskapstype
        lovvalgsperiode.dekning = utledDekning(medlemskapstype, bestemmelse)
        return lovvalgsperiodeRepository.save(lovvalgsperiode)
    }

    private fun utledMedlemskapstype(sakstype: Sakstyper, bestemmelse: LovvalgBestemmelse): Medlemskapstyper =
        if (sakstype == Sakstyper.TRYGDEAVTALE && listOf(
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9
            ).contains(bestemmelse)
        ) Medlemskapstyper.DELVIS_UNNTATT else Medlemskapstyper.UNNTATT

    private fun utledDekning(medlemskapstype: Medlemskapstyper, bestemmelse: LovvalgBestemmelse): Trygdedekninger {
        if (medlemskapstype == Medlemskapstyper.UNNTATT) return Trygdedekninger.UTEN_DEKNING

        return when (bestemmelse) {
            Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7, Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11 -> Trygdedekninger.UNNTATT_CAN_7_5_B
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2, Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9 -> Trygdedekninger.UNNTATT_USA_5_2_G
            else -> throw FunksjonellException("Finner ikke spesiell trykdedekning for bestemmelse $bestemmelse")
        }
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
}
