package no.nav.melosys.tjenester.gui.serverinfo;

import javax.ws.rs.core.Response;

import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerinfoTjenesteTest {

    @Test
    public void hentServerStatus() {
        Response response = new ServerinfoTjeneste().hentServerStatus();
        ServerinfoDto forventetServerinfoDto = lagServerinfo();
        assertThat(response.getEntity()).isEqualTo(forventetServerinfoDto);
    }

    private ServerinfoDto lagServerinfo() {
        return new ServerinfoDto(null, null, Serverinfo.FEIL, Serverinfo.FEIL, Serverinfo.hentVeraUrl(null, null));
    }
}