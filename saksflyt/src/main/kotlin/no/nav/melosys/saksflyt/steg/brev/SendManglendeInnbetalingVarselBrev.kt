package no.nav.melosys.saksflyt.steg.brev

import no.nav.melosys.domain.brev.TrygdeavgiftBetalingsfrist
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.sak.FagsakService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SendManglendeInnbetalingVarselBrev(
    @Autowired private val dokumentServiceFasade: DokumentServiceFasade,
    @Autowired private val fagsakService: FagsakService,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
        val fagsak = fagsakService.hentFagsak(saksnummer!!)

        val betalingsstatus = prosessinstans.getData(ProsessDataKey.BETALINGSSTATUS, Betalingsstatus::class.java)
        val fakturanummer = prosessinstans.getData(ProsessDataKey.FAKTURANUMMER)
        val behandlingID = fagsak.hentSistRegistrertBehandling().id

        val brevbestillingDto = BrevbestillingDto()
            .apply {
                this.betalingsstatus = betalingsstatus
                this.fakturanummer = fakturanummer
                this.produserbardokument = Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING
                this.mottaker = Mottakerroller.BRUKER
                this.betalingsfrist =  TrygdeavgiftBetalingsfrist.beregnTrygdeavgiftBetalingsfrist(LocalDate.now())
            }

        dokumentServiceFasade.produserDokument(behandlingID, brevbestillingDto)
    }
}
