package no.nav.melosys.service.sak;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpprettSakTest {
    @Mock
    private JournalfoeringService journalfoeringService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private EessiService eessiService;

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    @Captor
    private ArgumentCaptor<no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest> opprettSakRequestArgumentCaptor;


    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .excludeField(named("behandlingsaarsakFritekst"))
            .stringLengthRange(2, 4);
    }

    private OpprettSak opprettSak;

    @BeforeEach
    void setUp() {
        opprettSak = new OpprettSak(prosessinstansService, saksbehandlingRegler, fagsakService, eessiService, lovligeKombinasjonerSaksbehandlingService);
    }

    @Test
    void lagNySak_EU_EOS_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.setSoknadDto(opprettSoknadDto());


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue()).isEqualTo(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_TRYGDEAVTALE_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_FTRL_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue()).isEqualTo(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_mottaksdatoMangler_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.setMottaksdato(null);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Mottaksdato");
    }

    @Test
    void lagNySak_årsakFritekstMedFeilType_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingsaarsakFritekst("Fritekst");
        opprettSakDto.setBehandlingsaarsakType(Behandlingsaarsaktyper.SØKNAD);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Kan ikke lagre fritekst som årsak når årsakstype");
    }

    private SøknadDto opprettSoknadDto() {
        var søknadDto = new SøknadDto();
        søknadDto.periode = new PeriodeDto(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(3));
        søknadDto.land = SoeknadslandDto.av(Landkoder.DE);
        return søknadDto;
    }
}
