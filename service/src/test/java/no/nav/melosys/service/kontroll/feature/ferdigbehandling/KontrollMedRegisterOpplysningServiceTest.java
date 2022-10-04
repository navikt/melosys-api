package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontrollMedRegisterOpplysningServiceTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    private KontrollMedRegisteropplysning kontrollMedRegisterOpplysning;

    private final long behandlingID = 1L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private final Behandling behandling = lagBehandling(behandlingsgrunnlagData);


    @BeforeEach
    void setup() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        behandling.getSaksopplysninger().add(medlSaksopplysning);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);

        Kontroll kontroll = new Kontroll(behandlingService, lovvalgsperiodeService, persondataFasade, behandlingsresultatService);
        kontrollMedRegisterOpplysning = new KontrollMedRegisteropplysning(behandlingService, behandlingsresultatService, persondataFasade,
            registeropplysningerService, kontroll);
    }

    @Test
    void kontrollerVedtakMedNyeRegisteropplysninger_feilFraKontroller_kasterExceptionMedFeilkode() {
        var behandlingsresultat = lagBehandlingsresultat();
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID())).thenReturn("fnr");

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder())
            .extracting(KontrollfeilDto::getKode).containsExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO.getKode());

        assertThatThrownBy(() -> kontrollMedRegisterOpplysning.kontrollerVedtak(behandling, behandlingsresultat, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN))
            .isInstanceOfSatisfying(ValideringException.class, medFeilkode)
            .hasMessage("Feil i validering. Kan ikke fatte vedtak.");
    }

    @Test
    void kontrollerVedtak_oppdaterRegisteropplysninger_oppdatererRegisteropplysninger() throws ValideringException {
        lovvalgsperiode.setTom(LocalDate.now());
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID())).thenReturn("fnr");

        kontrollMedRegisterOpplysning.kontroller(behandling.getId(), Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
        verify(registeropplysningerService).hentOgLagreOpplysninger(any());
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        return behandlingsresultat;
    }
}
