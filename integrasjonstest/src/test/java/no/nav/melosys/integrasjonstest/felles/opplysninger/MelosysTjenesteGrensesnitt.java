package no.nav.melosys.integrasjonstest.felles.opplysninger;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.*;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.tjenester.gui.*;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MelosysTjenesteGrensesnitt {

    @MockBean
    TilgangService tilgangService;

    @Autowired
    LovvalgsperiodeTjeneste lovvalgsperiodeTjeneste;

    @Autowired
    LovvalgsperiodeRepository lovvalgsperiodeRepo;

    @Autowired
    AnmodningsperiodeRepository anmodningsperiodeRepo;

    @Autowired
    AnmodningsperiodeTjeneste anmodningsperiodeTjeneste;

    @Autowired
    private VilkaarTjeneste vilkårTjeneste;

    @Autowired
    private VilkaarsresultatRepository vilkårRepo;

    @Autowired
    private AvklartefaktaTjeneste avklartefaktaTjeneste;

    @Autowired
    private AvklarteFaktaRepository avklartefaktaRepo;

    @Autowired
    private AktoerTjeneste aktoerTjeneste;

    @Autowired
    private AktoerRepository aktoerRepository;

    @Autowired
    BehandlingRepository behandlingRepository;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    public void nullstill(long behandlingsId) {
        vilkårRepo.deleteAll();
        avklartefaktaRepo.deleteAll();
        anmodningsperiodeRepo.deleteAll();
        lovvalgsperiodeRepo.deleteAll();
        aktoerRepository.deleteAll();
        prosessinstansRepository.deleteAll();

        setUnderBehandling(behandlingsId);
        setFagsakOpprettet(behandlingsId);
    }

    public void opprettAvklartefakta(long behandlingsId, AvklartefaktaDto... avklartefaktaDtoer) throws FunksjonellException, TekniskException {
        avklartefaktaTjeneste.lagreAvklarteFakta(behandlingsId, Sets.newHashSet(avklartefaktaDtoer));
    }

    public void opprettVilkaar(long behandlingsId, VilkaarDto... vilkaarDto) throws FunksjonellException, TekniskException {
        vilkårTjeneste.registrerVilkår(behandlingsId, Arrays.asList(vilkaarDto));
    }

    public void opprettLovvalgsperiode(long behandlingsId, LovvalgsperiodeDto... lovvalgsperioder) throws FunksjonellException, TekniskException {
        lovvalgsperiodeTjeneste.lagreLovvalgsperioder(behandlingsId, Arrays.asList(lovvalgsperioder));
    }

    public void opprettAktoer(long behandlingsId, AktoerDto... aktører) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingsId).get();
        String saksnummer = behandling.getFagsak().getSaksnummer();

        for (AktoerDto aktørDto : aktører) {
             aktoerTjeneste.lagAktoerer(saksnummer, aktørDto);
        }
    }

    public void lagreAnmodningsperiodeSvar(long behandlingsid, Anmodningsperiodesvartyper svarType) throws FunksjonellException, TekniskException {
        Anmodningsperiode periode = anmodningsperiodeRepo.findByBehandlingsresultatId(behandlingsid).iterator().next();
        AnmodningsperiodeSvarDto svarDto = new AnmodningsperiodeSvarDto(svarType.getKode(),
            new PeriodeDto(LocalDate.now(), LocalDate.now()), "");

        anmodningsperiodeTjeneste.lagreAnmodningsperiodeSvar(periode.getId(), svarDto);
    }

    public void setUnderBehandling(long behandlingsid) {
        Optional<Behandling> behandling = behandlingRepository.findById(behandlingsid);
        behandling.ifPresent(b -> {
            b.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingRepository.save(b);
        });
    }

    public void setFagsakOpprettet(long behandlingsid) {
        behandlingRepository.findById(behandlingsid)
        .ifPresent(b -> {
            b.getFagsak().setStatus(Saksstatuser.OPPRETTET);
            behandlingRepository.save(b);
        });
    }
}