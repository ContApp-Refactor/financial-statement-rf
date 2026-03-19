package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountInfoClientTest {

    private AccountInfoClientProperties properties;
    private AccountInfoClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new AccountInfoClientProperties();
        properties.setBaseUrl("http://localhost:4001/accountInfo");
        properties.setMockModeEnabled(false);
        properties.setAcceptLegacyDateFormat(false);

        client = new AccountInfoClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void shouldMapValidIsoPayload() {
        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-001")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "voucher": { "number": 1010, "type": "NC" },
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "thirdPartyId": 1,
                            "accountingMovement": { "description": "Saldo caja", "debit": 150000000, "credit": 0 },
                            "costCenter": { "code": 1, "name": "General" }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-001",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 29)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-001");
        assertThat(result.get(0).getAccountCode()).isEqualTo("110505");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2025, 3, 29));

        server.verify();
    }

    @Test
    void shouldPreserveAccountCodeAsStringWithoutNumericCoercion() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": "0110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountCode()).isEqualTo("0110505");
        server.verify();
    }

    @Test
    void shouldAcceptLegacyMockDateWhenExplicitlyEnabled() {
        properties.setMockModeEnabled(true);
        properties.setAcceptLegacyDateFormat(true);

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-LOCAL")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "DEMO-ENT",
                            "date": "29-03-2025",
                            "voucher": { "number": 1010, "type": "NC" },
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "thirdPartyId": 1,
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 },
                            "costCenter": { "code": 1, "name": "General" }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-LOCAL",
                null,
                LocalDate.of(2025, 3, 29)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-LOCAL");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2025, 3, 29));

        server.verify();
    }

    @Test
    void shouldReturnEmptyListWhenAccountingModuleReturnsEmptyPayload() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(null, null, null);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void shouldRejectInvalidNature() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": "110505", "nature": "MIXTA", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void shouldRejectMissingAccountCode() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": "", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("codigo de cuenta");

        server.verify();
    }

    @Test
    void shouldRejectLegacyDateWhenNotExplicitlyEnabled() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "29-03-2025",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("Use yyyy-MM-dd");

        server.verify();
    }

    @Test
    void shouldSurfaceUnauthorizedFromAccountingModule() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"message\":\"unauthorized\"}"));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void shouldSurfaceForbiddenFromAccountingModule() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"message\":\"forbidden\"}"));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void shouldSurfaceServerErrorFromAccountingModule() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void shouldSurfaceTimeoutAsServiceUnavailable() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out");
                });

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        server.verify();
    }
}
