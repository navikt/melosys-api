package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import java.util.*


data class MedlemskapsperiodeDto(
    val id: UUID,
    val periode: DatoPeriodeDto,
    val avgiftsdekninger: Set<Avgiftsdekning>,
    val medlemskapstype: Medlemskapstyper,
) {
    companion object {
        fun List<Medlemskapsperiode>.tilMedlemskapsperiodeDtos(): Set<MedlemskapsperiodeDto> {
            return map {
                MedlemskapsperiodeDto(
                    it.idToUUID(),
                    DatoPeriodeDto(it.fom, it.tom),
                    AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.trygdedekning),
                    it.medlemskapstype
                )
            }.toSet()
        }

        fun Medlemskapsperiode.idToUUID(): UUID {
            return idToUUid(this.id)
        }

        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
