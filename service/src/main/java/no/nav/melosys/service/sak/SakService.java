package no.nav.melosys.service.sak;

import java.util.EnumSet;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

@Service
public class SakService {

    private static final Logger log = LoggerFactory.getLogger(SakService.class);

    private final SakConsumer sakConsumer;

    private static final EnumSet<Behandlingstema> GYLDIGE_BEHANDLINGSTEMA_MED = EnumSet.of(
        UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_ETT_LAND_ØVRIG, IKKE_YRKESAKTIV, ARBEID_FLERE_LAND,
        ARBEID_NORGE_BOSATT_ANNET_LAND, BESLUTNING_LOVVALG_NORGE, TRYGDETID, ØVRIGE_SED_MED);

    private static final EnumSet<Behandlingstema> GYLDIGE_BEHANDLINGSTEMA_UFM = EnumSet.of(
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
        ANMODNING_OM_UNNTAK_HOVEDREGEL, BESLUTNING_LOVVALG_ANNET_LAND, ØVRIGE_SED_UFM);

    public SakService(@Qualifier("system") SakConsumer sakConsumer) {
        this.sakConsumer = sakConsumer;
    }

    public Long opprettSak(String saksnummer, Behandlingstema behandlingstema, String aktørId) throws FunksjonellException, TekniskException {
        SakDto sakDto = new SakDto();

        if (GYLDIGE_BEHANDLINGSTEMA_MED.contains(behandlingstema)) {
            sakDto.setTema(Tema.MED.getKode());
        } else if (GYLDIGE_BEHANDLINGSTEMA_UFM.contains(behandlingstema)) {
            sakDto.setTema(Tema.UFM.getKode());
        } else {
            throw new FunksjonellException("Behandlingstema " + behandlingstema.getBeskrivelse() + " er ikke støttet.");
        }

        sakDto.setAktørId(aktørId);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakConsumer.opprettSak(sakDto);
        log.info("Sak opprettet med sakID: {}", sakDto.getId());
        return sakDto.getId();
    }

    public Tema hentTemaFraSak(Long gsakSaksnummer) throws FunksjonellException, TekniskException {
        return Tema.valueOf(sakConsumer.hentSak(gsakSaksnummer).getTema());
    }
}
