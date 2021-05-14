package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.Set;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningUnntakDto;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakTjenesteTest extends JsonSchemaTestParent {
    private static final String ANMODNING_UNNTAK_POST_SCHEMA = "saksflyt-anmodningsperioder-bestill-post-schema.json";

    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @BeforeEach
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgangService);
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

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(behandlingID, mottakerInstitusjon,
            Set.of(new DokumentReferanse(vedleggDto.getJournalpostID(), vedleggDto.getDokumentID())), fritekstSed);

        valider(dto, ANMODNING_UNNTAK_POST_SCHEMA);
    }
}
