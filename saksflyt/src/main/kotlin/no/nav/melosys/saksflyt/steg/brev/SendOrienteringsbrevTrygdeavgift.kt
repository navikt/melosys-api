package no.nav.melosys.saksflyt.steg.brev

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SendOrienteringsbrevTrygdeavgift(
    @Autowired private val dokumentServiceFasade: DokumentServiceFasade,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_ORIENTERINGSBREV_TRYGDEAVGIFT
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val fullmektigForBetaling = hentFakturamottaker(behandling.fagsak, behandling.id)

        val brevbestillingDto = BrevbestillingDto()
            .apply {
                this.produserbardokument = Produserbaredokumenter.TRYGDEAVGIFT_INFORMASJONSBREV
                this.mottaker = Mottakerroller.BRUKER
                this.fullmektigForBetaling = fullmektigForBetaling
            }

        dokumentServiceFasade.produserDokument(behandling.id, brevbestillingDto)
    }

    private fun hentFakturamottaker(fagsak: Fagsak, behandlingID: Long): String? {
        if (fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)
    }
}
