package no.nav.melosys.saksflyt.steg.brev

import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.ftrl.Betalingsstatus
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SendManglendeInnbetalingVarselBrev(
    @Autowired private val brevBestiller: BrevBestiller,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV
    }

    override fun utfør(prosessinstans: Prosessinstans?) {
        val fakturaserieReferanse = prosessinstans?.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)
        val betalingsstatus = prosessinstans?.getData(ProsessDataKey.BETALINGSSTATUS)
        val datoFakturaBestilt = prosessinstans?.getData(ProsessDataKey.DATO_FAKTURA_BESTILT)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultatAvFakturaserieReferanse(fakturaserieReferanse)
        val mottakere = mutableListOf<Mottaker>()
        val fagsak = behandlingsresultat.behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).orElse(null)
        // TODO: Her burde vi ha felles funksjon for å hente fakturamottaker.
        if (fullmektig != null) {
            mottakere.add(Mottaker.av(fullmektig))
        } else {
            mottakere.add(Mottaker.medRolle(Mottakerroller.BRUKER).apply { aktørId = fagsak.hentBrukersAktørID() })
        }
        brevBestiller.bestillVarselbrevManglendeInnbetaling(
            mottakere,
            LocalDate.parse(datoFakturaBestilt!!),
            Betalingsstatus.valueOf(betalingsstatus!!),
            behandlingsresultat.behandling.fagsak.saksnummer,
            behandlingsresultat.id
        )
    }
}
