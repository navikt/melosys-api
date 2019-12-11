package no.nav.melosys.tjenester.gui.serverinfo;

import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerinfoTjenesteTest {

    @Test
    public void hentServerStatus() {
        ResponseEntity response = new ServerinfoTjeneste().hentServerStatus();
        ServerinfoDto forventetServerinfoDto = lagServerinfo();
        assertThat(response.getBody()).isEqualTo(forventetServerinfoDto);
    }

    private ServerinfoDto lagServerinfo() {
        return new ServerinfoDto(null, null, Serverinfo.FEIL, Serverinfo.FEIL, Serverinfo.hentVeraUrl(null, null));
    }
}