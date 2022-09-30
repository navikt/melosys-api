package no.nav.melosys.service.sak;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArkivsakService {

    private static final Logger log = LoggerFactory.getLogger(ArkivsakService.class);

    private final SakConsumer sakConsumer;
    private final Unleash unleash;

    public ArkivsakService(SakConsumer sakConsumer, Unleash unleash) {
        this.sakConsumer = sakConsumer;
        this.unleash = unleash;
    }

    public Long opprettSakForBruker(String saksnummer, Behandlingstema behandlingstema, Sakstemaer sakstemaer, String aktørId) {
        boolean behandleAlleSakerEnabled = unleash.isEnabled("melosys.behandle_alle_saker");

        SakDto sakDto = new SakDto();
        sakDto.setTema(behandleAlleSakerEnabled ? OppgaveFactory.utledTema(sakstemaer).getKode() : TemaFactory.fraBehandlingstema(behandlingstema).getKode());
        sakDto.setAktørId(aktørId);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakConsumer.opprettSak(sakDto);
        log.info("Sak opprettet med sakID: {} for bruker", sakDto.getId());
        return sakDto.getId();
    }

    public Long opprettSakForVirksomhet(String saksnummer, Behandlingstema behandlingstema, Sakstemaer sakstemaer, String orgnr) {

        boolean behandleAlleSakerEnabled = unleash.isEnabled("melosys.behandle_alle_saker");
        SakDto sakDto = new SakDto();
        sakDto.setTema(behandleAlleSakerEnabled ? OppgaveFactory.utledTema(sakstemaer).getKode() : TemaFactory.fraBehandlingstema(behandlingstema).getKode());
        sakDto.setOrgnr(orgnr);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakConsumer.opprettSak(sakDto);
        log.info("Sak opprettet med sakID: {} for virksomhet: {}", sakDto.getId(), sakDto.getOrgnr());
        return sakDto.getId();
    }

    public Tema hentTemaFraSak(Long gsakSaksnummer) {
        return Tema.valueOf(sakConsumer.hentSak(gsakSaksnummer).getTema());
    }
}
