package com.caliberhub.infrastructure.scene.supportimpl;

import com.caliberhub.domain.scene.model.Domain;
import com.caliberhub.domain.scene.support.DomainRepository;
import com.caliberhub.infrastructure.scene.dao.mapper.DomainMapper;
import com.caliberhub.infrastructure.scene.dao.po.DomainPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Optional;

/**
 * 业务领域仓储实现
 */
@Repository
@RequiredArgsConstructor
public class DomainRepositoryImpl implements DomainRepository {

    private final DomainMapper domainMapper;

    // 支持多种日期格式：ISO格式(T分隔)和SQLite格式(空格分隔)
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral(' ')
            .appendPattern("HH:mm:ss")
            .toFormatter();

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null)
            return null;
        try {
            return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        }
    }

    @Override
    public Optional<Domain> findById(String id) {
        return domainMapper.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Domain> findByDomainKey(String domainKey) {
        return domainMapper.findByDomainKey(domainKey).map(this::toDomain);
    }

    @Override
    public List<Domain> findAll() {
        return domainMapper.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Domain domain) {
        DomainPO po = toPO(domain);
        domainMapper.save(po);
    }

    @Override
    public void delete(String id) {
        domainMapper.deleteById(id);
    }

    private Domain toDomain(DomainPO po) {
        return Domain.builder()
                .id(po.getId())
                .domainKey(po.getDomainKey())
                .name(po.getName())
                .description(po.getDescription())
                .createdBy(po.getCreatedBy())
                .createdAt(parseDateTime(po.getCreatedAt()))
                .updatedBy(po.getUpdatedBy())
                .updatedAt(parseDateTime(po.getUpdatedAt()))
                .build();
    }

    private DomainPO toPO(Domain domain) {
        return DomainPO.builder()
                .id(domain.getId())
                .domainKey(domain.getDomainKey())
                .name(domain.getName())
                .description(domain.getDescription())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt().format(FORMATTER))
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt().format(FORMATTER))
                .build();
    }
}
