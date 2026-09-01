package no.nav.melosys.service

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsnotatRepository
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsnotatService(
    private val behandlingsnotatRepository: BehandlingsnotatRepository,
    private val fagsakService: FagsakService,
) {

    @Transactional(readOnly = true)
    fun hentNotatForFagsak(saksnummer: String): Collection<Behandlingsnotat> =
        fagsakService.hentFagsak(saksnummer).behandlinger.flatMap { it.behandlingsnotater }

    @Transactional
    fun opprettNotat(saksnummer: String, tekst: String): Behandlingsnotat {
        val behandling: Behandling = fagsakService.hentFagsak(saksnummer).finnAktivBehandlingIkkeÅrsavregning()
            ?: throw FunksjonellException("Fagsak $saksnummer har ingen aktive behandlinger")

        val behandlingsnotat = Behandlingsnotat().apply {
            this.behandling = behandling
            this.tekst = tekst
        }
        return behandlingsnotatRepository.save(behandlingsnotat)
    }

    @Transactional
    fun oppdaterNotat(notatID: Long, tekst: String): Behandlingsnotat {
        val behandlingsnotat = hentNotat(notatID)

        if (!behandlingsnotat.erRedigerbar()) {
            throw FunksjonellException("Notat med id $notatID kan ikke oppdateres, da den tilhører en behandling som er avsluttet")
        } else if (!brukerKanRedigereNotat(behandlingsnotat)) {
            throw FunksjonellException("Et notat kan ikke endres av andre!")
        }

        behandlingsnotat.tekst = tekst
        return behandlingsnotatRepository.save(behandlingsnotat)
    }

    fun kanRedigereNotat(behandlingsnotat: Behandlingsnotat): Boolean =
        behandlingsnotat.erRedigerbar() && brukerKanRedigereNotat(behandlingsnotat)

    private fun hentNotat(id: Long): Behandlingsnotat = behandlingsnotatRepository.findById(id)
        .orElseThrow { IkkeFunnetException("Finner ikke notat med id $id") }

    private fun brukerKanRedigereNotat(behandlingsnotat: Behandlingsnotat): Boolean =
        SubjectHandler.getInstance().userID == behandlingsnotat.registrertAv
}
