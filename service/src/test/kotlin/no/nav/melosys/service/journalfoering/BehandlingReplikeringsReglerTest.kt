package no.nav.melosys.service.journalfoering

import io.kotest.matchers.shouldBe
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.*
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingReplikeringsReglerTest {
    @MockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @ParameterizedTest(name = "{0} - {1} - {2}")
    @MethodSource("skalTidligereBehandlingReplikeresData")
    fun skalTidligereBehandlingReplikeres(
        sakstype: Sakstyper,
        behandlingHolder: BehandlingHolder,
        result: Boolean
    ) {
        behandlingHolder.setupMock { id: Long, behandlingsresultattype: Behandlingsresultattyper? ->
            every { behandlingsresultatRepository.findById(id) } returns lagBehandlingsresultat(behandlingsresultattype)
        }
        val behandlingReplikeringsRegler = BehandlingReplikeringsRegler(behandlingsresultatRepository)

        val fagsak = Fagsak().apply {
            type = sakstype
            this.behandlinger = behandlingHolder.behandlinger(this).map { it.first }
        }

        behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(fagsak)
            .shouldBe(result)
    }

    private fun skalTidligereBehandlingReplikeresData(): List<Arguments> {
        return listOf(
            arguments(
                Sakstyper.TRYGDEAVTALE,
                BehandlingHolder().apply {
                    add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
                },
                false
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE)
                },
                false
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(Behandlingstyper.HENVENDELSE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
                },
                false
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(Behandlingstyper.ENDRET_PERIODE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
                },
                false
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(
                        Behandlingstyper.ENDRET_PERIODE,
                        Behandlingstema.UTSENDT_ARBEIDSTAKER,
                        Behandlingsresultattyper.HENLEGGELSE
                    )
                },

                false
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(
                        Behandlingstyper.ENDRET_PERIODE,
                        Behandlingstema.UTSENDT_ARBEIDSTAKER,
                        Behandlingsresultattyper.AVVIST_KLAGE
                    )
                },
                true
            ),
            arguments(
                Sakstyper.EU_EOS,
                BehandlingHolder().apply {
                    add(
                        Behandlingstyper.ENDRET_PERIODE,
                        Behandlingstema.UTSENDT_ARBEIDSTAKER,
                        Behandlingsresultattyper.HENLEGGELSE
                    )
                    add(
                        Behandlingstyper.NY_VURDERING,
                        Behandlingstema.UTSENDT_ARBEIDSTAKER,
                        Behandlingsresultattyper.AVVIST_KLAGE
                    )
                },
                true
            ),
        )
    }

    @ParameterizedTest(name = "{0} - {2} - {3} - {4}")
    @MethodSource("behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData")
    fun finnesBehandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyper(
        resultatTypeFraRepo: Behandlingsresultattyper?,
        behandlinger: List<Behandling>,
        typer: List<Behandlingstyper>,
        resultatTyper: List<Behandlingsresultattyper>,
        result: Boolean
    ) {
        every { behandlingsresultatRepository.findById(any()) } returns lagBehandlingsresultat(resultatTypeFraRepo)

        val behandlingReplikeringsRegler = BehandlingReplikeringsRegler(behandlingsresultatRepository)
        behandlingReplikeringsRegler.finnesBehandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyper(
            behandlinger, typer, resultatTyper
        ).shouldBe(result)
    }

    private fun lagBehandlingsresultat(resultatTypeFraRepo: Behandlingsresultattyper?): Optional<Behandlingsresultat> {
        if (resultatTypeFraRepo == null) return Optional.empty()
        return Optional.of(Behandlingsresultat().apply {
            type = resultatTypeFraRepo
        })
    }

    private fun behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData(): List<Arguments> {
        return listOf(
            arguments(
                Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
                listOf(Behandling().apply {
                    type = Behandlingstyper.FØRSTEGANG
                }),
                listOf(Behandlingstyper.FØRSTEGANG),
                listOf(Behandlingsresultattyper.ANMODNING_OM_UNNTAK),
                false
            ),
            arguments(
                null,
                listOf(Behandling().apply {
                    type = Behandlingstyper.FØRSTEGANG
                }),
                listOf(Behandlingstyper.FØRSTEGANG),
                listOf(Behandlingsresultattyper.ANMODNING_OM_UNNTAK),
                false
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(Behandling().apply {
                    type = Behandlingstyper.FØRSTEGANG
                }),
                listOf(Behandlingstyper.FØRSTEGANG),
                listOf(Behandlingsresultattyper.ANMODNING_OM_UNNTAK),
                true
            )
        )
    }

    class BehandlingHolder {
        private val behandling: ArrayList<Pair<Behandling, Behandlingsresultattyper?>> = ArrayList()
        fun add(
            type: Behandlingstyper,
            tema: Behandlingstema,
            behandlingsresultattype: Behandlingsresultattyper? = null
        ) {
            behandling.add(Pair(Behandling().apply {
                this.tema = tema
                this.type = type
            }, behandlingsresultattype))
        }

        fun behandlinger(fagsak: Fagsak): List<Pair<Behandling, Behandlingsresultattyper?>> {
            var i: Long = 0
            val date = LocalDate.of(2000, 1, 1).atStartOfDay()
            return behandling.map {
                it.first.id = i++
                it.first.registrertDato = date.plusDays(i).toInstant(ZoneOffset.UTC)
                it.first.fagsak = fagsak
                it
            }
        }

        override fun toString(): String {
            return behandling.map { "(${it.first.type}, ${it.first.tema}) - ${it.second}" }.toString()
        }

        fun setupMock(function: (Long, Behandlingsresultattyper?) -> MockKAdditionalAnswerScope<Optional<Behandlingsresultat>, Optional<Behandlingsresultat>>) {
            var i: Long = 0
            behandling.forEach {
                function(i++, it.second)
            }

        }
    }
}
