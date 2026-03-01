package com.cmbchina.datadirect.caliber.domain.model;

import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;

import java.time.OffsetDateTime;
import java.util.Locale;

public class CaliberDomain {

    private Long id;
    private String domainCode;
    private String domainName;
    private String domainOverview;
    private String commonTables;
    private String contacts;
    private Integer sortOrder;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    private CaliberDomain() {
    }

    public static CaliberDomain create(String domainCode,
                                       String domainName,
                                       String domainOverview,
                                       String commonTables,
                                       String contacts,
                                       Integer sortOrder,
                                       String operator) {
        String normalizedCode = normalizeCode(domainCode);
        String normalizedName = normalizeText(domainName);
        String normalizedOperator = normalizeText(operator);

        if (normalizedCode.isEmpty()) {
            throw new DomainValidationException("domainCode must not be blank");
        }
        if (normalizedName.isEmpty()) {
            throw new DomainValidationException("domainName must not be blank");
        }
        if (normalizedOperator.isEmpty()) {
            throw new DomainValidationException("operator must not be blank");
        }

        OffsetDateTime now = OffsetDateTime.now();
        CaliberDomain domain = new CaliberDomain();
        domain.domainCode = normalizedCode;
        domain.domainName = normalizedName;
        domain.domainOverview = normalizeText(domainOverview);
        domain.commonTables = normalizeText(commonTables);
        domain.contacts = normalizeText(contacts);
        domain.sortOrder = normalizeSortOrder(sortOrder);
        domain.createdBy = normalizedOperator;
        domain.createdAt = now;
        domain.updatedBy = normalizedOperator;
        domain.updatedAt = now;
        return domain;
    }

    public void update(String domainCode,
                       String domainName,
                       String domainOverview,
                       String commonTables,
                       String contacts,
                       Integer sortOrder,
                       String operator) {
        String normalizedCode = normalizeCode(domainCode);
        String normalizedName = normalizeText(domainName);
        String normalizedOperator = normalizeText(operator);

        if (normalizedCode.isEmpty()) {
            throw new DomainValidationException("domainCode must not be blank");
        }
        if (normalizedName.isEmpty()) {
            throw new DomainValidationException("domainName must not be blank");
        }
        if (normalizedOperator.isEmpty()) {
            throw new DomainValidationException("operator must not be blank");
        }

        this.domainCode = normalizedCode;
        this.domainName = normalizedName;
        this.domainOverview = normalizeText(domainOverview);
        this.commonTables = normalizeText(commonTables);
        this.contacts = normalizeText(contacts);
        this.sortOrder = normalizeSortOrder(sortOrder);
        this.updatedBy = normalizedOperator;
        this.updatedAt = OffsetDateTime.now();
    }

    private static String normalizeCode(String domainCode) {
        return normalizeText(domainCode).toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final CaliberDomain instance = new CaliberDomain();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder domainCode(String domainCode) {
            instance.domainCode = domainCode;
            return this;
        }

        public Builder domainName(String domainName) {
            instance.domainName = domainName;
            return this;
        }

        public Builder domainOverview(String domainOverview) {
            instance.domainOverview = domainOverview;
            return this;
        }

        public Builder commonTables(String commonTables) {
            instance.commonTables = commonTables;
            return this;
        }

        public Builder contacts(String contacts) {
            instance.contacts = contacts;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            instance.sortOrder = sortOrder;
            return this;
        }

        public Builder createdBy(String createdBy) {
            instance.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Builder updatedBy(String updatedBy) {
            instance.updatedBy = updatedBy;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            instance.updatedAt = updatedAt;
            return this;
        }

        public CaliberDomain build() {
            return instance;
        }
    }

    public Long getId() {
        return id;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainOverview() {
        return domainOverview;
    }

    public String getCommonTables() {
        return commonTables;
    }

    public String getContacts() {
        return contacts;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
