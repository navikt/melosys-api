package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


@Service
class IverksettTrygdeavgiftService(
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val prosessinstansService: ProsessinstansService,
    private val oppgaveService: OppgaveService
) {
    private val log = LoggerFactory.getLogger(HelseutgiftDekkesPeriodeService::class.java)

    @Transactional
    fun opprettProsessIverksettTrygdeavgiftPensjonist(behandlingID: Long, behandlingsresultattype: Behandlingsresultattyper, vedtakstype: Vedtakstyper){
        val behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow { IkkeFunnetException("Finner ingen behandlingsresultat for id: $behandlingID") }
        val behandling = behandlingsresultat.behandling

        log.info("Iverksetter trygdeavgift for (EU_EØS pensjonister) sak: {} behandling: {}", behandling.fagsak.saksnummer, behandlingID)

        oppdaterBehandlingsresultat(behandlingsresultat, vedtakstype, behandlingsresultattype)

        prosessinstansService.opprettProsessinstansEøsPensjonistAvgift(behandling)

        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }


    private fun oppdaterBehandlingsresultat(
        behandlingsresultat: Behandlingsresultat,
        vedtakstype: Vedtakstyper,
        behandlingsresultattype: Behandlingsresultattyper
    ) {
        behandlingsresultat.settVedtakMetadata(vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
        behandlingsresultat.type = behandlingsresultattype
        behandlingsresultatService.lagre(behandlingsresultat)
    }
}
