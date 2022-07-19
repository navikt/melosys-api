package no.nav.melosys.tjenester.gui.saksflyt;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntaksperiode.Unntaksperiode;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnntakTjenesteTest {

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
}
