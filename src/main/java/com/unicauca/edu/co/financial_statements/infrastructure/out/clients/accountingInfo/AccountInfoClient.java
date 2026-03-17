package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo;

import com.unicauca.edu.co.financial_statements.application.ports.out.IAccountingInfoClient;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo.dto.AccountInfoMovementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class AccountInfoClient implements IAccountingInfoClient {

    private static final DateTimeFormatter ISO_DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LEGACY_MOCK_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private final RestTemplate restTemplate;
    private final AccountInfoClientProperties properties;

    public AccountInfoClient(
            RestTemplateBuilder restTemplateBuilder,
            AccountInfoClientProperties properties
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.properties = properties;
    }

    @Override
    public List<AccountingEntry> findAccountingEntries(String entId, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("The accounting information service URL is not configured.");
        }

        URI filteredRequestUri = buildFilteredRequestUri(entId, startDate, endDate);
        URI fallbackRequestUri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .build(true)
                .toUri();
        boolean shouldUseMockFallback = shouldUseMockFallback(entId, startDate, endDate);

        try {
            AccountInfoMovementResponse[] responseBody = requestEntries(filteredRequestUri);

            if ((responseBody == null || responseBody.length == 0) && shouldUseMockFallback) {
                log.warn(
                        "The accounting mock returned an empty response for filtered request {}. Retrying without query filters using {}.",
                        filteredRequestUri,
                        fallbackRequestUri
                );
                responseBody = requestEntries(fallbackRequestUri);
            }

            if (responseBody == null || responseBody.length == 0) {
                return List.of();
            }

            return filterAndMapEntries(responseBody, entId, startDate, endDate);
        } catch (RestClientResponseException exception) {
            if (shouldUseMockFallback && exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn(
                        "The accounting mock returned 404 for filtered request {}. Retrying without query filters using {}.",
                        filteredRequestUri,
                        fallbackRequestUri
                );

                try {
                    AccountInfoMovementResponse[] responseBody = requestEntries(fallbackRequestUri);
                    return filterAndMapEntries(responseBody, entId, startDate, endDate);
                } catch (RestClientResponseException fallbackException) {
                    throw mapHttpException(fallbackRequestUri, fallbackException);
                }
            }

            throw mapHttpException(filteredRequestUri, exception);
        } catch (ResourceAccessException exception) {
            log.error("Timeout or connectivity error requesting accounting information from {}", filteredRequestUri, exception);
            throw new AccountInfoClientException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The accounting module is unavailable or timed out.",
                    exception
            );
        }
    }

    private URI buildFilteredRequestUri(String entId, LocalDate startDate, LocalDate endDate) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .queryParamIfPresent("entId", optionalText(entId))
                .queryParamIfPresent("startDate", optionalDate(startDate))
                .queryParamIfPresent("endDate", optionalDate(endDate))
                .build(true)
                .toUri();
    }

    private boolean shouldUseMockFallback(String entId, LocalDate startDate, LocalDate endDate) {
        return properties.isMockModeEnabled()
                && isLocalJsonServerMock()
                && (StringUtils.hasText(entId) || startDate != null || endDate != null);
    }

    private AccountInfoMovementResponse[] requestEntries(URI requestUri) {
        ResponseEntity<AccountInfoMovementResponse[]> response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                AccountInfoMovementResponse[].class
        );
        return response.getBody();
    }

    private List<AccountingEntry> filterAndMapEntries(
            AccountInfoMovementResponse[] body,
            String entId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (body == null || body.length == 0) {
            return List.of();
        }

        AccountInfoMovementResponse[] sourceEntries = adaptLocalMockEntries(body, entId);

        return Arrays.stream(sourceEntries)
                .filter(Objects::nonNull)
                .map(this::toDomain)
                .filter(entry -> !StringUtils.hasText(entId) || entId.equalsIgnoreCase(entry.getEntId()))
                .filter(entry -> startDate == null || !entry.getDate().isBefore(startDate))
                .filter(entry -> endDate == null || !entry.getDate().isAfter(endDate))
                .toList();
    }

    private AccountInfoMovementResponse[] adaptLocalMockEntries(
            AccountInfoMovementResponse[] body,
            String requestedEntId
    ) {
        if (!shouldAdaptLocalMockEntries(body, requestedEntId)) {
            return body;
        }

        log.warn(
                "The local accounting mock dataset does not contain enterprise {}. Reusing the demo data for that enterprise because mock mode is enabled.",
                requestedEntId
        );

        return Arrays.stream(body)
                .map(entry -> cloneWithEntId(entry, requestedEntId))
                .toArray(AccountInfoMovementResponse[]::new);
    }

    private boolean shouldAdaptLocalMockEntries(AccountInfoMovementResponse[] body, String requestedEntId) {
        if (!properties.isMockModeEnabled()
                || !isLocalJsonServerMock()
                || !StringUtils.hasText(requestedEntId)
                || body == null
                || body.length == 0) {
            return false;
        }

        return Arrays.stream(body)
                .map(AccountInfoMovementResponse::getEntId)
                .filter(StringUtils::hasText)
                .noneMatch(requestedEntId::equalsIgnoreCase);
    }

    private boolean isLocalJsonServerMock() {
        try {
            URI uri = URI.create(properties.getBaseUrl());
            String host = uri.getHost();
            int port = uri.getPort();

            return port == 4001 && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
        } catch (Exception exception) {
            return false;
        }
    }

    private AccountInfoMovementResponse cloneWithEntId(AccountInfoMovementResponse source, String requestedEntId) {
        if (source == null) {
            return null;
        }

        return new AccountInfoMovementResponse(
                requestedEntId,
                source.getDate(),
                source.getVoucher() != null
                        ? new AccountInfoMovementResponse.VoucherResponse(
                        source.getVoucher().getNumber(),
                        source.getVoucher().getType()
                )
                        : null,
                source.getAccount() != null
                        ? new AccountInfoMovementResponse.AccountResponse(
                        source.getAccount().getCode(),
                        source.getAccount().getNature(),
                        source.getAccount().getName()
                )
                        : null,
                source.getThirdPartyId(),
                source.getAccountingMovement() != null
                        ? new AccountInfoMovementResponse.AccountingMovementResponse(
                        source.getAccountingMovement().getDescription(),
                        source.getAccountingMovement().getDebit(),
                        source.getAccountingMovement().getCredit()
                )
                        : null,
                source.getCostCenter() != null
                        ? new AccountInfoMovementResponse.CostCenterResponse(
                        source.getCostCenter().getCode(),
                        source.getCostCenter().getName()
                )
                        : null
        );
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String bearerToken = resolveBearerToken();

        if (StringUtils.hasText(bearerToken)) {
            headers.setBearerAuth(bearerToken.trim());
        }

        return headers;
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getTokenValue();
        }

        String requestBearerToken = resolveBearerTokenFromCurrentRequest();
        if (StringUtils.hasText(requestBearerToken)) {
            return requestBearerToken;
        }

        return StringUtils.hasText(properties.getBearerToken()) ? properties.getBearerToken().trim() : null;
    }

    private String resolveBearerTokenFromCurrentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        String bearerPrefix = "Bearer ";
        if (authorization.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
            return authorization.substring(bearerPrefix.length()).trim();
        }

        return null;
    }

    private Optional<String> optionalText(String value) {
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private Optional<String> optionalDate(LocalDate value) {
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    private AccountingEntry toDomain(AccountInfoMovementResponse source) {
        validateSource(source);
        LocalDate parsedDate = parseDate(source.getDate());
        String normalizedNature = normalizeNature(source.getAccount().getNature());
        String accountCode = normalizeAccountCode(source.getAccount().getCode());

        return AccountingEntry.builder()
                .entId(source.getEntId())
                .date(parsedDate)
                .accountCode(accountCode)
                .accountName(source.getAccount().getName())
                .accountNature(normalizedNature)
                .voucherNumber(source.getVoucher() != null ? source.getVoucher().getNumber() : null)
                .voucherType(source.getVoucher() != null ? source.getVoucher().getType() : null)
                .thirdPartyId(source.getThirdPartyId())
                .costCenterCode(source.getCostCenter() != null ? source.getCostCenter().getCode() : null)
                .debit(safeBigDecimal(source.getAccountingMovement().getDebit()))
                .credit(safeBigDecimal(source.getAccountingMovement().getCredit()))
                .movementDescription(source.getAccountingMovement().getDescription())
                .build();
    }

    private void validateSource(AccountInfoMovementResponse source) {
        if (source == null) {
            throw invalidPayload("The accounting module returned a null movement.");
        }
        if (source.getAccount() == null) {
            throw invalidPayload("The accounting module returned a movement without account.");
        }
        if (source.getAccountingMovement() == null) {
            throw invalidPayload("The accounting module returned a movement without accountingMovement.");
        }
        if (!StringUtils.hasText(source.getDate())) {
            throw invalidPayload("The accounting module returned a movement without date.");
        }
        if (!StringUtils.hasText(source.getAccount().getCode())) {
            throw invalidPayload("The accounting module returned a movement without account.code.");
        }
        if (!StringUtils.hasText(source.getAccount().getNature())) {
            throw invalidPayload("The accounting module returned a movement without account.nature.");
        }
    }

    private LocalDate parseDate(String value) {
        String normalizedValue = value != null ? value.trim() : null;
        if (!StringUtils.hasText(normalizedValue)) {
            throw invalidPayload("The accounting module returned a movement without a valid date.");
        }

        try {
            return LocalDate.parse(normalizedValue, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            if (!properties.isAcceptLegacyDateFormat()) {
                throw invalidPayload(
                        "The accounting module returned an unsupported date format: " + normalizedValue + ". Expected yyyy-MM-dd."
                );
            }
        }

        try {
            return LocalDate.parse(normalizedValue, LEGACY_MOCK_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw invalidPayload(
                    "The accounting module returned an unsupported date format: " + normalizedValue + "."
            );
        }
    }

    private String normalizeNature(String value) {
        String normalized = value != null ? value.trim().toUpperCase(Locale.ROOT) : null;
        if (!"DEBITO".equals(normalized) && !"CREDITO".equals(normalized)) {
            throw invalidPayload("The accounting module returned an unsupported account.nature: " + value + ".");
        }
        return normalized;
    }

    private String normalizeAccountCode(String value) {
        String normalized = value != null ? value.trim() : null;
        if (!StringUtils.hasText(normalized)) {
            throw invalidPayload("The accounting module returned a movement without a valid account.code.");
        }
        return normalized;
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private AccountInfoClientException mapHttpException(URI requestUri, RestClientResponseException exception) {
        log.error(
                "The accounting module responded with status {} for request {}. Body: {}",
                exception.getStatusCode().value(),
                requestUri,
                exception.getResponseBodyAsString()
        );

        return new AccountInfoClientException(
                HttpStatus.BAD_GATEWAY,
                "The accounting module request failed with status " + exception.getStatusCode().value() + ".",
                exception
        );
    }

    private AccountInfoClientException invalidPayload(String message) {
        return new AccountInfoClientException(HttpStatus.BAD_GATEWAY, message);
    }
}
