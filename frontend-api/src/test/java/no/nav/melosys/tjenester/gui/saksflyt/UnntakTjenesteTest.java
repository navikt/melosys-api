package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import java.util.Optional;
import javax.ws.rs.BadRequestException;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntakTjenesteTest extends JsonSchemaTestParent {
    private static final String UNNTAKSPERIODE_SCHEMA = "saksflyt-unntaksperioder-ikkegodkjenn-post-schema.json";

    @Mock
    private UnntaksperiodeService unntaksperiodeService;
    @Mock
    private BehandlingRepository behandlingRepository;

    private UnntakTjeneste unntakTjeneste;

    @Before
    public void setUp() {
        unntakTjeneste = new UnntakTjeneste(unntaksperiodeService, behandlingRepository);
    }


    @Test(expected = BadRequestException.class)
    public void godkjennUnntaksperiode_feilBehandlingstype_kasterException() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        unntakTjeneste.godkjennUnntaksperiode(1L);
    }

    @Test(expected = BadRequestException.class)
    public void godkjennUnntaksperiode_feilStatus_kasterException() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        unntakTjeneste.godkjennUnntaksperiode(1L);
    }

    @Test
    public void godkjennUnntaksperiode_korrektStatus_ingenFeil() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        unntakTjeneste.godkjennUnntaksperiode(1L);
        verify(unntaksperiodeService).godkjennPeriode(any());
    }

    @Test
    public void innhentInfoUnntaksperiode_korrektStatus_ingenFeil() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        unntakTjeneste.godkjennUnntaksperiode(1L);
        verify(unntaksperiodeService).godkjennPeriode(any());
    }

    @Test
    public void ikkeGodkjennUnntaksperiode_gyldigBehandlingIdValiderSchema_ingenFeil() throws FunksjonellException, IOException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        VurderUnntaksperiodeDto dto = new VurderUnntaksperiodeDto(
            Sets.newHashSet(IkkeGodkjentBegrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name()), null
        );

        unntakTjeneste.ikkeGodkjennUnntaksperiode(1L, dto);

        valider(dto, UNNTAKSPERIODE_SCHEMA);
    }
}