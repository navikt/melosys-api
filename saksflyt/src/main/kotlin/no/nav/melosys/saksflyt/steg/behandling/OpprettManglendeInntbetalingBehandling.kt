package no.nav.melosys.saksflyt.steg.behandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OpprettManglendeInntbetalingBehandling(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val opprettBehandlingForSak: OpprettBehandlingForSak
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        //TODO: Ta hensyn til at det kan eksistere en åpen behadnling fra før. Gjøres på egen oppgave. MELOSYS-6187. Husk tester!

        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)
        val mottaksDato = LocalDate.parse(prosessinstans.getData(ProsessDataKey.MOTTATT_DATO))
        val behandlingsresultat =
            behandlingsresultatService.hentBehandlingsresultatAvFakturaserieReferanse(fakturaserieReferanse)
        val behandling = behandlingService.hentBehandling(behandlingsresultat.behandling.id)
        val fagsak = behandling.fagsak
        val sistBehandling = fagsak.hentSistRegistrertBehandling()

        //TODO: Det som er her nå er FEIL. For MELOSYS-6187, så er det viktig at vi setter korrekt behandlingid i prosessinstansen
        // og ikke saksnummer. I SendManglendeInnbetalingVarselBrev.kt er det ønskelig å bruke behandlingId istf. saksnummer.
        // Husk! Siste registrerte behandling er ikke nødvendigvis den som skal brukes.
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, fagsak.saksnummer)

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
