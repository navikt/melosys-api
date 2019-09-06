package no.nav.melosys.integrasjon.eessi;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SvarAnmodningUnntakDto;

public interface EessiConsumer {

    Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException;

    String opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException;

    List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, String status) throws MelosysException;

    List<Institusjon> hentMottakerinstitusjoner(String bucType) throws MelosysException;

    MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) throws MelosysException;

    void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) throws MelosysException;

    List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException;

    void sendAnmodningUnntakSvar(SvarAnmodningUnntakDto svarAnmodningUnntakDto, String rinaSaksnummer) throws MelosysException;

    byte[] hentSedForhåndsvisning(SedDataDto sedDataDto, SedType sedType) throws MelosysException;
}
