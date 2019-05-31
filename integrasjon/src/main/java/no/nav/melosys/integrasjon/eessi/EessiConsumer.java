package no.nav.melosys.integrasjon.eessi;

import java.util.List;
import java.util.Map;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.*;

public interface EessiConsumer {

    Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException;

    OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException;

    List<SedinfoDto> hentTilknyttedeSedUtkast(long gsakSaksnummer) throws MelosysException;

    List<InstitusjonDto> hentMottakerinstitusjoner(String bucType) throws MelosysException;

    List<BucSedRelasjonDto> hentBucSedRelasjoner() throws MelosysException;
}
