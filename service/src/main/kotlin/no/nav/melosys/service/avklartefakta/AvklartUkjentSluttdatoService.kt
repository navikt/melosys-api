package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype
import org.springframework.stereotype.Service

@Service
class AvklartUkjentSluttdatoService(private val avklartefaktaService: AvklartefaktaService) {
    fun lagreUkjentSluttdatoSomAvklartefakta(behandlingID: Long, ukjentSluttdato: Boolean) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.UKJENT_SLUTTDATO)
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.UKJENT_SLUTTDATO,
            Avklartefaktatyper.UKJENT_SLUTTDATO.kode,
            null,
            ukjentSluttdato.toString()
        )
    }

    fun hentUkjentSluttdato(behandlingID: Long): Boolean? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { Avklartefaktatyper.UKJENT_SLUTTDATO.kode == it.referanse && Avklartefaktatyper.UKJENT_SLUTTDATO == it.avklartefaktaType }
            .map { it.fakta.single().toBoolean() }
            .firstOrNull()
    }
}
