package no.nav.melosys.tjenester.gui.serverinfo;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerinfoTest {
    private final String image = "docker.adeo.no:5000/melosys/melosys:sprint-34-157-b96a19ce83236594f2601c517f452db78a48748a";

    @Test
    public void hentVersjonelementer() {
        List<String> strings = Serverinfo.hentVersjonelementer(image);
        assertThat(strings).hasSize(4);
    }

    @Test
    public void hentBranch() {
        assertThat(Serverinfo.hentBranch(image)).isEqualTo("sprint-34");
    }

    @Test
    public void hentByggnummer() {
        assertThat(Serverinfo.hentByggnummer(image)).isEqualTo("157");
    }

    @Test
    public void hentHash() {
        assertThat(Serverinfo.hentHash(image)).isEqualTo("b96a19ce83236594f2601c517f452db78a48748a");
    }

    @Test
    public void hentVeraUrl() {
        assertThat(Serverinfo.hentVeraUrl("t8", "dev-fss"))
            .isEqualTo("https://vera.adeo.no/#/matrix?apps=melosys$&envs=t8:dev-fss");
    }
}