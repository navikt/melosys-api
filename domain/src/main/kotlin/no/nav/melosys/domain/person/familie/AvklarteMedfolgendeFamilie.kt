package no.nav.melosys.domain.person.familie

import java.util.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER

class AvklarteMedfolgendeFamilie(
    val familieOmfattetAvNorskTrygd: Set<OmfattetFamilie>,
    val familieIkkeOmfattetAvNorskTrygd: Set<IkkeOmfattetFamilie>
) {

    fun hentBegrunnelseFritekst(): Optional<String> =
        familieIkkeOmfattetAvNorskTrygd.stream()
            .map { it.begrunnelseFritekst }
            .filter(Objects::nonNull)
            .map { it!! }
            .findFirst()

    fun tilAvklartefakta(uuidOgRolle: Map<String, MedfolgendeFamilie.Relasjonsrolle>): Collection<Avklartefakta> {
        val avklartefakta = mutableSetOf<Avklartefakta>()

        familieOmfattetAvNorskTrygd.forEach { omfattet ->
            avklartefakta.add(lagAvklarteFakta(omfattet.uuid, tilAvklartefaktaTyper(uuidOgRolle[omfattet.uuid])))
        }

        familieIkkeOmfattetAvNorskTrygd.forEach { ikkeOmfattet ->
            avklartefakta.add(
                lagAvklarteFakta(
                    ikkeOmfattet.uuid,
                    tilAvklartefaktaTyper(uuidOgRolle[ikkeOmfattet.uuid]),
                    ikkeOmfattet.begrunnelse,
                    ikkeOmfattet.begrunnelseFritekst
                )
            )
        }

        return avklartefakta
    }

    private fun lagAvklarteFakta(subjekt: String, type: Avklartefaktatyper?): Avklartefakta {
        val avklartefakta = Avklartefakta()
        avklartefakta.referanse = type?.kode
        avklartefakta.type = type
        avklartefakta.fakta = Avklartefakta.VALGT_FAKTA
        avklartefakta.subjekt = subjekt

        return avklartefakta
    }

    private fun lagAvklarteFakta(
        subjekt: String,
        type: Avklartefaktatyper?,
        begrunnelseKode: String?,
        begrunnelseFritekst: String?
    ): Avklartefakta {
        val avklartefakta = Avklartefakta()
        avklartefakta.referanse = type?.kode
        avklartefakta.type = type
        avklartefakta.fakta = Avklartefakta.IKKE_VALGT_FAKTA
        avklartefakta.subjekt = subjekt
        avklartefakta.begrunnelseFritekst = begrunnelseFritekst

        val registrering = AvklartefaktaRegistrering()
        registrering.begrunnelseKode = begrunnelseKode
        registrering.avklartefakta = avklartefakta

        avklartefakta.registreringer.add(registrering)

        return avklartefakta
    }

    private fun tilAvklartefaktaTyper(relasjonsrolle: MedfolgendeFamilie.Relasjonsrolle?): Avklartefaktatyper? =
        when (relasjonsrolle) {
            MedfolgendeFamilie.Relasjonsrolle.BARN -> VURDERING_LOVVALG_BARN
            else -> VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER
        }

    fun finnes(): Boolean = !(familieOmfattetAvNorskTrygd.isEmpty() && familieIkkeOmfattetAvNorskTrygd.isEmpty())
}