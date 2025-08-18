package no.nav.melosys.saksflyt.steg.jfr

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OppdaterOgFerdigstillJournalpost(private val joarkFasade: JoarkFasade, private val oppgaveFactory: OppgaveFactory) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPDATER_OG_FERDIGSTILL_JOURNALPOST
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)
        val behandling = prosessinstans.hentBehandling
        val fagsak = behandling.fagsak

        val avsenderID = prosessinstans.getData(ProsessDataKey.AVSENDER_ID)
        val avsenderNavn = utledAvsenderNavn(prosessinstans, avsenderID)

        val journalpostOppdatering = JournalpostOppdatering.Builder()
            .medSaksnummer(fagsak.saksnummer)
            .medBrukerID(prosessinstans.getData(ProsessDataKey.BRUKER_ID))
            .medVirksomhetOrgnr(prosessinstans.getData(ProsessDataKey.VIRKSOMHET_ORGNR))
            .medHovedDokumentID(prosessinstans.getData(ProsessDataKey.DOKUMENT_ID))
            .medTittel(prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL))
            .medMottattDato(prosessinstans.finnData<LocalDate>(ProsessDataKey.MOTTATT_DATO))
            .medTema(oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type).kode)
            .medAvsenderID(avsenderID)
            .medAvsenderNavn(avsenderNavn)
            .medAvsenderType(prosessinstans.finnData<Avsendertyper>(ProsessDataKey.AVSENDER_TYPE))
            .medAvsenderLand(prosessinstans.getData(ProsessDataKey.AVSENDER_LAND))
            .medFysiskeVedlegg(prosessinstans.getFysiskeVedleggTitler())
            .medLogiskeVedleggTitler(prosessinstans.getLogiskeVedleggTitler())
            .build()
        joarkFasade.oppdaterOgFerdigstillJournalpost(journalpostID, journalpostOppdatering)
        log.info("Oppdatert og ferdigstilt journalpost $journalpostID for fagsak: ${fagsak.saksnummer}")
    }

    private fun utledAvsenderNavn(prosessinstans: Prosessinstans, avsenderID: String?): String? {
        var avsenderNavn = prosessinstans.getData(ProsessDataKey.AVSENDER_NAVN)
        val mottakskanalErElektronisk = prosessinstans.getMottakskanalErElektronisk()
        if (avsenderNavn == null && !mottakskanalErElektronisk) {
            if (avsenderID == null) {
                throw FunksjonellException("Både avsenderID og AvsenderNavn er null. AvsenderNavn er påkrevd for å journalføre.")
            }
            avsenderNavn = avsenderID
        }
        return avsenderNavn
    }

    private fun Prosessinstans.getLogiskeVedleggTitler() = this.finnData<List<String>>(
        ProsessDataKey.LOGISKE_VEDLEGG_TITLER, emptyList()
    )

    private fun Prosessinstans.getFysiskeVedleggTitler() = this.finnData<Map<String, String>>(
        ProsessDataKey.FYSISKE_VEDLEGG, emptyMap()
    )

    private fun Prosessinstans.getMottakskanalErElektronisk() = this.finnData<Boolean>(
        ProsessDataKey.MOTTAKSKANAL_ER_ELEKTRONISK, false
    )
}
