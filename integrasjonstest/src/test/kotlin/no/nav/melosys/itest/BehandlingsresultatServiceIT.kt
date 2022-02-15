package no.nav.melosys.itest

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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ActiveProfiles(profiles = ["test"])
@ExtendWith(SpringExtension::class)
@DataJpaTest(
    showSql = false,
    excludeAutoConfiguration = [FlywayAutoConfiguration::class],
    properties = ["spring.profiles.active:test"],

    )
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories("no.nav.melosys.repository")
@EntityScan("no.nav.melosys.domain")
@Import(
    value = [
        BehandlingsresultatService::class,
    ]
)

internal class BehandlingsresultatServiceIT {
    private val AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE = "AvklartefaktaRegistrering-begrunnelsekode"

    @Autowired
    private val behandlingsresultatService: BehandlingsresultatService? = null

    @Autowired
    private val behandlingRepository: BehandlingRepository? = null

    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository? = null

    @Autowired
    private val fagsakRepository: FagsakRepository? = null

    @Test
    fun replikerBehandlingOgBehandlingsresultat_dataBlirRiktigIDB() {
//        val hentBehandlingsresultat = behandlingsresultatService!!.hentBehandlingsresultat(1L)
//        println(hentBehandlingsresultat)

        val fsak = Fagsak().apply {
            saksnummer = "MEL-1001"
            type = Sakstyper.TRYGDEAVTALE
            status = Saksstatuser.LOVVALG_AVKLART
            registrertDato = Instant.now()
            endretDato = Instant.now()
            endretAv = "bla"
        }.also { fagsakRepository!!.save(it) }

        val tidligsteInaktiveBehandling = Behandling().apply {
            id = 1001L
            fagsak = fsak
            registrertDato = Instant.now()
            endretDato = Instant.now()
            endretAv = "bla"
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.SOEKNAD
            tema = Behandlingstema.YRKESAKTIV
        }.also { behandlingRepository!!.save(it) }

        val behandlingsreplika = Behandling().apply {
            id = 1002L
            fagsak = fsak
            registrertDato = Instant.now()
            endretDato = Instant.now()
            endretAv = "bla"
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.SOEKNAD
            tema = Behandlingstema.YRKESAKTIV
        }.also { behandlingRepository!!.save(it) }

        val behandlingsresultat: Behandlingsresultat = lagBehandlingsresultat(tidligsteInaktiveBehandling)

        behandlingsresultatService!!.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika)

