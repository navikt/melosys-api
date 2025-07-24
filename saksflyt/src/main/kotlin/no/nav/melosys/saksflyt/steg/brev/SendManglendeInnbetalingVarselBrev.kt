package no.nav.melosys.saksflyt.steg.brev

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
class SendManglendeInnbetalingVarselBrev(
    @Autowired private val dokumentServiceFasade: DokumentServiceFasade,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
) : StegBehandler {

    val TRYGDEAVGIFT_BETALINGSFRIST_UKER = 4

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val betalingsstatus = prosessinstans.getData(ProsessDataKey.BETALINGSSTATUS, Betalingsstatus::class.java)
        val mottaksDato = prosessinstans.getData(ProsessDataKey.MOTTATT_DATO, LocalDate::class.java)
        val fakturanummer = prosessinstans.getData(ProsessDataKey.FAKTURANUMMER)
        val fullmektigForBetaling = finnFullmektigTrygdeavgift(behandling.fagsak, behandling.id)

        val brevbestillingDto = BrevbestillingDto()
            .apply {
                this.betalingsstatus = betalingsstatus
                this.fakturanummer = fakturanummer
                this.produserbardokument = Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING
                this.mottaker = Mottakerroller.BRUKER
                this.betalingsfrist = mottaksDato.plus(Period.ofWeeks(TRYGDEAVGIFT_BETALINGSFRIST_UKER))
                this.fullmektigForBetaling = fullmektigForBetaling
            }

        dokumentServiceFasade.produserDokument(behandling.id, brevbestillingDto)
    }

    private fun finnFullmektigTrygdeavgift(fagsak: Fagsak, behandlingID: Long): String? {
        if (fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)
    }
}
