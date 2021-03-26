package no.nav.melosys.service.kontroll.ufm;

import java.time.LocalDate;
import java.util.function.Function;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UfmKontrollServiceTest {

    @Mock
    private KontrollFactory kontrollFactory;

    private UfmKontrollService ufmKontrollService;

    private final Behandling behandling = new Behandling();

    private Function<UfmKontrollData, Kontroll_begrunnelser> f1 = (o) -> Kontroll_begrunnelser.BOSATT_I_NORGE;
    private Function<UfmKontrollData, Kontroll_begrunnelser> f2 = (o) -> Kontroll_begrunnelser.INGEN_SLUTTDATO;
    private Function<UfmKontrollData, Kontroll_begrunnelser> f3 = (o) -> Kontroll_begrunnelser.MOTTAR_YTELSER;

    @Before
    public void setup() {
        ufmKontrollService = new UfmKontrollService(kontrollFactory);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A009);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusMonths(1)));
        behandling.getSaksopplysninger().add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new MedlemskapDokument(), SaksopplysningType.MEDL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new InntektDokument(), SaksopplysningType.INNTK));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new PersonDokument(), SaksopplysningType.PERSOPL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new UtbetalingDokument(), SaksopplysningType.UTBETAL));

        when(kontrollFactory.hentKontrollerForSedType(any())).thenReturn(Lists.newArrayList(f1, f2, f3));
    }

    @Test
    public void utførKontroller_periodeIkkeGyldig_forventEttTreff() throws Exception {
        SedDokument sedDokument = behandling.hentSedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().minusYears(1)));
        assertThat(ufmKontrollService.utførKontroller(behandling))
            .containsExactly(Kontroll_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    public void utførKontroller_periodeGyldig_forventTreTreff() throws Exception {
        assertThat(ufmKontrollService.utførKontroller(behandling))
            .containsExactly(
                Kontroll_begrunnelser.BOSATT_I_NORGE,
                Kontroll_begrunnelser.INGEN_SLUTTDATO,
                Kontroll_begrunnelser.MOTTAR_YTELSER
            );
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningDokument saksopplysningDokument, SaksopplysningType type) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(saksopplysningDokument);
        saksopplysning.setType(type);
        return saksopplysning;
    }
}