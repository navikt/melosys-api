package no.nav.melosys.tjenester.gui;

import javax.ws.rs.core.Response;

import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import no.nav.melosys.tjenester.gui.serverinfo.Serverinfo;
import no.nav.melosys.tjenester.gui.serverinfo.ServerinfoTjeneste;
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
        return new ServerinfoDto(null, null, Serverinfo.FEIL, Serverinfo.FEIL, Serverinfo.VERA_URL);
    }
}