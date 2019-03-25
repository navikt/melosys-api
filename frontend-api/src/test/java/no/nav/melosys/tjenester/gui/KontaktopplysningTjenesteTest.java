package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.KontaktopplysningID;
import no.nav.melosys.repository.KontaktopplysningRepository;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KontaktopplysningTjenesteTest {

    private KontaktopplysningTjeneste kontaktopplysningTjeneste;

    @Mock
    private KontaktopplysningRepository kontaktopplysningRepo;

    private Kontaktopplysning kontaktopplysning;

    private static final String SAK_NUMMER = "MEL-1";
    private static final String ORG_NUMMER = "999";

    @Before
    public void setUp() {
        kontaktopplysningTjeneste = new KontaktopplysningTjeneste(kontaktopplysningRepo);
        kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER));
        kontaktopplysning.setKontaktNavn("JOHN MAN");
        kontaktopplysning.setKontaktOrgnr("1000");
    }

    @Test
    public void hentKontaktopplysning_gjeldig() {
        when(kontaktopplysningRepo.findById(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER))).thenReturn(Optional.of(kontaktopplysning));
        Response response = kontaktopplysningTjeneste.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);
        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getEntity()).isInstanceOf(Kontaktopplysning.class);

        Kontaktopplysning kontaktopplysning = (Kontaktopplysning) response.getEntity();
        assertThat(kontaktopplysning.getKontaktOrgnr()).isEqualTo("1000");
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo("JOHN MAN");
    }

    @Test
    public void hentKontaktopplysning_ikkeGjeldig() {
        Response response = kontaktopplysningTjeneste.hentKontaktopplysning(null, null);
        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNull();
    }

    @Test
    public void lagKontaktopplysning_ny() {
        KontaktInfoDto kontaktInfoDto = new KontaktInfoDto();
        kontaktInfoDto.kontaktnavn = "AA";
        kontaktInfoDto.kontaktorgnr = "100";
        when(kontaktopplysningRepo.findById(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER))).thenReturn(Optional.empty());
        Response response = kontaktopplysningTjeneste.lagKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktInfoDto);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(kontaktopplysningRepo).save(ArgumentMatchers.any(Kontaktopplysning.class));
        Kontaktopplysning kontaktopplysningRes = (Kontaktopplysning) response.getEntity();

        assertThat(kontaktopplysningRes.getKontaktopplysningID().getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysningRes.getKontaktopplysningID().getOrgnr()).isEqualTo(ORG_NUMMER);
        assertThat(kontaktopplysningRes.getKontaktNavn()).isEqualTo("AA");
        assertThat(kontaktopplysningRes.getKontaktOrgnr()).isEqualTo("100");
    }

    @Test
    public void lagKontaktopplysning_oppdatering() {
        KontaktInfoDto kontaktInfoDto = new KontaktInfoDto();
        kontaktInfoDto.kontaktnavn = "BB";
        kontaktInfoDto.kontaktorgnr = "200";
        when(kontaktopplysningRepo.findById(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER))).thenReturn(Optional.of(kontaktopplysning));
        Response response = kontaktopplysningTjeneste.lagKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktInfoDto);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(kontaktopplysningRepo).save(ArgumentMatchers.any(Kontaktopplysning.class));

        Kontaktopplysning kontaktopplysningRes = (Kontaktopplysning) response.getEntity();
        assertThat(kontaktopplysningRes.getKontaktNavn()).isEqualTo("BB");
        assertThat(kontaktopplysningRes.getKontaktOrgnr()).isEqualTo("200");
    }

    @Test
    public void slettKontaktopplysning_kallerDeleteByIdMedGittSaksnummerOgOrgNummer() {
        kontaktopplysningTjeneste.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        ArgumentCaptor<KontaktopplysningID> captor = ArgumentCaptor.forClass(KontaktopplysningID.class);
        verify(kontaktopplysningRepo).deleteById(captor.capture());
        KontaktopplysningID kontaktopplysningID = captor.getValue();

        assertThat(kontaktopplysningID.getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysningID.getOrgnr()).isEqualTo(ORG_NUMMER);
    }
}
