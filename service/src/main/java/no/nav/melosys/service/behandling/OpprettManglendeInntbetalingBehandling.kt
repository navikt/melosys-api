package no.nav.melosys.service.behandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class OpprettManglendeInntbetalingBehandling(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val opprettBehandlingForSak: OpprettBehandlingForSak
) {

    @Transactional
    fun opprettBehandlingManglendeInnbetaling(fakturaserieReferanse: String, mottaksDato: LocalDate) {
        val behandlingsresultat =
            behandlingsresultatService.hentBehandlingsresultatAvFakturaserieReferanse(fakturaserieReferanse)
        val behandling = behandlingService.hentBehandling(behandlingsresultat.behandling.id)
        val fagsak = behandling.fagsak
        val sistBehandling = fagsak.hentSistRegistrertBehandling()

        opprettBehandlingForSak.opprettBehandling(fagsak.saksnummer, lagOpprettSakDto(sistBehandling, mottaksDato, fakturaserieReferanse))
    }

    private fun lagOpprettSakDto(sistBehandling: Behandling, mottaksDato: LocalDate, fakturaserieReferanse: String?): OpprettSakDto {
        val opprettSakDto = OpprettSakDto()
        opprettSakDto.fakturaserieReferanse = fakturaserieReferanse
        opprettSakDto.behandlingstema = sistBehandling.tema
        opprettSakDto.behandlingstype = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        opprettSakDto.behandlingsaarsakType = sistBehandling.behandlingsårsak.type
        opprettSakDto.behandlingsaarsakFritekst = sistBehandling.behandlingsårsak.fritekst
        opprettSakDto.mottaksdato = mottaksDato
        return opprettSakDto
    }
}
