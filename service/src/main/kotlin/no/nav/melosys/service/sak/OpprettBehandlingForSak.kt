package no.nav.melosys.service.sak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpprettBehandlingForSak(
    private val fagsakService: FagsakService,
    private val prosessinstansService: ProsessinstansService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatRepository: BehandlingsresultatRepository
) {
    @Transactional
    fun opprettBehandling(saksnummer: String?, opprettSakDto: OpprettSakDto) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val alleBehandlinger = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()

        val behandlingstema = opprettSakDto.behandlingstema
        val behandlingstype = opprettSakDto.behandlingstype

        val sistBehandling: Behandling = if (behandlingstype == Behandlingstyper.ÅRSAVREGNING) {
            alleBehandlinger.firstOrNull()
        } else {
            alleBehandlinger.firstOrNull { behandling -> behandling.type != Behandlingstyper.ÅRSAVREGNING }

        } ?: throw IkkeFunnetException("Finner ikke behandling for fagsak med saksnummer: ${saksnummer.toString()}")


        valider(fagsak, opprettSakDto)
        lovligeKombinasjonerSaksbehandlingService.validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(
            fagsak,
            sistBehandling,
            behandlingstema!!,
            behandlingstype!!
        )

        val skalReplikeres = saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, behandlingstype, behandlingstema)
        val behandlingIdForReplikering = finnBehandlingIdForReplikeringVedAnmodningOmUnntak(sistBehandling)

        if (sistBehandling.erAktiv() && behandlingstype != Behandlingstyper.ÅRSAVREGNING) {
            behandlingService.avsluttBehandling(sistBehandling.id)
        }

        if (skalReplikeres) {
            prosessinstansService.opprettOgReplikerBehandlingForSak(
                saksnummer, opprettSakDto.tilOpprettSakRequest(), behandlingIdForReplikering
            )
        } else {
            prosessinstansService.opprettNyBehandlingForSak(saksnummer, opprettSakDto.tilOpprettSakRequest())
        }
    }

    /**
     * Når en aktiv behandling med resultat ANMODNING_OM_UNNTAK avsluttes pga ny vurdering,
     * må vi lagre behandling-ID-en eksplisitt for replikering. Etter avslutning endres resultatet
     * til FERDIGBEHANDLET, som normalt blokkerer replikering i ReplikerBehandling-sagasteget.
     */
    private fun finnBehandlingIdForReplikeringVedAnmodningOmUnntak(behandling: Behandling): Long? {
        if (!behandling.erAktiv()) return null
        val resultat = behandlingsresultatRepository.findById(behandling.id).orElse(null) ?: return null
        return if (resultat.type == Behandlingsresultattyper.ANMODNING_OM_UNNTAK) behandling.id else null
    }

    private fun valider(fagsak: Fagsak, opprettSakDto: OpprettSakDto) {
        if (opprettSakDto.behandlingstema == null) {
            throw FunksjonellException("Behandlingstema mangler")
        }
        if (opprettSakDto.behandlingstype == null) {
            throw FunksjonellException("Behandlingstype mangler")
        }

        val muligeBehandlingstyper: Set<Behandlingstyper?> =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
                Aktoersroller.BRUKER,
                fagsak.saksnummer,
                opprettSakDto.behandlingstema
            )

        if (!muligeBehandlingstyper.contains(opprettSakDto.behandlingstype)) {
            throw FunksjonellException(
                String.format(
                    "Behandlingstype %s er ikke lovlig for behandlingstema %s og saksnummer %s",
                    opprettSakDto.behandlingstype,
                    opprettSakDto.behandlingstema,
                    fagsak.saksnummer
                )
            )
        }

        if (opprettSakDto.mottaksdato == null) {
            throw FunksjonellException("Mottaksdato er påkrevd for å opprette behandling")
        }
        if (opprettSakDto.behandlingsaarsakType == null) {
            throw FunksjonellException("Årsak er påkrevd for å opprette behandling")
        }
        if (StringUtils.isNotEmpty(opprettSakDto.behandlingsaarsakFritekst) && opprettSakDto.behandlingsaarsakType != Behandlingsaarsaktyper.FRITEKST) {
            throw FunksjonellException("Kan ikke lagre fritekst som årsak når årsakstype er " + opprettSakDto.behandlingsaarsakType)
        }
    }
}
