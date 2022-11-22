package no.nav.melosys.service.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
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
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaksbehandlingReglerTest {
    @MockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    private val unleash = FakeUnleash();

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalIkkeReplikeresData")
    fun tidligereBehandlingSkalIkkeReplikeres(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype, sakstema), behandlingstype, behandlingstema
        )


        result.shouldBe(false)
    }

    private fun tidligereBehandlingSkalIkkeReplikeresData() = listOf(
        arguments(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.HENVENDELSE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.ENDRET_PERIODE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.HENLEGGELSE
                )
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstema.YRKESAKTIV,
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
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
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
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                )
            }
        ),
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV)
            }
        ),
    )

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalReplikeresData")
    fun tidligereBehandlingSkalReplikeres(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype, sakstema), behandlingstype, behandlingstema
        )


        result.shouldBe(true)
    }

    private fun tidligereBehandlingSkalReplikeresData() = listOf(
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
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
            Sakstemaer.MEDLEMSKAP_LOVVALG,
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
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingsresultattyper.AVVIST_KLAGE
                )
            }
        ),
    )

    @ParameterizedTest(name = "behandlingsresultatType:{0} - FunnetBehandlingID:{2}")
    @MethodSource("behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData")
    fun finnBehandlingSomKanReplikeres_forventetBehandling(
        resultatTypeFraRepo: Behandlingsresultattyper?,
        behandlinger: List<Behandling>,
        expectedBehandlingID: Long?
    ) {
        every { behandlingsresultatRepository.findById(any()) } returns lagBehandlingsresultat(resultatTypeFraRepo)
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)

        val behandling = saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandlinger)


        behandling?.id.shouldBe(expectedBehandlingID)
    }

    private fun behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData(): List<Arguments> {
        val behandlingFørstegangAvsluttet = Behandling().apply {
            id = 0
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            status = Behandlingsstatus.AVSLUTTET
            fagsak = Fagsak().apply {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }

        return listOf(
            arguments(
                Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
                listOf(behandlingFørstegangAvsluttet),
                null
            ),
            arguments(
                null,
                listOf(behandlingFørstegangAvsluttet),
                null
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.ANKE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    }
                ),
                null
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(behandlingFørstegangAvsluttet),
                0L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.ANKE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    },
                    Behandling().apply {
                        id = 1
                        type = Behandlingstyper.FØRSTEGANG
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    }
                ),
                1L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.FØRSTEGANG
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.UNDER_BEHANDLING
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    },
                    Behandling().apply {
                        id = 1
                        type = Behandlingstyper.ANKE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    },
                    Behandling().apply {
                        id = 2
                        type = Behandlingstyper.NY_VURDERING
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = Fagsak().apply {
                            type = Sakstyper.EU_EOS
                            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                        }
                    }
                ),
                2L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(Behandling().apply {
                    id = 0
                    type = Behandlingstyper.HENVENDELSE
                    tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = Fagsak().apply {
                        type = Sakstyper.EU_EOS
                        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                    }
                }),
                null
            ),
        )
    }

    class BehandlingHolder {
        private val behandlingerMedType: ArrayList<Pair<Behandling, Behandlingsresultattyper?>> = ArrayList()
        private val unleash = FakeUnleash();

        fun setup(behandlingsresultatRepository: BehandlingsresultatRepository): SaksbehandlingRegler {
            unleash.enableAll()
            setupMock { id: Long, behandlingsresultattype: Behandlingsresultattyper? ->
                every { behandlingsresultatRepository.findById(id) } returns lagBehandlingsresultat(
                    behandlingsresultattype
                )
            }
            return SaksbehandlingRegler(behandlingsresultatRepository, unleash)
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

        fun lagFagsak(type: Sakstyper, tema: Sakstemaer) =
            Fagsak().apply {
                this.type = type
                this.tema = tema
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

