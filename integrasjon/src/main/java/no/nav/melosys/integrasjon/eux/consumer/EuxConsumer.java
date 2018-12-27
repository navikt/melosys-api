package no.nav.melosys.integrasjon.eux.consumer;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestConsumer;

import java.util.List;

interface EuxConsumer extends RestConsumer {

    List<String> getInstitusjoner(String bucType, String landkode) throws TekniskException, FunksjonellException;

    String opprettBuCogSED(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId) throws TekniskException, FunksjonellException;

    String opprettRinaSak(String bucType) throws TekniskException, FunksjonellException;

    void oppdaterSED(String rinaSaksnummer, String korrelasjonsId, String sedType, String dokumentId, Object sed) throws FunksjonellException, TekniskException;

    void sendSED(String korrelasjonsId, String dokumentId) throws TekniskException, FunksjonellException;

    void opprettSED(String rinaSaksnummer, String euFormat, String korrelasjonsId, Object SED) throws TekniskException, FunksjonellException;

    String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType) throws TekniskException, FunksjonellException;

    String hentSED(String rinaSaksnummer, String dokumentId) throws FunksjonellException, TekniskException;

    String hentBuc(String buc) throws FunksjonellException, TekniskException;

    List<String> hentTilgjengeligeSEDTyper(String rinaSaksnummer) throws FunksjonellException, TekniskException;

    List<String> bucTypePerSektor() throws FunksjonellException, TekniskException;

}
