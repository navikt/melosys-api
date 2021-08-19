package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import java.time.LocalDate;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UnntakTjenesteTest extends JsonSchemaTestParent {
    private static final String UNNTAKSPERIODE_GODKJENN_SCHEMA = "saksflyt-unntaksperioder-godkjenn-post-schema.json";
    private static final String UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA = "saksflyt-unntaksperioder-ikkegodkjenn-post-schema.json";

    @Mock
    private UnntaksperiodeService unntaksperiodeService;

    @Captor
    private ArgumentCaptor<UnntaksperiodeGodkjenning> endretUnntaksperiodeGodkjenningArgumentCaptor;

    private UnntakTjeneste unntakTjeneste;

    @BeforeEach
    public void setUp() {
        unntakTjeneste = new UnntakTjeneste(unntaksperiodeService);
    }

    @Test
    public void unntaksperiodeGodkjennSchema() throws IOException {
        Unntaksperiode unntaksperiodeDto = new Unntaksperiode(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2005, 1, 1)
        );
        UnntaksperiodeGodkjenning dto = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(true)
            .fritekst("tekst")
            .unnntaksperiode(unntaksperiodeDto)
            .build();

        valider(dto, UNNTAKSPERIODE_GODKJENN_SCHEMA);
    }

    @Test
    public void godkjennUnntaksperiode_endretPeriodeErIkkeSatt_godkjennerPeriode() {
        PeriodeDto periodeDto = new PeriodeDto(null,null);
        GodkjennUnntaksperiodeDto dto = new GodkjennUnntaksperiodeDto(true, "tekst", periodeDto, null);

        unntakTjeneste.godkjennUnntaksperiode(1L, dto);

        verify(unntaksperiodeService).godkjennPeriode(eq(1L), endretUnntaksperiodeGodkjenningArgumentCaptor.capture());
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().isVarsleUtland()).isTrue();
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getFritekst()).isEqualTo("tekst");
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getEndretPeriode().fom()).isNull();
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getEndretPeriode().tom()).isNull();
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getLovvalgsbestemmelse()).isNull();
    }

    @Test
    public void godkjennUnntaksPeriode_endretPeriodeErSatt_godkjennerOgEndrerPeriode() {
        PeriodeDto periodeDto = new PeriodeDto(LocalDate.of(2001,1,1),LocalDate.of(2002, 1,1));
        GodkjennUnntaksperiodeDto dto = new GodkjennUnntaksperiodeDto(true, "tekst", periodeDto, "en bestemmelse");

        unntakTjeneste.godkjennUnntaksperiode(1L, dto);

        verify(unntaksperiodeService).godkjennOgEndrePeriode(eq(1L), endretUnntaksperiodeGodkjenningArgumentCaptor.capture());
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().isVarsleUtland()).isTrue();
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getFritekst()).isEqualTo("tekst");
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getEndretPeriode().fom()).isEqualTo(LocalDate.of(2001,1,1));
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getEndretPeriode().tom()).isEqualTo(LocalDate.of(2002,1,1));
        assertThat(endretUnntaksperiodeGodkjenningArgumentCaptor.getValue().getLovvalgsbestemmelse()).isEqualTo("en bestemmelse");
    }

    @Test
    public void ikkeGodkjennUnntaksperiode() throws IOException {
        IkkeGodkjennUnntaksperiodeDto dto = new IkkeGodkjennUnntaksperiodeDto(
            Sets.newHashSet(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name()), null);
        unntakTjeneste.ikkeGodkjennUnntaksperiode(1L, dto);
        valider(dto, UNNTAKSPERIODE_IKKEGODKJENN_SCHEMA);
    }
}
