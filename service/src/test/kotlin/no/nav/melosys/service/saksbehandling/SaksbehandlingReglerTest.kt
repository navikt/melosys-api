package no.nav.melosys.service.saksbehandling

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
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
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
class SaksbehandlingReglerTest {
    @MockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalIkkeReplikeresData")
    fun tidligereBehandlingSkalIkkeReplikeres(
        sakstype: Sakstyper,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype), behandlingstype, behandlingstema
        )


        result.shouldBe(false)
    }

    private fun tidligereBehandlingSkalIkkeReplikeresData() = listOf(
        arguments(
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.HENVENDELSE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.ENDRET_PERIODE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.HENLEGGELSE
                )
            }
        )
    )

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalReplikeresData")
    fun tidligereBehandlingSkalReplikeres(
        sakstype: Sakstyper,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype), behandlingstype, behandlingstema
        )


        result.shouldBe(true)
    }

    private fun tidligereBehandlingSkalReplikeresData() = listOf(
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.ENDRET_PERIODE,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.AVVIST_KLAGE
                )
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Behandlingstyper.ENDRET_PERIODE,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
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
            }
        ),
    )

    @ParameterizedTest(name = "{0} - {2} - {3} - {4}")
    @MethodSource("behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData")
    fun finnesBehandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyper(
        resultatTypeFraRepo: Behandlingsresultattyper?,
        behandlinger: List<Behandling>,
        expected: Boolean
    ) {
        every { behandlingsresultatRepository.findById(any()) } returns lagBehandlingsresultat(resultatTypeFraRepo)
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository)


        val kanReplikeres =
            saksbehandlingRegler.finnBehandlingSomKanReplikeres(
                behandlinger
            ) != null


        kanReplikeres.shouldBe(expected)
    }

    private fun behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData(): List<Arguments> {
        fun createBehandlinger(behandlingstyper: List<Behandlingstyper>) =
            behandlingstyper.map {
                Behandling().apply {
                    type = it
                    status = Behandlingsstatus.AVSLUTTET
                }
            }

        fun createBehandling(behandlingstype: Behandlingstyper) = createBehandlinger(listOf(behandlingstype))

        return listOf(
            arguments(
                Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
                createBehandling(Behandlingstyper.FØRSTEGANG),
                false
            ),
            arguments(
                null,
                createBehandling(Behandlingstyper.FØRSTEGANG),
                false
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                createBehandling(Behandlingstyper.FØRSTEGANG),
                true
            )
        )
    }

    class BehandlingHolder {
        private val behandlingerMedType: ArrayList<Pair<Behandling, Behandlingsresultattyper?>> = ArrayList()

        fun setup(behandlingsresultatRepository: BehandlingsresultatRepository): SaksbehandlingRegler {
            setupMock { id: Long, behandlingsresultattype: Behandlingsresultattyper? ->
                every { behandlingsresultatRepository.findById(id) } returns lagBehandlingsresultat(
                    behandlingsresultattype
                )
            }
            return SaksbehandlingRegler(behandlingsresultatRepository)
        }

        fun add(
            type: Behandlingstyper,
            tema: Behandlingstema,
            behandlingsresultattype: Behandlingsresultattyper? = null
        ) {
            behandlingerMedType.add(Pair(Behandling().apply {
                this.tema = tema
                this.type = type
                this.status = Behandlingsstatus.AVSLUTTET
            }, behandlingsresultattype))
        }

        fun lagFagsak(type: Sakstyper) =
            Fagsak().apply {
                this.type = type
                this.behandlinger = behandlinger(this).map { it.first }
            }

        override fun toString(): String {
            return behandlingerMedType.map { (behandling, behandlingsresultattyper) ->
                "(${behandling.type}, ${behandling.tema}) - ${behandlingsresultattyper ?: "ikke i db"}"
            }.toString()
        }

        private fun behandlinger(fagsak: Fagsak): List<Pair<Behandling, Behandlingsresultattyper?>> {
            val date = LocalDate.of(2000, 1, 1).atStartOfDay()
            return behandlingerMedType.mapIndexed { index, it ->
                it.first.id = index.toLong()
                it.first.registrertDato = date.plusDays(index.toLong()).toInstant(ZoneOffset.UTC)
                it.first.fagsak = fagsak
                it
            }
        }

        private fun setupMock(function: (Long, Behandlingsresultattyper?) -> MockKAdditionalAnswerScope<Optional<Behandlingsresultat>, Optional<Behandlingsresultat>>) {
            var i: Long = 0
            behandlingerMedType.forEach {
                function(i++, it.second)
            }

        }
    }
}

fun lagBehandlingsresultat(resultatTypeFraRepo: Behandlingsresultattyper?): Optional<Behandlingsresultat> {
    if (resultatTypeFraRepo == null) return Optional.empty()
    return Optional.of(Behandlingsresultat().apply {
        type = resultatTypeFraRepo
    })
}

