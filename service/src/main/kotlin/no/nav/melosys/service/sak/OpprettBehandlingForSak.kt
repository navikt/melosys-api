package no.nav.melosys.service.sak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
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
    private val behandlingService: BehandlingService
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

        if (sistBehandling.erAktiv() && behandlingstype != Behandlingstyper.ÅRSAVREGNING) {
            behandlingService.avsluttBehandling(sistBehandling.id)
        }

        if (saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, behandlingstype, behandlingstema)) {
            prosessinstansService.opprettOgReplikerBehandlingForSak(saksnummer, opprettSakDto.tilOpprettSakRequest())
        } else {
            prosessinstansService.opprettNyBehandlingForSak(saksnummer, opprettSakDto.tilOpprettSakRequest())
        }
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
