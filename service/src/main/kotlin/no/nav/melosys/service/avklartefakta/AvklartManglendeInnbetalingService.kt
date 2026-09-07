package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.MANGLENDE_INNBETALING_VURDERING
import no.nav.melosys.domain.kodeverk.ManglendeInnbetalingVurdering
import org.springframework.stereotype.Service

@Service
class AvklartManglendeInnbetalingService(private val avklartefaktaService: AvklartefaktaService) {

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

    fun hentManglendeInnbetalingVurdering(behandlingID: Long): ManglendeInnbetalingVurdering? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { MANGLENDE_INNBETALING_VURDERING.kode == it.referanse && MANGLENDE_INNBETALING_VURDERING == it.avklartefaktaType }
            .map { ManglendeInnbetalingVurdering.valueOf(it.fakta.single()) }
            .firstOrNull()
    }

    fun lagreManglendeInnbetalingVurderingSomAvklartFakta(behandlingID: Long, manglendeInnbetalingVurdering: ManglendeInnbetalingVurdering) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, MANGLENDE_INNBETALING_VURDERING)

        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID, MANGLENDE_INNBETALING_VURDERING, MANGLENDE_INNBETALING_VURDERING.kode,
            null, manglendeInnbetalingVurdering.kode
        )
    }
}
