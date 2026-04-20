package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.util.*

data class TrygdeavgiftsgrunnlagDto(
    /**
     * Nullable fordi grunnlaget for en trygdeavgiftsperiode ikke alltid peker på en
     * medlemskapsperiode — det kan også være en lovvalgsperiode eller en
     * helseutgiftDekkesPeriode. DTO-et skiller ikke disse typene eksplisitt;
     * mottaker slår opp UUID-en mot `behandlingsresultat.finnAvgiftspliktigPerioder()`
     * og caster til riktig type.
     */
    val medlemskapsperiodeId: UUID?,
    val skatteforholdsperiodeId: UUID,
    val inntektsperiodeId: UUID,
)
