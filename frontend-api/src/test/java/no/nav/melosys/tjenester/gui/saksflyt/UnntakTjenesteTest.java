package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UnntakTjenesteTest extends JsonSchemaTestParent {
    private static final String UNNTAKSPERIODE_GODKJENN_SCHEMA = "saksflyt-unntaksperioder-godkjenn-post-schema.json";
    private static final String UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA = "saksflyt-unntaksperioder-ikkegodkjenn-post-schema.json";

    @Mock
    private UnntaksperiodeService unntaksperiodeService;

    private UnntakTjeneste unntakTjeneste;

    @Before
    public void setUp() {
        unntakTjeneste = new UnntakTjeneste(unntaksperiodeService);
    }

    @Test
    public void godkjennUnntaksperiode() throws FunksjonellException, TekniskException, IOException {
        GodkjennUnntaksperiodeDto dto = new GodkjennUnntaksperiodeDto(true, "tekst");
        unntakTjeneste.godkjennUnntaksperiode(1L, dto);
        verify(unntaksperiodeService).godkjennPeriode(anyLong(), eq(true), eq("tekst"));
        valider(dto, UNNTAKSPERIODE_GODKJENN_SCHEMA);
    }

    @Test
    public void ikkeGodkjennUnntaksperiode() throws FunksjonellException, IOException, TekniskException {
        VurderUnntaksperiodeDto dto = new VurderUnntaksperiodeDto(
            Sets.newHashSet(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name()), null);
        unntakTjeneste.ikkeGodkjennUnntaksperiode(1L, dto);
        valider(dto, UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA);
    }
}
