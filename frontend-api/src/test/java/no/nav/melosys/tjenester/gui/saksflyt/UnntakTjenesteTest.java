package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import java.time.LocalDate;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.unntaksperiode.Unntaksperiode;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.IkkeGodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnntakTjenesteTest extends JsonSchemaTestParent {
    private static final String UNNTAKSPERIODE_GODKJENN_SCHEMA = "saksflyt-unntaksperioder-godkjenn-post-schema.json";
    private static final String UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA = "saksflyt-unntaksperioder-ikkegodkjenn-post-schema.json";

    @Mock
    private UnntaksperiodeService unntaksperiodeService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private UnntakTjeneste unntakTjeneste;

    @BeforeEach
    public void setUp() {
        unntakTjeneste = new UnntakTjeneste(unntaksperiodeService, aksesskontroll);
    }

    @Test
    public void unntaksperiodeGodkjennSchema() throws IOException {
        PeriodeDto unntaksperiodeDto = new PeriodeDto(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2005, 1, 1)
        );
        GodkjennUnntaksperiodeDto dto = new GodkjennUnntaksperiodeDto(
            true,
            "tekst",
            unntaksperiodeDto,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1.toString()
        );

        valider(dto, UNNTAKSPERIODE_GODKJENN_SCHEMA);
    }

    @Test
    public void godkjennUnntaksPeriode_godkjennerPeriode() {
        PeriodeDto periodeDto = new PeriodeDto(LocalDate.of(2001,1,1),LocalDate.of(2002, 1,1));
        GodkjennUnntaksperiodeDto dto = new GodkjennUnntaksperiodeDto(true, "tekst", periodeDto, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1.toString());

        unntakTjeneste.godkjennUnntaksperiode(1L, dto);

        UnntaksperiodeGodkjenning forventetUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(true)
            .fritekst("tekst")
            .endretPeriode(new Unntaksperiode(LocalDate.of(2001,1,1), LocalDate.of(2002,1,1)))
            .lovvalgsbestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
            .build();
        verify(unntaksperiodeService).godkjennPeriode(eq(1L), eq(forventetUnntaksperiodeGodkjenning));
    }

    @Test
    public void ikkeGodkjennUnntaksperiode() throws IOException {
        IkkeGodkjennUnntaksperiodeDto dto = new IkkeGodkjennUnntaksperiodeDto(
            Sets.newHashSet(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name()), null);
        unntakTjeneste.ikkeGodkjennUnntaksperiode(1L, dto);
        valider(dto, UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA);
    }
}
