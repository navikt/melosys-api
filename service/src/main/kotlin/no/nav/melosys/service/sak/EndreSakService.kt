package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import javax.transaction.Transactional

private val log = KotlinLogging.logger { }

@Service
class EndreSakService(
    private val fagsakService: FagsakService,
    private val behandlingsgrunnlagService: BehandlingsgrunnlagService,
    private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun endre(saksnummer: String, nySakstype: Sakstyper, nySakstema: Sakstemaer) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        validerSak(fagsak, nySakstype, nySakstema)

        val behandling = fagsak.hentAktivBehandling()
        gjenopprettBehandlingsgrunnlag(nySakstype, behandling)

        fagsakService.oppdaterSakstype(saksnummer, nySakstype)
        fagsakService.oppdaterSakstema(saksnummer, nySakstema)

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)
        applicationEventPublisher.publishEvent(FagsakEndretAvSaksbehandler(fagsak.saksnummer))
        log.debug { "Ferdig med endring av sak $saksnummer (type: $nySakstype, tema: $nySakstema)" }
    }

    private fun gjenopprettBehandlingsgrunnlag(
        nySakstype: Sakstyper, behandling: Behandling
    ) {
        val behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandling.id)
        if (nySakstype == Sakstyper.EU_EOS) {
            validerBehandlingsgrunnlag(behandlingsgrunnlag)
        }
        behandlingsgrunnlagService.slettBehandlingsgrunnlag(behandling.id)
        behandlingsgrunnlagService.opprettSøknad(
            behandling,
            behandlingsgrunnlag.behandlingsgrunnlagdata.periode,
            behandlingsgrunnlag.behandlingsgrunnlagdata.soeknadsland
        )
    }

    private fun validerSak(
        fagsak: Fagsak, sakstype: Sakstyper, sakstema: Sakstemaer
    ) {
        if (fagsak.type == sakstype && fagsak.tema == sakstema) {
            throw FunksjonellException("Sak ${fagsak.saksnummer} har allerede type ${fagsak.type} og tema ${fagsak.tema}")
        }
        if (!fagsak.kanEndreTypeOgTema()) {
            throw FunksjonellException("Sak ${fagsak.saksnummer} kan ikke endres")
        }
    }

    private fun validerBehandlingsgrunnlag(behandlingsgrunnlag: Behandlingsgrunnlag?) {
        val behandlingsgrunnlagdata = behandlingsgrunnlag?.behandlingsgrunnlagdata
        if (behandlingsgrunnlagdata == null || manglerPeriode(behandlingsgrunnlagdata) || manglerSøknadsland(behandlingsgrunnlagdata)) {
            throw FunksjonellException("Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS")
        }
    }

    private fun manglerPeriode(behandlingsgrunnlagdata: BehandlingsgrunnlagData) =
        behandlingsgrunnlagdata.periode == null || behandlingsgrunnlagdata.periode.fom == null

    private fun manglerSøknadsland(behandlingsgrunnlagdata: BehandlingsgrunnlagData) =
        behandlingsgrunnlagdata.soeknadsland == null || behandlingsgrunnlagdata.soeknadsland.erUkjenteEllerAlleEosLand
}
