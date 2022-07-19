package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.Set;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakTjenesteTest {

    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @BeforeEach
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, aksesskontroll);
    }

    @Test
    void anmodningOmUnntak_fungerer() throws Exception {
        final long behandlingID = 3;
        final String mottakerInstitusjon = "SE:321";
        final String fritekstSed = "hei hei";

        AnmodningUnntakDto dto = new AnmodningUnntakDto();
        dto.setMottakerinstitusjon(mottakerInstitusjon);
        dto.setFritekstSed(fritekstSed);
        final var vedleggDto = new VedleggDto("jpID", "dokID");
        dto.setVedlegg(Set.of(vedleggDto));
        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID, dto);

        verify(aksesskontroll).autoriserSkriv(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(behandlingID, mottakerInstitusjon,
            Set.of(new DokumentReferanse(vedleggDto.journalpostID(), vedleggDto.dokumentID())), fritekstSed);

    }
}
