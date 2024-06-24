package no.nav.melosys.service.eessi

import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedInformasjon
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.eessi.ruting.AdminSedRuter
import no.nav.melosys.service.eessi.ruting.SedRuterForSedTyper
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class AdminInnvalideringSedRuter(
    fagsakService: FagsakService,
    prosessinstansService: ProsessinstansService,
    private val oppgaveService: OppgaveService,
    behandlingsresultatService: BehandlingsresultatService,
    medlPeriodeService: MedlPeriodeService,
    private val eessiService: EessiService,
    private val behandlingService: BehandlingService
) : AdminSedRuter(fagsakService, behandlingsresultatService, medlPeriodeService, prosessinstansService), SedRuterForSedTyper {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AdminInnvalideringSedRuter::class.java)
    }

    override fun gjelderSedTyper(): Collection<SedType> {
        return setOf(SedType.X008)
    }

    override fun rutSedTilBehandling(prosessinstans: Prosessinstans, arkivsakID: Long?) {
        val melosysEessiMelding = prosessinstans.hentMelosysEessiMelding()
        val fagsak = hentFagsakDersomArkivsakIDEksisterer(arkivsakID)

        if (fagsak.isEmpty) {
            log.info("Oppretter jfr-oppgave for SED {} i RINA-sak {}", melosysEessiMelding.sedId, melosysEessiMelding.rinaSaksnummer)
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.journalpostId, melosysEessiMelding.aktoerId)
            return
        }

        val sistAktiveBehandling = fagsak.get().hentSistAktivBehandlingIkkeÅrsavregning()

        if (sistAktiveBehandling.erNorgeUtpekt()) {
            if (sistAktiveBehandling.erAktiv()) {
                behandlingService.endreStatus(sistAktiveBehandling.id, Behandlingsstatus.VURDER_DOKUMENT)
                opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling)
                return
            } else {
                oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.journalpostId, melosysEessiMelding.aktoerId)
                return
            }
        }

        val sedDokument = sistAktiveBehandling.finnSedDokument()

        val aktivBehandlingErInvalidert = erAktivBehandlingInvalidert(sedDokument, arkivsakID)

        if (behandlingSkalAnnuleres(melosysEessiMelding, sedDokument.get())
            || aktivBehandlingErInvalidert && (sistAktiveBehandling.erRegisteringAvUnntak() || sistAktiveBehandling.erAnmodningOmUnntak())
        ) {
            annullerSakOgBehandling(sistAktiveBehandling)
            behandlingsresultatService.oppdaterBehandlingsresultattype(sistAktiveBehandling.id, Behandlingsresultattyper.HENLEGGELSE)
        } else {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(sistAktiveBehandling, melosysEessiMelding.journalpostId, melosysEessiMelding.aktoerId, null)
        }
        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling)
    }

    private fun behandlingSkalAnnuleres(melosysEessiMelding: MelosysEessiMelding, sistAktiveSedDokument: SedDokument): Boolean {
        return melosysEessiMelding.sedType == SedType.X008.toString() && sistAktiveSedDokument.sedType == SedType.A003
    }

    private fun erAktivBehandlingInvalidert(sedDokument: Optional<SedDokument>, arkivsakID: Long?): Boolean {
        if (arkivsakID == null) {
            return false
        }
        return sedDokument.filter { dokument ->
            eessiService.hentTilknyttedeBucer(arkivsakID, listOf())
                .stream()
                .filter { b -> b.id == dokument.rinaSaksnummer }
                .flatMap { b -> b.seder.stream() }
                .filter { s -> s.sedId == dokument.rinaDokumentID }
                .anyMatch(SedInformasjon::erAvbrutt)
        }.isPresent
    }
}
