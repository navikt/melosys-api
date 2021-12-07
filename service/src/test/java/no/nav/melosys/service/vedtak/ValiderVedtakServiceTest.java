package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ValiderVedtakServiceTest {

    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private VedtakKontrollService vedtakKontrollService;

    private ValiderVedtakService validerVedtakService;

    @BeforeEach
    void setUp() {
        validerVedtakService = new ValiderVedtakService(persondataFasade, registeropplysningerService, vedtakKontrollService);
    }

    @Test
    void validerInnvilgelse_feilFraKontroller_kasterExceptionMedFeilkode() {
        var behandling = lagBehandling();
        var behandlingsresultat = lagBehandlingsresultat();
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentAktørID())).thenReturn("fnr");
        when(vedtakKontrollService.utførKontroller(behandling.getId(), FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS))
            .thenReturn(Collections.singletonList(new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)));

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder())
            .extracting(KontrollfeilDto::getKode).containsExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());

        assertThatThrownBy(() -> validerVedtakService.validerInnvilgelse(behandling, behandlingsresultat, FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS))
            .isInstanceOfSatisfying(ValideringException.class, medFeilkode)
            .hasMessage("Feil i validering. Kan ikke fatte vedtak.");
    }

    private Behandling lagBehandling() {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        var fagsak = new Fagsak();
        fagsak.setAktører(Set.of(bruker));
        var behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        return behandling;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(new Lovvalgsperiode()));
        return behandlingsresultat;
    }
}
