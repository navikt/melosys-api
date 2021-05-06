package no.nav.melosys.integrasjonstest.saksflyt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.saksflyt.UnntakTjeneste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.profiles.active:local-mock")
class UnntakTjenesteIT {

    @Autowired
    private UnntakTjeneste unntakTjeneste;

    @Autowired
    private BehandlingService behandlingService;

    @Autowired
    private BehandlingRepository behandlingRepository;
    @Autowired
    private FagsakRepository fagsakRepository;
    @Autowired
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    private final LocalDate PERIODE_FOM = LocalDate.now();
    private final LocalDate PERIODE_TOM = LocalDate.now().plusMonths(2);
    private final String FRITEKST = "Begrunnelse fritekst";


    @AfterEach
    void cleanup() {
        behandlingsresultatRepository.deleteAll();
        prosessinstansRepository.deleteAll();
        behandlingRepository.deleteAll();
        fagsakRepository.deleteAll();
    }

    @Test
    void ikkeGodkjennUnntaksperiode_oppretterProsessistansRegistreringUnntakAvvist() throws FunksjonellException {
        final var ikkeGodkjentBegrunnelseKoder = Set.of(Ikke_godkjent_begrunnelser.UTSENDELSE_OVER_24_MD.getKode(), Ikke_godkjent_begrunnelser.ANNET.getKode());
        var fagsak = fagsakRepository.save(opprettFagsak());
        var behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, Behandlingstyper.SED, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, null, null);
        behandling.setSaksopplysninger(Set.of(opprettSedSaksopplysning(behandling)));
        behandlingRepository.save(behandling);

        var request = new VurderUnntaksperiodeDto(ikkeGodkjentBegrunnelseKoder, FRITEKST);
        unntakTjeneste.ikkeGodkjennUnntaksperiode(behandling.getId(), request);

        var prosessinstanser = prosessinstansRepository.findAll();
        assertThat(prosessinstanser.size()).isEqualTo(1);
        assertThat(prosessinstanser.get(0).getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_AVVIS);
        assertThat(prosessinstanser.get(0).getBehandling()).isEqualTo(behandling);
        assertThat(prosessinstanser.get(0).getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, Set.class)).containsExactlyInAnyOrder(ikkeGodkjentBegrunnelseKoder.stream().toArray());
        assertThat(prosessinstanser.get(0).getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isEqualTo(FRITEKST);
    }

    @Test
    void godkjennUnntaksperiode_oppretterRiktigProsessistans() throws FunksjonellException {
        final var varsleUtland = false;
        var fagsak = fagsakRepository.save(opprettFagsak());
        var behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, Behandlingstyper.SED, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, null, null);
        behandling.setSaksopplysninger(Set.of(opprettSedSaksopplysning(behandling)));
        behandlingRepository.save(behandling);

        var request = new GodkjennUnntaksperiodeDto(varsleUtland, FRITEKST);
        unntakTjeneste.godkjennUnntaksperiode(behandling.getId(), request);

        var prosessinstanser = prosessinstansRepository.findAll();
        assertThat(prosessinstanser.size()).isEqualTo(1);
        assertThat(prosessinstanser.get(0).getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_GODKJENN);
        assertThat(prosessinstanser.get(0).getBehandling()).isEqualTo(behandling);
        assertThat(prosessinstanser.get(0).getData(ProsessDataKey.VARSLE_UTLAND, boolean.class)).isEqualTo(varsleUtland);
        assertThat(prosessinstanser.get(0).getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isEqualTo(FRITEKST);
    }

    private Fagsak opprettFagsak() {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer("12");
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setAktører(Set.of(opprettAktør(fagsak)));
        return fagsak;
    }

    private Aktoer opprettAktør(Fagsak fagsak) {
        var aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.BRUKER);
        aktør.setFagsak(fagsak);
        return aktør;
    }

    private Saksopplysning opprettSedSaksopplysning(Behandling behandling) {
        var saksopplysning = new Saksopplysning();
        saksopplysning.setBehandling(behandling);
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setVersjon("1");
        saksopplysning.setRegistrertDato(Instant.now().minus(5, ChronoUnit.DAYS));
        saksopplysning.setEndretDato(Instant.now().minus(5, ChronoUnit.DAYS));
        saksopplysning.setDokument(opprettSedDokument());
        return saksopplysning;
    }

    private SaksopplysningDokument opprettSedDokument() {
        var sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(PERIODE_FOM, PERIODE_TOM));
        return sedDokument;
    }
}
