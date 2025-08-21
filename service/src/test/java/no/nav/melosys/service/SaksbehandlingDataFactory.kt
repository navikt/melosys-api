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

object SaksbehandlingDataFactory {
    fun lagBehandling(): Behandling = lagBehandling(lagFagsak(), MottatteOpplysningerData())

    fun lagBehandling(mottatteOpplysningerData: MottatteOpplysningerData): Behandling =
        lagBehandling(lagFagsak(), mottatteOpplysningerData)

    fun lagBehandling(fagsak: Fagsak): Behandling = lagBehandling(fagsak, MottatteOpplysningerData())

    fun lagBehandling(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val nå = Instant.now()
        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(MottatteOpplysninger())
            .medRegistrertDato(nå.minus(30, ChronoUnit.DAYS))
            .medEndretDato(nå)
            .build().apply {
                this.mottatteOpplysninger?.mottatteOpplysningerData = mottatteOpplysningerData
            }
    }

    fun lagBehandlingMedMedlemskapDokument(): Behandling =
        lagBehandlingMedMedlemskapDokument(lagFagsak(), MottatteOpplysningerData())

    fun lagBehandlingMedMedlemskapDokument(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val behandling = lagBehandling(fagsak, mottatteOpplysningerData)
        val medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode)
        val medlDokument = MedlemskapDokument().apply {
            this.medlemsperiode.add(medlemsperiode)
        }
        val medl = Saksopplysning().apply {
            dokument = medlDokument
            type = SaksopplysningType.MEDL
        }
        return behandling.apply {
            saksopplysninger = mutableSetOf(medl)
        }
    }

    private fun lagMedlemsperiode(id: Long, grunnlagMedlKode: String): Medlemsperiode {
        val periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        return Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null
        )
    }

    fun lagInaktivBehandling(): Behandling = lagBehandling().apply {
        status = Behandlingsstatus.AVSLUTTET
    }

    fun lagInaktivBehandling(fagsak: Fagsak): Behandling = lagBehandling().apply {
        this.fagsak = fagsak
        status = Behandlingsstatus.AVSLUTTET
    }

    fun lagInaktivBehandlingSomIkkeResulterIVedtak(): Behandling {
        val nå = Instant.now()
        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.TRYGDETID)
            .medFagsak(lagFagsak())
            .medRegistrertDato(nå.minus(30, ChronoUnit.DAYS))
            .medEndretDato(nå)
            .build()
    }

    fun lagBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        vedtakMetadata = VedtakMetadata().apply {
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        }
    }

    fun lagFagsak(): Fagsak = FagsakTestFactory.builder().medBruker().medGsakSaksnummer().build()
}
