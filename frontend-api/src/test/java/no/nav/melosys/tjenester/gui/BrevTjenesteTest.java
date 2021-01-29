package no.nav.melosys.tjenester.gui;

import java.util.List;

import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class BrevTjenesteTest {

    private BrevTjeneste brevTjeneste = new BrevTjeneste();

    @Test
    void skalReturnereTilgjengeligeBrevmaler() {
        List<BrevmalDto> brevmaler = brevTjeneste.hentTilgjengeligeMaler();

        assertEquals(2, brevmaler.size());
    }

}