package no.nav.melosys.integrasjon.eessi;

import java.util.Map;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;

public interface EessiConsumer {

    Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException;
}
