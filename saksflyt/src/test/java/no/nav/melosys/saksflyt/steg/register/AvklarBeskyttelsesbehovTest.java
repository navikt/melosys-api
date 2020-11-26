package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.MedfolgendeFamilie;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvklarBeskyttelsesbehovTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private TpsFasade tpsFasade;

    private AvklarBeskyttelsesbehov avklarBeskyttelsesbehov;

    private static final String SPSF = "SPSF";

    @BeforeEach
    void setUp() {
        avklarBeskyttelsesbehov = new AvklarBeskyttelsesbehov(behandlingService, tpsFasade);
    }

    @Test
    void utfør_personHarSensitiveOpplysninger_oppgaveErSensitiv() throws MelosysException {
        Behandling behandling = lagBehandling(SPSF);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        avklarBeskyttelsesbehov.utfør(prosessinstans);

        assertThat(prosessinstans.getData(ProsessDataKey.HAR_SENSITIVE_OPPLYSNINGER, Boolean.class, false))
            .isTrue();
    }

    @Test
    void utfør_medfølgendeBarnHarSensitiveOpplysninger_oppgaveErSensitiv() throws MelosysException {
        Behandling behandling = lagBehandling(null);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(tpsFasade.harSensitiveOpplysninger(anyString())).thenReturn(true);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        avklarBeskyttelsesbehov.utfør(prosessinstans);

        assertThat(prosessinstans.getData(ProsessDataKey.HAR_SENSITIVE_OPPLYSNINGER, Boolean.class, false))
            .isTrue();
    }

    @Test
    void utfør_ingenHarSensitiveOpplysninger_oppgaveErIkkeSensitiv() throws MelosysException {
        Behandling behandling = lagBehandling(null);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        avklarBeskyttelsesbehov.utfør(prosessinstans);

        assertThat(prosessinstans.getData(ProsessDataKey.HAR_SENSITIVE_OPPLYSNINGER, Boolean.class, false))
            .isFalse();
    }

    private static Behandling lagBehandling(String diskresjonskode) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        PersonDokument personDokument = new PersonDokument();
        personDokument.diskresjonskode = new Diskresjonskode(diskresjonskode);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.personOpplysninger.medfolgendeFamilie
            = List.of(MedfolgendeFamilie.tilBarnFraFnr("barnFnr"));
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        return behandling;
    }

}