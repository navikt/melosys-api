package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.Aktoer;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.Applikasjoner;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.Kodeverdi;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class BehandlingStatusMapperTest {
    @Test
    public void build_forventVerdier() {
        BehandlingStatusMapper behandlingStatusMapper = new BehandlingStatusMapper.Builder()
            .medBehandlingsId(1L)
            .medSaksnummer("2")
            .medArkivtema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode())
            .medAktørID("123")
            .build();

        assertThat(behandlingStatusMapper.getBehandlingsID()).isEqualTo(String.format("%s-%d", Fagsystem.MELOSYS.getKode(), 1L));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo("2");
        assertThat(behandlingStatusMapper.getSakstema()).extracting(Kodeverdi::getValue).isEqualTo(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode());
        assertThat(behandlingStatusMapper.getAktoerREF()).extracting(Aktoer::getAktoerId).isEqualTo("123");

        assertThat(behandlingStatusMapper.getHendelsesId()).isNotNull();
        assertThat(behandlingStatusMapper.getHendelsesprodusentREF()).extracting(Applikasjoner::getValue).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(behandlingStatusMapper.getAnsvarligEnhetREF()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));

        assertThat(KonverteringsUtils.xmlGregorianCalendarToInstant(behandlingStatusMapper.getHendelsesTidspunkt()))
            .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
    }
}

