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

internal class BehandlingsresultatServiceIT(
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository
) {
    private val AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE = "AvklartefaktaRegistrering-begrunnelsekode"

    @Test
    fun replikerBehandlingOgBehandlingsresultat_dataBlirRiktigIDB() {
//        val hentBehandlingsresultat = behandlingsresultatService!!.hentBehandlingsresultat(1L)
//        println(hentBehandlingsresultat)

        val fsak = Fagsak().apply {
            saksnummer = "MEL-1001"
            type = Sakstyper.TRYGDEAVTALE
            status = Saksstatuser.LOVVALG_AVKLART
            leggTilRegisteringInfo()
        }.also { fagsakRepository.save(it) }

        val tidligsteInaktiveBehandling = Behandling().apply {
            fagsak = fsak
            leggTilRegisteringInfo()
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.SOEKNAD
            tema = Behandlingstema.YRKESAKTIV
        }.also { behandlingRepository.save(it) }

        val behandlingsreplika = Behandling().apply {
            fagsak = fsak
            leggTilRegisteringInfo()
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.SOEKNAD
            tema = Behandlingstema.YRKESAKTIV
        }.also { behandlingRepository.save(it) }


        val behandlingsresultat: Behandlingsresultat = lagBehandlingsresultat(tidligsteInaktiveBehandling)

//        behandlingsresultatService!!.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika)
//
//        println(behandlingsresultat.avklartefakta.map { it.behandlingsresultat.id })

    }

    fun lagBehandlingsresultat(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat {
        val behandlingsresultat: Behandlingsresultat = Behandlingsresultat().apply {
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
                            begrunnelseKode = AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE
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
            behandlingsresultatRepository.save(br)
        }

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

    private fun setRegistreringsInfo(registreringsInfo: RegistreringsInfo) =
        registreringsInfo.apply {
            leggTilRegisteringInfo()
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

    private fun opprettTomtBehandlingsresultatMedId(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
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

    private fun opprettBehandlingsresultatBegrunnelse(): BehandlingsresultatBegrunnelse? {
        val behandlingsresultatBegrunnelse = BehandlingsresultatBegrunnelse()
        behandlingsresultatBegrunnelse.id = 32L
        behandlingsresultatBegrunnelse.behandlingsresultat =
            opprettTomtBehandlingsresultatMedId()
        behandlingsresultatBegrunnelse.kode = "begrunnelsekode"
        return behandlingsresultatBegrunnelse
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
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            avklartefakta = LinkedHashSet()
            lovvalgsperioder = LinkedHashSet()
            vilkaarsresultater = LinkedHashSet()
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            leggTilRegisteringInfo()
        }.also {
            it.vedtakMetadata = VedtakMetadata().apply {
                behandlingsresultat = it
                vedtaksdato = Instant.parse("2002-02-11T09:37:30Z")
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            behandlingsresultatRepository.save(it)
        }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
