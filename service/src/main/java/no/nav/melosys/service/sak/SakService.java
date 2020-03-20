package no.nav.melosys.service.sak;

import java.util.EnumSet;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

@Service
public class SakService {

    private static final Logger log = LoggerFactory.getLogger(SakService.class);

    private final SakConsumer sakConsumer;

    private static final EnumSet<Behandlingstyper> GYLDIGE_BEHANDLINGSTYPER_MED = EnumSet.of(
        SOEKNAD, SOEKNAD_IKKE_YRKESAKTIV, SOEKNAD_ARBEID_FLERE_LAND,
        SOEKNAD_ARBEID_NORGE_BOSATT_ANNET_LAND, VURDER_TRYGDETID, BESLUTNING_LOVVALG_NORGE);

    private static final EnumSet<Behandlingstyper> GYLDIGE_BEHANDLINGSTYPER_UFM = EnumSet.of(
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
        ANMODNING_OM_UNNTAK_HOVEDREGEL, BESLUTNING_LOVVALG_ANNET_LAND);

    public SakService(@Qualifier("system") SakConsumer sakConsumer) {
        this.sakConsumer = sakConsumer;
    }

    public Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws FunksjonellException, TekniskException {
        SakDto sakDto = new SakDto();

        if (GYLDIGE_BEHANDLINGSTYPER_MED.contains(behandlingstype)) {
            sakDto.setTema(Tema.MED.getKode());
        } else if (GYLDIGE_BEHANDLINGSTYPER_UFM.contains(behandlingstype)) {
            sakDto.setTema(Tema.UFM.getKode());
        } else {
            throw new FunksjonellException("Behandlingtype " + behandlingstype.getBeskrivelse() + " er ikke støttet.");
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
