package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import java.util.*


data class MedlemskapsperiodeDto(
    val id: UUID,
    val periode: DatoPeriodeDto,
    val avgiftsdekninger: Set<Avgiftsdekning>,
    val medlemskapstype: Medlemskapstyper,
) {
    companion object {

        fun Medlemskapsperiode.idToUUID(): UUID {
            return idToUUid(this.id)
        }

        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
