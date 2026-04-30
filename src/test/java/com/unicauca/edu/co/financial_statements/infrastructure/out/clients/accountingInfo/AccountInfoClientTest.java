package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
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
    void shouldFailFastWhenBaseUrlIsMissing() {
        properties.setBaseUrl("   ");

        assertThatThrownBy(() -> client.findAccountingEntries("ENT-001", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no esta configurada");
    }

    @Test
    void shouldSendConfiguredBearerTokenInAuthorizationHeader() {
        properties.setBearerToken("abc-123");

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer abc-123"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findAccountingEntries(null, null, null)).isEmpty();
        server.verify();
    }

    @Test
    void shouldPreferBearerTokenFromJwtSecurityContext() {
        properties.setBearerToken("property-token");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("jwt-123")
                        .header("alg", "none")
                        .claim("sub", "tester")
                        .build()
        ));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer jwt-123"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findAccountingEntries(null, null, null)).isEmpty();
        server.verify();
    }

    @Test
    void shouldFallbackToBearerTokenFromCurrentRequestHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AUTHORIZATION, "Bearer request-456");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer request-456"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findAccountingEntries(null, null, null)).isEmpty();
        server.verify();
    }

    @Test
    void shouldFallbackToConfiguredBearerTokenWhenCurrentRequestHasNoAuthorizationHeader() {
        properties.setBearerToken("property-789");
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer property-789"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findAccountingEntries(null, null, null)).isEmpty();
        server.verify();
    }

    @Test
    void shouldIgnoreNonBearerAuthorizationHeaderInCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AUTHORIZATION, "Basic abc123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(AUTHORIZATION))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findAccountingEntries(null, null, null)).isEmpty();
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
    void shouldTrimAccountCodeAndNormalizeLowercaseNature() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": " 220505 ", "nature": " credito ", "name": "Proveedores" },
                            "accountingMovement": { "description": "Saldo", "debit": 0, "credit": 4500 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountCode()).isEqualTo("220505");
        assertThat(result.get(0).getAccountNature()).isEqualTo("CREDITO");
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
    void shouldUseLoopbackAddressAsLocalMockAndSkipNullEntriesDuringFallbackAdaptation() {
        properties.setBaseUrl("http://127.0.0.1:4001/accountInfo");
        properties.setMockModeEnabled(true);

        client = new AccountInfoClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-LOOPBACK")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(ExpectedCount.once(), requestTo("http://127.0.0.1:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          null,
                          {
                            "entId": "DEMO-ENT",
                            "date": "2025-01-10",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-LOOPBACK",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-LOOPBACK");
        server.verify();
    }

    @Test
    void shouldRetryWithoutFiltersWhenMockReturns404AndReuseRequestedEnterpriseId() {
        properties.setMockModeEnabled(true);

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-REQUESTED")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "DEMO-ENT",
                            "date": "2025-01-10",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          },
                          {
                            "entId": "DEMO-ENT",
                            "date": "2024-12-31",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo anterior", "debit": 500, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-REQUESTED",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-REQUESTED");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2025, 1, 10));

        server.verify();
    }

    @Test
    void shouldAdaptHostedPostmanMockPayloadToRequestedEnterpriseId() {
        properties.setBaseUrl("https://financial-statement.mock.pstmn.io/accountInfo");
        properties.setMockModeEnabled(true);

        client = new AccountInfoClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-POSTMAN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "DEMO-ENT",
                            "date": "2025-03-29",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-POSTMAN",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-POSTMAN");

        server.verify();
    }

    @Test
    void shouldRetryHostedPostmanMockWithoutFiltersWhenFilteredRequestReturns404() {
        properties.setBaseUrl("https://financial-statement.mock.pstmn.io/accountInfo");
        properties.setMockModeEnabled(true);

        client = new AccountInfoClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-POSTMAN-404")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(ExpectedCount.once(), requestTo("https://financial-statement.mock.pstmn.io/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "DEMO-ENT",
                            "date": "2025-01-10",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          },
                          {
                            "entId": "DEMO-ENT",
                            "date": "2024-12-31",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo anterior", "debit": 500, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(
                "ENT-POSTMAN-404",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntId()).isEqualTo("ENT-POSTMAN-404");
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2025, 1, 10));

        server.verify();
    }

    @Test
    void shouldMapFallbackHttpExceptionWhenRetryAfter404AlsoFails() {
        properties.setMockModeEnabled(true);

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-FAIL")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findAccountingEntries(
                "ENT-FAIL",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        ))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("estado 500")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void shouldNotRetry404WhenMockFallbackConditionsAreNotMet() {
        properties.setBaseUrl("http://example.com/accountInfo");
        properties.setMockModeEnabled(true);

        client = new AccountInfoClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(ExpectedCount.once(), requestTo(containsString("entId=ENT-NO-FALLBACK")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.findAccountingEntries(
                "ENT-NO-FALLBACK",
                LocalDate.of(2025, 1, 1),
                null
        ))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("estado 404")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

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
    void shouldMapNullDebitAndCreditAsZeroAndAllowMissingVoucherAndCostCenter() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "voucher": null,
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "thirdPartyId": 77,
                            "accountingMovement": { "description": "Saldo caja", "debit": null, "credit": null },
                            "costCenter": null
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<AccountingEntry> result = client.findAccountingEntries(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDebit()).isZero();
        assertThat(result.get(0).getCredit()).isZero();
        assertThat(result.get(0).getVoucherNumber()).isNull();
        assertThat(result.get(0).getCostCenterCode()).isNull();
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
    void shouldRejectMissingAccountObject() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": null,
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("sin cuenta contable");

        server.verify();
    }

    @Test
    void shouldRejectMissingAccountingMovementObject() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": null
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("sin detalle contable");

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
    void shouldRejectMissingAccountNature() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025-03-29",
                            "account": { "code": "110505", "nature": "   ", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("sin naturaleza de cuenta");

        server.verify();
    }

    @Test
    void shouldRejectMissingDate() {
        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "   ",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("sin fecha");

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
    void shouldRejectUnsupportedDateFormatEvenWhenLegacyParsingIsEnabled() {
        properties.setAcceptLegacyDateFormat(true);

        server.expect(ExpectedCount.once(), requestTo("http://localhost:4001/accountInfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "entId": "ENT-001",
                            "date": "2025/03/29",
                            "account": { "code": "110505", "nature": "DEBITO", "name": "Caja" },
                            "accountingMovement": { "description": "Saldo caja", "debit": 1000, "credit": 0 }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAccountingEntries(null, null, null))
                .isInstanceOf(AccountInfoClientException.class)
                .hasMessageContaining("formato de fecha no soportado");

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
