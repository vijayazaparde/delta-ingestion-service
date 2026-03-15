package com.delta.ingestion.repository;

import com.delta.ingestion.dto.IncomingCustomerDTO;
import com.delta.ingestion.exception.DatabaseException;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StagingRepository {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Timer copyThroughputTimer;

    public void copyInsert(String requestId,
                           OffsetDateTime createdAt,
                           List<IncomingCustomerDTO> customers) {

        copyThroughputTimer.record(() -> {

            Connection connection = null;
            CopyIn copyIn = null;

            try {

                connection = DataSourceUtils.getConnection(dataSource);
                BaseConnection pgConn = connection.unwrap(BaseConnection.class);

                CopyManager copyManager = new CopyManager(pgConn);

                String sql = """
                        COPY staging_customer
                        (request_id, external_id, name, email, country_code, status_code, created_at)
                        FROM STDIN WITH (FORMAT text, DELIMITER E'\\t')
                        """;

                copyIn = copyManager.copyIn(sql);

                StringBuilder sb = new StringBuilder(512);
                String timestamp = createdAt.toString();

                for (IncomingCustomerDTO dto : customers) {

                    sb.setLength(0);

                    sb.append(requestId).append('\t')
                            .append(clean(dto.getExternalId())).append('\t')
                            .append(clean(dto.getName())).append('\t')
                            .append(clean(dto.getEmail())).append('\t')
                            .append(cleanUpper(dto.getCountryCode())).append('\t')
                            .append(cleanUpper(dto.getStatusCode())).append('\t')
                            .append(timestamp)
                            .append('\n');

                    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                    copyIn.writeToCopy(bytes, 0, bytes.length);
                }

                copyIn.endCopy();

            } catch (Exception ex) {

                if (copyIn != null && copyIn.isActive()) {
                    try { copyIn.cancelCopy(); } catch (Exception ignored) {}
                }

                cleanup(requestId);

                log.error("COPY failed for requestId={}", requestId, ex);

                throw new DatabaseException("Batch stream failed", ex);

            } finally {

                if (connection != null) {
                    DataSourceUtils.releaseConnection(connection, dataSource);
                }
            }
        });
    }

    public void cleanup(String requestId) {

        try {
            jdbcTemplate.update(
                    "DELETE FROM staging_customer WHERE request_id = ?",
                    requestId
            );
        } catch (Exception ex) {
            log.error("Cleanup failed for request {}", requestId, ex);
        }
    }

    private String clean(String value) {

        if (value == null) return "\\N";

        return value.replace("\t", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private String cleanUpper(String value) {

        if (value == null) return "\\N";

        return clean(value).toUpperCase();
    }
}