package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG
import no.nav.melosys.domain.kodeverk.ManglendeInnbetalingHandlingsvalg
import no.nav.melosys.service.behandling.ReplikerBehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManglendeInnbetalingHandlingsvalgService(
    private val avklartefaktaService: AvklartefaktaService,
    private val replikerBehandlingsresultatService: ReplikerBehandlingsresultatService,
) {

    fun hentFullstendigManglendeInnbetaling(behandlingID: Long): Boolean? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { FULLSTENDIG_MANGLENDE_INNBETALING.kode == it.referanse && FULLSTENDIG_MANGLENDE_INNBETALING == it.avklartefaktaType }
            .map { it.fakta.single().toBoolean() }
            .firstOrNull()
    }

    fun lagreFullstendigManglendeInnbetalingSomAvklartFakta(behandlingID: Long, fullstendigManglendeInnbetaling: Boolean) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING)

        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING, FULLSTENDIG_MANGLENDE_INNBETALING.kode,
            null, fullstendigManglendeInnbetaling.toString().uppercase()
        )
    }

    fun hentManglendeInnbetalingHandlingsvalg(behandlingID: Long): ManglendeInnbetalingHandlingsvalg? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { MANGLENDE_INNBETALING_HANDLINGSVALG.kode == it.referanse && MANGLENDE_INNBETALING_HANDLINGSVALG == it.avklartefaktaType }
            .map { ManglendeInnbetalingHandlingsvalg.valueOf(it.fakta.single()) }
            .firstOrNull()
    }

    @Transactional
    fun lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta(behandlingID: Long, manglendeInnbetalingHandlingsvalg: ManglendeInnbetalingHandlingsvalg) {
        if (hentManglendeInnbetalingHandlingsvalg(behandlingID) == manglendeInnbetalingHandlingsvalg) return

        replikerBehandlingsresultatService.tilbakestillBehandlingsresultat(behandlingID)

        avklartefaktaService.slettAvklarteFakta(behandlingID, MANGLENDE_INNBETALING_HANDLINGSVALG)

        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID, MANGLENDE_INNBETALING_HANDLINGSVALG, MANGLENDE_INNBETALING_HANDLINGSVALG.kode,
            null, manglendeInnbetalingHandlingsvalg.kode
        )
    }
}
