package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Optional;
import javax.ws.rs.BadRequestException;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SaksflytTjenesteTest extends JsonSchemaTestParent {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VedtakService vedtakService;

    @Mock
    private Tilgang tilgang;

    @Mock
    private UnntaksperiodeService unntaksperiodeService;

    @Mock
    private BehandlingRepository behandlingRepository;

    private SaksflytTjeneste saksflytTjeneste;

    private static final String schemaType = "vedtak-post-schema.json";

    private FattVedtakDto fattVedtakDto;
    private EndreVedtakDto endreVedtakDto;

    private long behandlingID;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        saksflytTjeneste = new SaksflytTjeneste(vedtakService, unntaksperiodeService, behandlingRepository, tilgang);
        fattVedtakDto = new FattVedtakDto();
        endreVedtakDto = new EndreVedtakDto();
        behandlingID = 3;
    }

    @Test
    public void fattVedtak_henleggelse_fungerer() throws FunksjonellException, TekniskException, IOException {
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.HENLEGGELSE);
        saksflytTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultattype());

        valider(fattVedtakDto);
    }

    @Test
    public void fattVedtak_anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException, IOException {
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        saksflytTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).anmodningOmUnntak(behandlingID);

        valider(fattVedtakDto);
    }

    @Test(expected = BadRequestException.class)
    public void fattVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        saksflytTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        valider(fattVedtakDto);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        endreVedtakDto.setBegrunnelseKode(Endretperioder.ENDRINGER_ARBEIDSSITUASJON);
        saksflytTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).endreVedtak(behandlingID, Endretperioder.ENDRINGER_ARBEIDSSITUASJON);

        valider(fattVedtakDto);
    }

    public void endreVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        expectedException.expect(BadRequestException.class);
        saksflytTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgang, never()).sjekk(behandlingID);
        valider(fattVedtakDto);
    }

    @Test(expected = BadRequestException.class)
    public void godkjennUnntaksperiode_feilBehandlingstype_kasterException() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.godkjennUnntaksperiode(1L);
    }

    @Test(expected = BadRequestException.class)
    public void godkjennUnntaksperiode_feilStatus_kasterException() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.godkjennUnntaksperiode(1L);
    }

    @Test
    public void godkjennUnntaksperiode_korrektStatus_ingenFeil() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.godkjennUnntaksperiode(1L);
        verify(unntaksperiodeService).godkjennPeriode(any());
    }

    @Test
    public void innhentInfoUnntaksperiode_korrektStatus_ingenFeil() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.godkjennUnntaksperiode(1L);
        verify(unntaksperiodeService).godkjennPeriode(any());
    }

    @Test(expected = BadRequestException.class)
    public void ikkeGodkjennUnntaksperiode_ingenBegrunnelser_kasterException() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.ikkeGodkjennUnntaksperiode(1L, new VurderUnntaksperiodeDto(Sets.newHashSet(), ""));
    }

    @Test(expected = BadRequestException.class)
    public void ikkeGodkjennUnntaksperiode_begrunnelseAnnetIngenFritekst_kasterException() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.ikkeGodkjennUnntaksperiode(1L,
            new VurderUnntaksperiodeDto(Sets.newHashSet(IkkeGodkjentBegrunnelser.ANNET),
                null));
    }

    @Test
    public void ikkeGodkjennUnntaksperiode_medBegrunnelse_ingenFeil() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        saksflytTjeneste.ikkeGodkjennUnntaksperiode(1L,
            new VurderUnntaksperiodeDto(Sets.newHashSet(IkkeGodkjentBegrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND),
                null));
    }
}