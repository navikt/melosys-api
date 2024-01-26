package no.nav.melosys.tjenester.gui.dto.trygdeavtale

import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.Relasjonsrolle
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@JvmRecord
data class TrygdeavtaleResultatDto(

    @JvmField val virksomhet: String?,
    @JvmField val bestemmelse: String?,
    val tilleggsbestemmelse: String?,
    @JvmField val lovvalgsperiodeFom: LocalDate?,
    @JvmField val lovvalgsperiodeTom: LocalDate?,
    val barn: List<MedfolgendeFamilieDto?> = listOf(),
    val ektefelle: MedfolgendeFamilieDto? = null
) {
    fun til(): TrygdeavtaleResultat {
        return TrygdeavtaleResultat.Builder()
            .familie(lagAvklarteMedfolgendeFamilie())
            .bestemmelse(bestemmelse)
            .tilleggsbestemmelse(tilleggsbestemmelse)
            .virksomhet(virksomhet)
            .lovvalgsperiodeFom(lovvalgsperiodeFom)
            .lovvalgsperiodeTom(lovvalgsperiodeTom)
            .build()
    }

    private fun lagAvklarteMedfolgendeFamilie(): AvklarteMedfolgendeFamilie {
        val omfattetFamilie = barn.asSequence()
            .plus(ektefelle)
            .filterNotNull()
            .filter(MedfolgendeFamilieDto::erOmfattet)
            .map { OmfattetFamilie(it.uuid) }
            .toSet()

        val ikkeOmfattetFamilie = barn.asSequence()
            .plus(ektefelle)
            .filterNotNull()
            .filter(MedfolgendeFamilieDto::erIkkeOmfattet)
            .map { IkkeOmfattetFamilie(it.uuid, it.begrunnelseKode, it.begrunnelseFritekst) }
            .toSet()

        return AvklarteMedfolgendeFamilie(omfattetFamilie, ikkeOmfattetFamilie)
    }

    class Builder {
        private var virksomhet: String? = null
        private var bestemmelse: String? = null
        private var tilleggsbestemmelse: String? = null
        private var lovvalgsperiodeFom: LocalDate? = null
        private var lovvalgsperiodeTom: LocalDate? = null
        private val barn: MutableList<MedfolgendeFamilieDto?> = ArrayList()
        private var ektefelle: MedfolgendeFamilieDto? = null

        fun virksomhet(virksomhet: String?): Builder {
            this.virksomhet = virksomhet
            return this
        }

        fun bestemmelse(bestemmelse: String?): Builder {
            this.bestemmelse = bestemmelse
            return this
        }

        fun tilleggsbestemmelse(tilleggsbestemmelse: String?): Builder {
            this.tilleggsbestemmelse = tilleggsbestemmelse
            return this
        }

        fun lovvalgsperiodeFom(lovvalgsperiodeFom: LocalDate?): Builder {
            this.lovvalgsperiodeFom = lovvalgsperiodeFom
            return this
        }

        fun lovvalgsperiodeTom(lovvalgsperiodeTom: LocalDate?): Builder {
            this.lovvalgsperiodeTom = lovvalgsperiodeTom
            return this
        }

        fun addBarn(uuid: String?, omfattet: Boolean, begrunnelseKode: String?, begrunnelseFritekst: String?): Builder {
            barn.add(MedfolgendeFamilieDto(uuid, omfattet, begrunnelseKode, begrunnelseFritekst))
            return this
        }

        fun barn(barn: List<MedfolgendeFamilieDto?>?): Builder {
            this.barn.addAll(barn!!)
            return this
        }

        fun ektefelle(
            uuid: String?,
            omfattet: Boolean,
            begrunnelseKode: String?,
            begrunnelseFritekst: String?
        ): Builder {
            this.ektefelle = MedfolgendeFamilieDto(uuid, omfattet, begrunnelseKode, begrunnelseFritekst)
            return this
        }

        fun ektefelle(ektefelle: MedfolgendeFamilieDto?): Builder {
            this.ektefelle = ektefelle
            return this
        }

        fun build(): TrygdeavtaleResultatDto {
            return TrygdeavtaleResultatDto(
                virksomhet,
                bestemmelse,
                tilleggsbestemmelse,
                lovvalgsperiodeFom,
                lovvalgsperiodeTom,
                barn,
                ektefelle
            )
        }
    }

    companion object {
        @JvmStatic
        fun fra(resultat: TrygdeavtaleResultat, familie: List<MedfolgendeFamilie>): TrygdeavtaleResultatDto {
            val ektefelle = familie.stream()
                .filter { mf: MedfolgendeFamilie -> mf.relasjonsrolle == Relasjonsrolle.EKTEFELLE_SAMBOER }
                .map { mf: MedfolgendeFamilie -> tilMedfolgendeFamilieDto(resultat, mf.uuid) }
                .flatMap { obj: List<MedfolgendeFamilieDto?> -> obj.stream() }
                .findFirst().orElse(null)

            val barn = familie.stream().filter { mf: MedfolgendeFamilie -> mf.relasjonsrolle == Relasjonsrolle.BARN }
                .map { mf: MedfolgendeFamilie -> tilMedfolgendeFamilieDto(resultat, mf.uuid) }
                .flatMap { obj: List<MedfolgendeFamilieDto?> -> obj.stream() }
                .toList()

            return Builder()
                .ektefelle(ektefelle)
                .barn(barn)
                .bestemmelse(resultat.bestemmelse)
                .tilleggsbestemmelse(resultat.tilleggsbestemmelse)
                .virksomhet(resultat.virksomhet)
                .lovvalgsperiodeFom(resultat.lovvalgsperiodeFom)
                .lovvalgsperiodeTom(resultat.lovvalgsperiodeTom)
                .build()
        }

        private fun tilMedfolgendeFamilieDto(
            resultat: TrygdeavtaleResultat,
            uuid: String
        ): List<MedfolgendeFamilieDto?> {
            return Stream.concat(
                resultat.familie.familieOmfattetAvNorskTrygd
                    .stream().filter { omfattet: OmfattetFamilie -> omfattet.uuid == uuid }
                    .map { omfattet: OmfattetFamilie? -> MedfolgendeFamilieDto(uuid, true, null, null) },
                resultat.familie.familieIkkeOmfattetAvNorskTrygd
                    .stream().filter { ikkeOmfattet: IkkeOmfattetFamilie -> ikkeOmfattet.uuid == uuid }
                    .map { ikkeOmfattet: IkkeOmfattetFamilie ->
                        MedfolgendeFamilieDto(
                            uuid,
                            false,
                            ikkeOmfattet.begrunnelse,
                            ikkeOmfattet.begrunnelseFritekst
                        )
                    }
            ).toList()
        }
    }
}
