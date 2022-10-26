package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Set

internal class BehandlingTest {
    @Test
    fun erAktiv_underBehandling_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.VURDER_DOKUMENT
        }
        behandling.erAktiv().shouldBe(true)
    }

    @Test
    fun erAktiv_avsluttet_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erAktiv().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erUnderBehandling_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun erRedigerbar_erAvsluttet_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erMidlertidigLovvalgsbeslutning_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erIverksetterVedtak_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.IVERKSETTER_VEDTAK
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendt_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendtIkkeYrkesaktiv_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
            tema = Behandlingstema.IKKE_YRKESAKTIV
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun utledBehandlingsfrist_8Uker() {
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling.fagsak,
            behandling
        )
        behandlingsfrist.shouldBe(LocalDate.now().plusWeeks(8))
    }

    @Test
    fun utledBehandlingsfrist_70dager() {
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.KLAGE
        }

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling.fagsak,
            behandling
        )
        behandlingsfrist.shouldBe(LocalDate.now().plusDays(70))
    }

    @Test
    fun utledBehandlingsfrist_70dager_med_mottattDato() {
        val utgangspunktDato = LocalDate.now().minusDays(5)

        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            behandlingsgrunnlag = Behandlingsgrunnlag().apply {
                mottaksdato = utgangspunktDato
            }
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.KLAGE
        }

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling.fagsak,
            behandling
        )
        behandlingsfrist.shouldBe(utgangspunktDato.plusDays(70))
    }

    @Test
    fun utledBehandlingsfrist_90dager() {
        val behandling_soknadsbehandlinger = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_anmodning_unntak = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_attester_fra_andre_trygdeavtaleland = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.REGISTRERING_UNNTAK
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_henvendelser = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.HENVENDELSE
        }

        val behandlingsfrist_soknadsbehandlinger = Behandling.utledBehandlingsfrist(
            behandling_soknadsbehandlinger.fagsak,
            behandling_soknadsbehandlinger
        )
        val behandlingsfrist_anmodning_unntak = Behandling.utledBehandlingsfrist(
            behandling_anmodning_unntak.fagsak,
            behandling_anmodning_unntak
        )
        val behandlingsfrist_attester_fra_andre_trygdeavtaleland = Behandling.utledBehandlingsfrist(
            behandling_attester_fra_andre_trygdeavtaleland.fagsak,
            behandling_attester_fra_andre_trygdeavtaleland
        )
        val behandlingsfrist_henvendelser = Behandling.utledBehandlingsfrist(
            behandling_henvendelser.fagsak,
            behandling_henvendelser
        )

        behandlingsfrist_soknadsbehandlinger.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_anmodning_unntak.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_attester_fra_andre_trygdeavtaleland.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_henvendelser.shouldBe(LocalDate.now().plusDays(90))
    }

    @Test
    fun utledBehandlingsfrist_180dager() {
        val behandling_utstasjonering = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.UNNTAK
            }
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            type = Behandlingstyper.NY_VURDERING
        }
        val behandling_ovrige = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.UNNTAK
            }
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            type = Behandlingstyper.NY_VURDERING
        }
        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling_utstasjonering.fagsak,
            behandling_utstasjonering
        )
        val behandlingsfrist_ovrige = Behandling.utledBehandlingsfrist(
            behandling_ovrige.fagsak,
            behandling_ovrige
        )

        behandlingsfrist.shouldBe(LocalDate.now().plusDays(180))
        behandlingsfrist_ovrige.shouldBe(LocalDate.now().plusDays(180))
    }

    @Test
    fun utledBehandlingsfrist_180dager_med_mottattDato() {
        val utgangspunktDato = LocalDate.now().minusDays(5)

        val behandling_utstasjonering = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.UNNTAK
            }
            behandlingsgrunnlag = Behandlingsgrunnlag().apply {
                mottaksdato = utgangspunktDato
            }
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            type = Behandlingstyper.NY_VURDERING
        }
        val behandling_ovrige = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.UNNTAK
            }
            behandlingsgrunnlag = Behandlingsgrunnlag().apply {
                mottaksdato = utgangspunktDato
            }
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            type = Behandlingstyper.NY_VURDERING
        }
        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling_utstasjonering.fagsak,
            behandling_utstasjonering
        )
        val behandlingsfrist_ovrige = Behandling.utledBehandlingsfrist(
            behandling_ovrige.fagsak,
            behandling_ovrige
        )

        behandlingsfrist.shouldBe(utgangspunktDato.plusDays(180))
        behandlingsfrist_ovrige.shouldBe(utgangspunktDato.plusDays(180))
    }

    @Test
    fun saksopplysningerEksistererIkke_eksisterer_false() {
        val behandling = Behandling().apply {
            saksopplysninger = setOf(
                Saksopplysning().apply {
                    type = SaksopplysningType.PERSHIST
                }, Saksopplysning().apply {
                    type = SaksopplysningType.PERSOPL
                })
        }
        behandling.manglerSaksopplysningerAvType(
            listOf(
                SaksopplysningType.PDL_PERSOPL,
                SaksopplysningType.PERSOPL
            )
        ).shouldBe(false)
    }

    @Test
    fun saksopplysningerEksistererIkke_eksistererIkke_true() {
        val behandling = Behandling().apply {
            saksopplysninger = Set.of(
                Saksopplysning().apply {
                    type = SaksopplysningType.PDL_PERSOPL
                }
            )
        }
        behandling.manglerSaksopplysningerAvType(
            listOf(
                SaksopplysningType.PDL_PERS_SAKS,
                SaksopplysningType.PERSHIST
            )
        ).shouldBe(true)
    }
}
