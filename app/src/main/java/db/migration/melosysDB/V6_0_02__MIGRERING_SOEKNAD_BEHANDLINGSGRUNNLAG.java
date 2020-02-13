package db.migration.melosysDB;

import java.sql.Clob;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@SuppressWarnings("unused")
public class V6_0_02__MIGRERING_SOEKNAD_BEHANDLINGSGRUNNLAG extends BaseJavaMigration {

    private final ObjectMapper objectMapper;
    private final DokumentFactory dokumentFactory;

    public V6_0_02__MIGRERING_SOEKNAD_BEHANDLINGSGRUNNLAG() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());
    }

    @Override
    public void migrate(Context context) throws Exception {

        OracleConnection con = context.getConnection().unwrap(OracleConnection.class);
        con.setAutoCommit(false);
        OracleResultSet resultSet = null;
        try (Statement statement = con.createStatement()) {
            statement.setFetchSize(50);
            resultSet = (OracleResultSet) statement.executeQuery("SELECT * FROM SAKSOPPLYSNING WHERE OPPLYSNING_TYPE = 'SØKNAD'");
            while (resultSet.next()) {
                opprettBehandlingsgrunnlag(resultSet, con);
            }
            con.commit();
        } finally {
            if (resultSet != null) resultSet.close();
        }
    }

    private void opprettBehandlingsgrunnlag(OracleResultSet resultSet, OracleConnection con) throws Exception {
        long behandlingID = resultSet.getLong("behandling_id");
        String versjon = resultSet.getString("versjon");
        Instant registrertDato = resultSet.getTimestamp("registrert_dato").toInstant();
        Instant endretDato = resultSet.getTimestamp("endret_dato").toInstant();
        String xmlString = resultSet.getString("intern_xml");
        String søknadJson = hentSøknadDokumentJson(xmlString);

        insertSøknad(con, behandlingID, versjon, registrertDato, endretDato, søknadJson);
    }

    private void insertSøknad(OracleConnection con, long behandlingID, String versjon, Instant registrertDato, Instant endretDato, String søknadJson) throws Exception {
        try (OraclePreparedStatement ps = (OraclePreparedStatement) con.prepareStatement(
            "INSERT INTO BEHANDLINGSGRUNNLAG(behandling_id, versjon, registrert_dato, endret_dato, type, data) VALUES (?, '1.1', ?, ?, 'SØKNAD', ?)")) {

            Clob clob = con.createClob();
            clob.setString(1, søknadJson);

            ps.setLong(1, behandlingID);
            ps.setTimestamp(2, Timestamp.from(registrertDato));
            ps.setTimestamp(3, Timestamp.from(endretDato));
            ps.setClob(4, clob);

            ps.execute();
        }
    }

    private String hentSøknadDokumentJson(String søknadXml) throws Exception {

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setInternXml(søknadXml);
        saksopplysning.setDokumentXml(søknadXml);
        saksopplysning.setVersjon("1.1");
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        dokumentFactory.lagDokument(saksopplysning);

        return objectMapper.writeValueAsString(saksopplysning.getDokument());
    }

    @Override
    public Integer getChecksum() {
        return 1764893572;
    }
}