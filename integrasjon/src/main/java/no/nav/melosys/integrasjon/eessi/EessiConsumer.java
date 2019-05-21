package no.nav.melosys.integrasjon.eessi;

import java.util.List;
import java.util.Map;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;

public interface EessiConsumer {

    Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException;

    OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException;

    List<SedinfoDto> hentTilknyttedeSeder(long gsakSaksnummer) throws MelosysException;
}
