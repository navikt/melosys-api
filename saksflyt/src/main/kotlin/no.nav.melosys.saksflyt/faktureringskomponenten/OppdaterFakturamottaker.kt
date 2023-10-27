package no.nav.melosys.saksflyt.faktureringskomponenten

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Fullmaktstype

import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaMottakerDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OppdaterFakturamottaker(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPDATER_FAKTURAMOTTAKER
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.MELOSYS_OPPDATER_FAKTURAMOTTAKER)) {
            return
        }
        val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val fakturaserieReferanserPåSak = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()
            .map { hentFakturaserieReferanse(it.id) }
            .filterNotNull()

        if (fakturaserieReferanserPåSak.isEmpty()) {
            return
        }

        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).orElse(null)
        val sisteFakturaserieReferanse = fakturaserieReferanserPåSak.first()

        log.info("Oppdaterer fakturamottaker for fakturaserie : $sisteFakturaserieReferanse")

        faktureringskomponentenConsumer.oppdaterFakturaMottaker(
            sisteFakturaserieReferanse,
            FakturaMottakerDto(FullmektigDto(fullmektig))
        )
    }

    private fun hentFakturaserieReferanse(behandlingID: Long): String? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        return behandlingsresultat.fakturaserieReferanse
    }
}
