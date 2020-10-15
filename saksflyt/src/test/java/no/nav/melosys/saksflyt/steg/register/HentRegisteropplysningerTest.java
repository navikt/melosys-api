package no.nav.melosys.saksflyt.steg.register;

import java.time.LocalDate;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentRegisteropplysningerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private TpsFasade tpsFasade;

    private HentRegisteropplysninger hentRegisteropplysninger;

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> requestCaptor;

    private final Behandling behandling = new Behandling();
    private final String aktørID = "34253";
    private final String ident = "143545";

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        hentRegisteropplysninger = new HentRegisteropplysninger(registeropplysningerService, behandlingService, tpsFasade);

        behandling.setId(222L);

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(aktørID);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(eq(behandling.getId()))).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(eq(aktørID))).thenReturn(ident);
    }

    @Test
    void utfør_behandlingstemaUtsendtArbeidstaker_henterPeriodeFraSøknad() throws MelosysException {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new SoeknadDokument());
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode = periode;
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());

        assertThat(requestCaptor.getValue())
            .extracting(RegisteropplysningerRequest::getBehandlingID, RegisteropplysningerRequest::getFnr, RegisteropplysningerRequest::getFom, RegisteropplysningerRequest::getTom)
            .containsExactly(behandling.getId(), ident, periode.getFom(), periode.getTom());
    }
}
