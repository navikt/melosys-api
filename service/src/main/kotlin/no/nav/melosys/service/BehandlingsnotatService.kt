package no.nav.melosys.service

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.domain.Fagsak
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
    fun opprettNotat(saksnummer: String, tekst: String, behandlingId: Long? = null): Behandlingsnotat {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = behandlingId
            ?.let { finnBehandlingPåFagsak(fagsak, it) }
            ?: finnBehandlingUtenEksplisittValg(fagsak)

        val behandlingsnotat = Behandlingsnotat().apply {
            this.behandling = behandling
            this.tekst = tekst
        }
        return behandlingsnotatRepository.save(behandlingsnotat)
    }

    /**
     * Slår opp behandlingen i fagsakens egen samling. Det sikrer samtidig at behandlingen faktisk
     * tilhører fagsaken det er gjort tilgangskontroll på.
     */
    private fun finnBehandlingPåFagsak(fagsak: Fagsak, behandlingId: Long): Behandling {
        val behandling = fagsak.behandlinger.firstOrNull { it.id == behandlingId }
            ?: throw IkkeFunnetException("Finner ikke behandling med id $behandlingId på fagsak ${fagsak.saksnummer}")

        if (!behandling.erAktiv()) {
            throw FunksjonellException(
                "Behandling med id $behandlingId på fagsak ${fagsak.saksnummer} er ikke aktiv, og kan ikke få nye notater"
            )
        }
        return behandling
    }

    /**
     * Bakoverkompatibel utledning for klienter som ikke sender behandlingId. Prioriterer aktiv
     * ikke-årsavregning slik det alltid har vært gjort, men faller tilbake til en entydig aktiv
     * årsavregning. Er det flere aktive årsavregninger er forespørselen tvetydig, og vi gjetter ikke.
     */
    private fun finnBehandlingUtenEksplisittValg(fagsak: Fagsak): Behandling {
        fagsak.finnAktivBehandlingIkkeÅrsavregning()?.let { return it }

        val aktiveÅrsavregninger = fagsak.hentAktiveÅrsavregninger()
        return when (aktiveÅrsavregninger.size) {
            0 -> throw FunksjonellException("Fagsak ${fagsak.saksnummer} har ingen aktive behandlinger")
            1 -> aktiveÅrsavregninger.single()
            else -> throw FunksjonellException(
                "Fagsak ${fagsak.saksnummer} har flere aktive årsavregninger. behandlingId må oppgis for å angi hvilken behandling notatet gjelder"
            )
        }
    }

    /**
     * [saksnummer] er fagsaken det er gjort tilgangskontroll på. Verifiseres mot notatets egen fagsak
     * slik at et notat på en annen fagsak ikke kan endres via et saksnummer man har tilgang til.
     */
    @Transactional
    fun oppdaterNotat(saksnummer: String, notatID: Long, tekst: String): Behandlingsnotat {
        val behandlingsnotat = hentNotat(notatID)

        if (behandlingsnotat.behandling.fagsak.saksnummer != saksnummer) {
            throw IkkeFunnetException("Finner ikke notat med id $notatID på fagsak $saksnummer")
        }

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
