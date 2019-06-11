package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.util.function.Function;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegisterkontrollServiceTest {

    @Mock
    private KontrollFactory kontrollFactory;

    private RegisterkontrollService registerkontrollService;

    private final Behandling behandling = new Behandling();

    private Function<KontrollData, Unntak_periode_begrunnelser> f1 = (o) -> Unntak_periode_begrunnelser.BOSATT_I_NORGE;
    private Function<KontrollData, Unntak_periode_begrunnelser> f2 = (o) -> Unntak_periode_begrunnelser.INGEN_SLUTTDATO;
    private Function<KontrollData, Unntak_periode_begrunnelser> f3 = (o) -> Unntak_periode_begrunnelser.MOTTAR_YTELSER;

    @Before
    public void setup() {
        registerkontrollService = new RegisterkontrollService(kontrollFactory);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A009);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusMonths(1)));
        behandling.getSaksopplysninger().add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new MedlemskapDokument(), SaksopplysningType.MEDL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new InntektDokument(), SaksopplysningType.INNTK));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new PersonDokument(), SaksopplysningType.PERSOPL));

        when(kontrollFactory.hentKontrollerForSedType(any())).thenReturn(Lists.newArrayList(f1, f2, f3));
    }

    @Test
    public void utførKontroller_periodeIkkeGyldig_forventEttTreff() throws Exception {
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().minusYears(1)));
        assertThat(registerkontrollService.utførKontroller(behandling))
            .containsExactly(Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    public void utførKontroller_periodeGyldig_forventTreTreff() throws Exception {
        assertThat(registerkontrollService.utførKontroller(behandling))
            .containsExactly(
                Unntak_periode_begrunnelser.BOSATT_I_NORGE,
                Unntak_periode_begrunnelser.INGEN_SLUTTDATO,
                Unntak_periode_begrunnelser.MOTTAR_YTELSER
            );
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningDokument saksopplysningDokument, SaksopplysningType type) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(saksopplysningDokument);
        saksopplysning.setType(type);
        return saksopplysning;
    }



}