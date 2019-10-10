package no.nav.melosys.integrasjonstest.felles.opplysninger;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import no.nav.melosys.tjenester.gui.AnmodningsperiodeTjeneste;
import no.nav.melosys.tjenester.gui.LovvalgsperiodeTjeneste;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Behandlingsdata {

    @MockBean
    TilgangService tilgangService;

    @Autowired
    LovvalgsperiodeTjeneste lovvalgsperiodeTjeneste;

    @Autowired
    AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    AnmodningsperiodeTjeneste anmodningsperiodeTjeneste;

    @Autowired
    private VilkaarsresultatService vilkårService;

    @Autowired
    BehandlingRepository behandlingRepository;

    public void opprettVilkaar(long behandlingsId, VilkaarDto... vilkaarDto) throws FunksjonellException {
        vilkårService.registrerVilkår(behandlingsId, Arrays.asList(vilkaarDto));
    }

    public void opprettLovvalgsperiode(long behandlingsId, LovvalgsperiodeDto... lovvalgsperioder) throws FunksjonellException, TekniskException {
        lovvalgsperiodeTjeneste.lagreLovvalgsperioder(behandlingsId, Arrays.asList(lovvalgsperioder));
    }

    public void lagreAnmodningsperiodeSvar(long behandlingsid, Anmodningsperiodesvartyper svarType) throws FunksjonellException, TekniskException {
        Anmodningsperiode periode = anmodningsperiodeService.hentAnmodningsperioder(behandlingsid).iterator().next();
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
}