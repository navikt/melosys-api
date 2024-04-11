package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.ReplikerBehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.LocalDate

@Import(value = [ReplikerBehandlingsresultatService::class, BehandlingsresultatService::class, SaksbehandlingRegler::class, FakeUnleash::class, VilkaarsresultatService::class])
internal class BehandlingsresultatServiceIT(
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val replikerBehandlingsresultatService: ReplikerBehandlingsresultatService,
    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository
) : DataJpaTestBase() {
    data class Behandlinger(val orginal: Behandling, val replika: Behandling)

    @Test
    fun replikerBehandlingOgBehandlingsresultat_relasjonerBlirRiktigIDb() {
        val behandlinger = lagFagsakMedBehandlinger()

        val behandlingsresultat = lagBehandlingsresultat(behandlinger.orginal)
        behandlingsresultatRepository.save(behandlingsresultat)
        replikerBehandlingsresultatService.replikerBehandlingsresultat(behandlinger.orginal, behandlinger.replika)

        val replikaResultat = behandlingsresultatRepository.findById(behandlinger.replika.id).get()

        listOf(
            replikaResultat.lovvalgsperioder,
            replikaResultat.avklartefakta,
            replikaResultat.vilkaarsresultater,
            replikaResultat.behandlingsresultatBegrunnelser,
            replikaResultat.kontrollresultater,
            replikaResultat.anmodningsperioder,
            replikaResultat.utpekingsperioder
        ).forEach {
            assertThat(it)
                .singleElement()
                .hasFieldOrPropertyWithValue("behandlingsresultat.id", behandlinger.replika.id)
        }
        assertThat(replikaResultat.avklartefakta.flatMap { it.registreringer })
            .singleElement()
            .matches { it.avklartefakta.id == replikaResultat.avklartefakta.first().id }

        assertThat(replikaResultat.vilkaarsresultater.flatMap { it.begrunnelser })
            .singleElement()
            .matches { it.vilkaarsresultat.id == replikaResultat.vilkaarsresultater.first().id }
    }

    private fun lagFagsakMedBehandlinger(): Behandlinger {
        Fagsak().apply {
            saksnummer = "MEL-1001"
            type = Sakstyper.TRYGDEAVTALE
            status = Saksstatuser.LOVVALG_AVKLART
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            leggTilRegisteringInfo()
        }.also { fsak ->
            fagsakRepository.save(fsak)

            val tidligsteInaktiveBehandling = Behandling().apply {
                fagsak = fsak
                leggTilRegisteringInfo()
                behandlingsfrist = LocalDate.now().plusYears(1)
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
            }.also { behandlingRepository.save(it) }

            val behandlingsreplika = Behandling().apply {
                fagsak = fsak
                leggTilRegisteringInfo()
                behandlingsfrist = LocalDate.now().plusYears(1)
                status = Behandlingsstatus.OPPRETTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
            }.also {
                behandlingRepository.save(it)
            }
            return Behandlinger(tidligsteInaktiveBehandling, behandlingsreplika)
        }
    }

    fun lagBehandlingsresultat(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat =
        Behandlingsresultat().apply {
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            leggTilRegisteringInfo()
        }.also { br ->
            br.vedtakMetadata = VedtakMetadata().apply {
                behandlingsresultat = br
                vedtaksdato = Instant.parse("2002-02-11T09:37:30Z")
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                leggTilRegisteringInfo()
            }

            br.avklartefakta.add(
                Avklartefakta().apply {
                    behandlingsresultat = br
                    fakta = "fakta"
                    type = Avklartefaktatyper.ARBEIDSLAND
                    referanse = "referanse"
                }.also {
                    it.registreringer.add(
                        AvklartefaktaRegistrering().apply {
                            avklartefakta = it
                            begrunnelseKode = "AvklartefaktaRegistrering-begrunnelsekode"
                            leggTilRegisteringInfo()
                        })
                })

            br.vilkaarsresultater.add(
                Vilkaarsresultat().apply {
                    behandlingsresultat = br
                    begrunnelseFritekst = "fritekst"
                    begrunnelseFritekstEessi = "free text"
                    vilkaar = Vilkaar.BOSATT_I_NORGE
                    leggTilRegisteringInfo()
                }.also {
                    it.begrunnelser = setOf(VilkaarBegrunnelse().apply {
                        vilkaarsresultat = it
                        kode = "kode"
                        leggTilRegisteringInfo()
                    })
                })

            br.lovvalgsperioder.add(
                Lovvalgsperiode().apply {
                    behandlingsresultat = br
                    dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusMonths(2)
                })

            br.behandlingsresultatBegrunnelser.add(
                BehandlingsresultatBegrunnelse().apply {
                    behandlingsresultat = br
                    kode = "begrunnelsekode"
                }
            )

            br.kontrollresultater.add(
                Kontrollresultat().apply {
                    behandlingsresultat = br
                    begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
                })

            br.anmodningsperioder.add(
                Anmodningsperiode().apply {
                    behandlingsresultat = br
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusYears(1L)
                    lovvalgsland = Land_iso2.SE
                    unntakFraLovvalgsland = Land_iso2.NO
                    bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                    unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                    tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1
                    setSendtUtland(true)
                    dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                }.also {
                    it.anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                        anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
                        anmodningsperiode = it
                        registrertDato = LocalDate.now()
                    }
                }
            )

            br.utpekingsperioder.add(
                Utpekingsperiode(
                    LocalDate.now(),
                    LocalDate.now().plusYears(1),
                    Land_iso2.SE,
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                ).apply {
                    behandlingsresultat = br
                    medlPeriodeID = 1242L
                    sendtUtland = LocalDate.now()
                }
            )
        }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
