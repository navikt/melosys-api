package no.nav.melosys.service

import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.integrasjon.medl.GrunnlagMedl
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Factory for å lage test-data for saksbehandling.
 *
 * VIKTIG: lagBehandling-funksjonene legger IKKE behandlingen til fagsakens behandlinger-liste.
 * Dette er bevisst, slik at testene selv kan kontrollere når behandlingen knyttes til fagsak.
 * Kall `fagsak.leggTilBehandling(behandling)` etter å ha hentet behandlingen.
 *
 * Eksempel:
 * ```kotlin
 * val fagsak = SaksbehandlingDataFactory.lagFagsak()
 * val behandling = SaksbehandlingDataFactory.lagBehandling(fagsak)
 * fagsak.leggTilBehandling(behandling)  // Må kalles eksplisitt
 * ```
 */
object SaksbehandlingDataFactory {
    fun lagBehandling(): Behandling = lagBehandling(lagFagsak(), MottatteOpplysningerData())

    fun lagBehandling(mottatteOpplysningerData: MottatteOpplysningerData): Behandling =
        lagBehandling(lagFagsak(), mottatteOpplysningerData)

    fun lagBehandling(fagsak: Fagsak): Behandling = lagBehandling(fagsak, MottatteOpplysningerData())

    /**
     * Lager en Behandling med gitt fagsak og mottatte opplysninger.
     *
     * MERK: Behandlingen legges IKKE automatisk til fagsak.behandlinger.
     * Kall `fagsak.leggTilBehandling(behandling)` for å knytte dem sammen.
     */
    fun lagBehandling(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val naa = Instant.now()
        // Bruker BehandlingTestFactory.builderWithDefaults() direkte (ikke Behandling.forTest)
        // for å unngå at behandlingen automatisk legges til fagsaken.
        // Dette bevarer original oppførsel der testene selv kaller fagsak.leggTilBehandling().
        return BehandlingTestFactory.builderWithDefaults().apply {
            id = 1L
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            this.fagsak = fagsak
            mottatteOpplysninger = MottatteOpplysninger().apply {
                this.mottatteOpplysningerData = mottatteOpplysningerData
            }
            registrertDato = naa.minus(30, ChronoUnit.DAYS)
            endretDato = naa
        }.build().apply {
            this.mottatteOpplysninger?.behandling = this
        }
    }

    fun lagBehandlingMedMedlemskapDokument(): Behandling =
        lagBehandlingMedMedlemskapDokument(lagFagsak(), MottatteOpplysningerData())

    fun lagBehandlingMedMedlemskapDokument(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val naa = Instant.now()
        val medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode)
        val medlDokument = MedlemskapDokument().apply {
            this.medlemsperiode = listOf(medlemsperiode)
        }
        val medlSaksopplysning = saksopplysningForTest {
            dokument = medlDokument
            type = SaksopplysningType.MEDL
        }

        return BehandlingTestFactory.builderWithDefaults().apply {
            id = 1L
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            this.fagsak = fagsak
            mottatteOpplysninger = MottatteOpplysninger().apply {
                this.mottatteOpplysningerData = mottatteOpplysningerData
            }
            saksopplysninger = mutableSetOf(medlSaksopplysning)
            registrertDato = naa.minus(30, ChronoUnit.DAYS)
            endretDato = naa
        }.build().apply {
            this.mottatteOpplysninger?.behandling = this
            this.saksopplysninger.forEach { it.behandling = this }
        }
    }

    private fun lagMedlemsperiode(id: Long, grunnlagMedlKode: String): Medlemsperiode {
        val periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        return Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null
        )
    }

    fun lagInaktivBehandling(): Behandling = Behandling.forTest {
        id = 1L
        status = Behandlingsstatus.AVSLUTTET
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak { medBruker(); medGsakSaksnummer() }
        mottatteOpplysninger { }
    }

    /**
     * Lager en inaktiv (avsluttet) Behandling med gitt fagsak.
     *
     * MERK: Behandlingen legges IKKE automatisk til fagsak.behandlinger.
     * Kall `fagsak.leggTilBehandling(behandling)` for å knytte dem sammen.
     */
    fun lagInaktivBehandling(fagsak: Fagsak): Behandling {
        val naa = Instant.now()
        return BehandlingTestFactory.builderWithDefaults().apply {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            this.fagsak = fagsak
            mottatteOpplysninger = MottatteOpplysninger()
            registrertDato = naa.minus(30, ChronoUnit.DAYS)
            endretDato = naa
        }.build().apply {
            this.mottatteOpplysninger?.behandling = this
        }
    }

    fun lagInaktivBehandlingSomIkkeResulterIVedtak(): Behandling = Behandling.forTest {
        id = 1L
        status = Behandlingsstatus.AVSLUTTET
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.TRYGDETID
        fagsak { medBruker(); medGsakSaksnummer() }
    }

    fun lagBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat.forTest {
        id = 1L
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        vedtakMetadata {
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        }
    }

    fun lagFagsak(): Fagsak = Fagsak.forTest {
        medBruker()
        medGsakSaksnummer()
    }
}
