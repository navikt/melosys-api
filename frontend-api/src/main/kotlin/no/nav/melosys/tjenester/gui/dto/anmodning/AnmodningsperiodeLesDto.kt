package no.nav.melosys.tjenester.gui.dto.anmodning

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto

class AnmodningsperiodeLesDto : AnmodningsperiodeSkrivDto {
    @JvmField
    val sendtUtland: Boolean

    private constructor(
        id: String,
        periodeDto: PeriodeDto,
        bestemmelse: LovvalgBestemmelse,
        tilleggsbestemmelse: LovvalgBestemmelse,
        lovvalgsland: Land_iso2,
        unntakFraBestemmelse: LovvalgBestemmelse,
        unntakFraLovvalgsland: Land_iso2,
        trygdedekning: Trygdedekninger,
        medlemskapsperiodeID: String?,
        sendtUtland: Boolean
    ) : super(
        id, periodeDto, bestemmelse, tilleggsbestemmelse, lovvalgsland, unntakFraBestemmelse,
        unntakFraLovvalgsland, trygdedekning, medlemskapsperiodeID
    ) {
        this.sendtUtland = sendtUtland
    }

    companion object {
        @JvmStatic
        fun av(anmodningsperiode: Anmodningsperiode): AnmodningsperiodeLesDto {
            return AnmodningsperiodeLesDto(
                anmodningsperiode.id.toString(),
                PeriodeDto(anmodningsperiode.fom, anmodningsperiode.tom),
                anmodningsperiode.bestemmelse,
                anmodningsperiode.tilleggsbestemmelse,
                anmodningsperiode.lovvalgsland,
                anmodningsperiode.unntakFraBestemmelse,
                anmodningsperiode.unntakFraLovvalgsland,
                anmodningsperiode.dekning,
                if (anmodningsperiode.medlPeriodeID != null) anmodningsperiode.medlPeriodeID.toString() else null,
                anmodningsperiode.erSendtUtland()
            )
        }
    }
}
