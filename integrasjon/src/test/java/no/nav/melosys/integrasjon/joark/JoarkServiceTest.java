package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumentInformasjonMangler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journalfoeringsbehov;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.JournalpostMangler;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JoarkServiceTest {

    private JoarkService joarkService;

    @Before
    public void setUp() {
        this.joarkService = new JoarkService(null, null, null);
    }

    @Test
    public void konverterTilJournalfoeringmangler() {
        JournalpostMangler input = new JournalpostMangler();
        input.setArkivSak(Journalfoeringsbehov.MANGLER);
        input.setAvsenderId(Journalfoeringsbehov.MANGLER_IKKE);
        input.setAvsenderNavn(Journalfoeringsbehov.MANGLER);
        input.setBruker(Journalfoeringsbehov.MANGLER);
        input.setForsendelseInnsendt(Journalfoeringsbehov.MANGLER_IKKE);
        DokumentInformasjonMangler dokumentInformasjonMangler = new DokumentInformasjonMangler();
        dokumentInformasjonMangler.setDokumentkategori(Journalfoeringsbehov.MANGLER);
        dokumentInformasjonMangler.setTittel(Journalfoeringsbehov.MANGLER_IKKE);
        input.setHoveddokument(dokumentInformasjonMangler);
        input.setInnhold(Journalfoeringsbehov.MANGLER);
        input.setTema(Journalfoeringsbehov.MANGLER);

        List<JournalfoeringMangel> journalfoeringMangler = joarkService.konverterTilJournalfoeringmangler(input);

        assertThat(journalfoeringMangler).isNotNull();
        assertThat(journalfoeringMangler).isNotEmpty();
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.ARKIVSAK);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.AVSENDERID);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.AVSENDERNAVN);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.BRUKER);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.FORSENDELSEINNSENDT);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.INNHOLD);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.TEMA);
    }
}