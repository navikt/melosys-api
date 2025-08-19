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
    @JvmStatic
    fun lagBehandling(): Behandling {
        return lagBehandling(lagFagsak(), MottatteOpplysningerData())
    }

    @JvmStatic
    fun lagBehandling(mottatteOpplysningerData: MottatteOpplysningerData): Behandling {
        return lagBehandling(lagFagsak(), mottatteOpplysningerData)
    }

    @JvmStatic
    fun lagBehandling(fagsak: Fagsak): Behandling {
        return lagBehandling(fagsak, MottatteOpplysningerData())
    }

    @JvmStatic
    fun lagBehandling(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val nå = Instant.now()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(MottatteOpplysninger())
            .medRegistrertDato(nå.minus(30, ChronoUnit.DAYS))
            .medEndretDato(nå)
            .build()
        behandling.mottatteOpplysninger?.mottatteOpplysningerData = mottatteOpplysningerData
        return behandling
    }

    @JvmStatic
    fun lagBehandlingMedMedlemskapDokument(): Behandling {
        return lagBehandlingMedMedlemskapDokument(lagFagsak(), MottatteOpplysningerData())
    }

    @JvmStatic
    fun lagBehandlingMedMedlemskapDokument(
        fagsak: Fagsak,
        mottatteOpplysningerData: MottatteOpplysningerData
    ): Behandling {
        val behandling = lagBehandling(fagsak, mottatteOpplysningerData)
        val medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode)
        val medlDokument = MedlemskapDokument()
        val medl = Saksopplysning()

        medlDokument.medlemsperiode.add(medlemsperiode)
        medl.dokument = medlDokument
        medl.type = SaksopplysningType.MEDL

        behandling.saksopplysninger = mutableSetOf(medl)

        return behandling
    }

    private fun lagMedlemsperiode(id: Long, grunnlagMedlKode: String): Medlemsperiode {
        val periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        return Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null
        )
    }

    @JvmStatic
    fun lagInaktivBehandling(): Behandling {
        val behandling = lagBehandling()
        behandling.status = Behandlingsstatus.AVSLUTTET
        return behandling
    }

    @JvmStatic
    fun lagInaktivBehandling(fagsak: Fagsak): Behandling {
        val behandling = lagBehandling()
        behandling.fagsak = fagsak
        behandling.status = Behandlingsstatus.AVSLUTTET
        return behandling
    }

    @JvmStatic
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

    @JvmStatic
    fun lagBehandlingsresultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 1L
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        vedtakMetadata.vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        return behandlingsresultat
    }

    @JvmStatic
    fun lagFagsak(): Fagsak {
        return FagsakTestFactory.builder().medBruker().medGsakSaksnummer().build()
    }
}
