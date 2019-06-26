package no.nav.melosys.integrasjon.eessi;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;

public interface EessiConsumer {

    Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException;

    String opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException;

    List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, String status) throws MelosysException;

    List<Institusjon> hentMottakerinstitusjoner(String bucType) throws MelosysException;
}