        println(behandlingsresultat.avklartefakta.map { it.behandlingsresultat.id })

    }

    fun lagBehandlingsresultat(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat {
        val behandlingsresultat: Behandlingsresultat = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefakta = opprettAvklartefakta()
        behandlingsresultat!!.avklartefakta.add(avklartefakta)
        val vilkaarsresultat = opprettVilkaarsresultat()
        behandlingsresultat.vilkaarsresultater.add(vilkaarsresultat)
        val lovvalgsperiode = opprettLovvalgsperiode()
        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        val behandlingsresultatBegrunnelse = opprettBehandlingsresultatBegrunnelse()
        behandlingsresultat.behandlingsresultatBegrunnelser.add(behandlingsresultatBegrunnelse)
        val kontrollresultat = opprettKontrollresultat()
        behandlingsresultat.kontrollresultater.add(kontrollresultat)
        val anmodningsperiode = opprettAnmodningsperiode()
        behandlingsresultat.anmodningsperioder.add(anmodningsperiode)
        val utpekingsperiode = opprettUtpekingsperiode()
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)
        return behandlingsresultat
    }

    private fun opprettTomtBehandlingsresultatMedId(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 667L
        return behandlingsresultat
    }

    private fun opprettLovvalgsperiode(): Lovvalgsperiode? {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.id = 32L
        lovvalgsperiode.behandlingsresultat = opprettTomtBehandlingsresultatMedId()
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsperiode.fom = LocalDate.now()
        lovvalgsperiode.tom = LocalDate.now().plusMonths(2)
        return lovvalgsperiode
    }

    private fun opprettAnmodningsperiode(): Anmodningsperiode? {
        val anmodningsperiode = Anmodningsperiode()
        anmodningsperiode.id = 32L
        anmodningsperiode.fom = LocalDate.now()
        anmodningsperiode.tom = LocalDate.now().plusYears(1L)
        anmodningsperiode.lovvalgsland = Landkoder.SE
        anmodningsperiode.unntakFraLovvalgsland = Landkoder.NO
        anmodningsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        anmodningsperiode.unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        anmodningsperiode.tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1
        anmodningsperiode.behandlingsresultat = opprettTomtBehandlingsresultatMedId()
        anmodningsperiode.setSendtUtland(true)
        anmodningsperiode.anmodningsperiodeSvar = AnmodningsperiodeSvar()
        anmodningsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        return anmodningsperiode
    }

    private fun opprettUtpekingsperiode(): Utpekingsperiode? {
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), LocalDate.now().plusYears(1), Landkoder.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        )
        utpekingsperiode.id = 11111L
        utpekingsperiode.medlPeriodeID = 1242L
        utpekingsperiode.sendtUtland = LocalDate.now()
        return utpekingsperiode
    }

    private fun opprettAvklartefakta(): Avklartefakta {
        val avklartefakta = Avklartefakta()
        avklartefakta.id = 32L
        avklartefakta.behandlingsresultat = opprettTomtBehandlingsresultatMedId()
        avklartefakta.fakta = "fakta"
        avklartefakta.type = Avklartefaktatyper.ARBEIDSLAND
        val avklartefaktaRegistrering = AvklartefaktaRegistrering()
        avklartefaktaRegistrering.begrunnelseKode =
            AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE
        avklartefakta.registreringer.add(avklartefaktaRegistrering)
        return avklartefakta
    }

    private fun opprettBehandlingsresultatBegrunnelse(): BehandlingsresultatBegrunnelse? {
        val behandlingsresultatBegrunnelse = BehandlingsresultatBegrunnelse()
        behandlingsresultatBegrunnelse.id = 32L
        behandlingsresultatBegrunnelse.behandlingsresultat =
            opprettTomtBehandlingsresultatMedId()
        behandlingsresultatBegrunnelse.kode = "begrunnelsekode"
        return behandlingsresultatBegrunnelse
    }

    private fun opprettVilkaarsresultat(): Vilkaarsresultat? {
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.behandlingsresultat = opprettTomtBehandlingsresultatMedId()
        vilkaarsresultat.id = 32L
        vilkaarsresultat.begrunnelseFritekst = "fritekst"
        vilkaarsresultat.begrunnelseFritekstEessi = "free text"
        val begrunnelser = HashSet<VilkaarBegrunnelse>()
        val vilkaarBegrunnelse = VilkaarBegrunnelse()
        vilkaarBegrunnelse.id = 2222L
        vilkaarBegrunnelse.kode = "kode"
        begrunnelser.add(vilkaarBegrunnelse)
        vilkaarsresultat.begrunnelser = begrunnelser
        return vilkaarsresultat
    }

    private fun opprettKontrollresultat(): Kontrollresultat? {
        val kontrollresultat = Kontrollresultat()
        kontrollresultat.id = 123L
        kontrollresultat.behandlingsresultat = opprettTomtBehandlingsresultatMedId()
        kontrollresultat.begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
        return kontrollresultat
    }

    fun opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = tidligsteInaktiveBehandling.id
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
//            vedtakMetadata = VedtakMetadata().apply {
//                id = 1001L
//                vedtaksdato = Instant.parse("2002-02-11T09:37:30Z")
//                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
//            }.also {  }
            avklartefakta = LinkedHashSet()
            lovvalgsperioder = LinkedHashSet()
            vilkaarsresultater = LinkedHashSet()
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            registrertDato = Instant.now()
            endretDato = Instant.now()
            endretAv = "bla"
        }.also {
            behandlingsresultatRepository!!.save(it)
        }
    }
}
