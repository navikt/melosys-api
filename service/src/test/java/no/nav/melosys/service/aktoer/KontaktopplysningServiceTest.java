package no.nav.melosys.service.aktoer;

import java.util.Optional;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.KontaktopplysningID;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.KontaktopplysningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KontaktopplysningServiceTest {
    private KontaktopplysningService kontaktopplysningService;

    @Mock
    private KontaktopplysningRepository kontaktopplysningRepository;

    private Kontaktopplysning eksisterendeKontaktopplysning;

    private static final String SAK_NUMMER = "MEL-1";
    private static final String ORG_NUMMER = "999";

    @BeforeEach
    public void setUp() {
        kontaktopplysningService = new KontaktopplysningService(kontaktopplysningRepository);
        eksisterendeKontaktopplysning = new Kontaktopplysning();
        eksisterendeKontaktopplysning.setKontaktopplysningID(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER));
        eksisterendeKontaktopplysning.setKontaktNavn("eksisterendenavn");
        eksisterendeKontaktopplysning.setKontaktOrgnr("eksisterendeorgnr");
    }

    @Test
    public void hentKontaktopplysning_kallerRepoFindById() {
        kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        ArgumentCaptor<KontaktopplysningID> captor = ArgumentCaptor.forClass(KontaktopplysningID.class);
        verify(kontaktopplysningRepository).findById(captor.capture());
        KontaktopplysningID kontaktopplysningID = captor.getValue();
        assertThat(kontaktopplysningID.getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysningID.getOrgnr()).isEqualTo(ORG_NUMMER);
    }

    @Test
    public void lagEllerOppdaterKontaktopplysning_nyttObject_lagerNyttObjekt() {
        when(kontaktopplysningRepository.findById(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER))).thenReturn(Optional.empty());

        String kontaktorgnr = "nyttkontaktorgnr";
        String kontaktnavn = "nyttkontaktnavn";
        String kontakttelefon = "nyttkontakttelefonnummer";
        Kontaktopplysning kontaktopplysning = kontaktopplysningService
            .lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktorgnr, kontaktnavn, kontakttelefon);

        assertThat(kontaktopplysning).isNotSameAs(eksisterendeKontaktopplysning);
        verify(kontaktopplysningRepository).save(kontaktopplysning);
        assertThat(kontaktopplysning.getKontaktopplysningID().getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysning.getKontaktopplysningID().getOrgnr()).isEqualTo(ORG_NUMMER);
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo(kontaktnavn);
        assertThat(kontaktopplysning.getKontaktOrgnr()).isEqualTo(kontaktorgnr);
        assertThat(kontaktopplysning.getKontaktTelefon()).isEqualTo(kontakttelefon);
    }

    @Test
    public void lagEllerOppdaterKontaktopplysning_ekisterendeObject_oppdatererObjekt() {
        when(kontaktopplysningRepository.findById(new KontaktopplysningID(SAK_NUMMER, ORG_NUMMER))).thenReturn(Optional.of(eksisterendeKontaktopplysning));
        String kontaktorgnr = "nyttkontaktorgnr";
        String kontaktnavn = "nyttkontaktnavn";
        String kontakttelefon = "nyttkontakttelefonnummer";
        Kontaktopplysning kontaktopplysning = kontaktopplysningService
            .lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktorgnr, kontaktnavn, kontakttelefon);

        assertThat(kontaktopplysning).isSameAs(eksisterendeKontaktopplysning);
        verify(kontaktopplysningRepository).save(kontaktopplysning);
        assertThat(kontaktopplysning.getKontaktopplysningID().getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysning.getKontaktopplysningID().getOrgnr()).isEqualTo(ORG_NUMMER);
        assertThat(kontaktopplysning.getKontaktNavn()).isEqualTo(kontaktnavn);
        assertThat(kontaktopplysning.getKontaktOrgnr()).isEqualTo(kontaktorgnr);
        assertThat(kontaktopplysning.getKontaktTelefon()).isEqualTo(kontakttelefon);
    }

    @Test
    public void slettKontaktopplysning_kallerDeleteByIdMedGittSaksnummerOgOrgNummer() throws FunksjonellException {
        kontaktopplysningService.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        ArgumentCaptor<KontaktopplysningID> captor = ArgumentCaptor.forClass(KontaktopplysningID.class);
        verify(kontaktopplysningRepository).deleteById(captor.capture());
        KontaktopplysningID kontaktopplysningID = captor.getValue();

        assertThat(kontaktopplysningID.getSaksnummer()).isEqualTo(SAK_NUMMER);
        assertThat(kontaktopplysningID.getOrgnr()).isEqualTo(ORG_NUMMER);
    }
}
