package com.caliberhub.domain.scene.support;

import com.caliberhub.domain.scene.model.Domain;

import java.util.List;
import java.util.Optional;

/**
 * 业务领域仓储接口
 */
public interface DomainRepository {

    /**
     * 根据ID查询
     */
    Optional<Domain> findById(String id);

    /**
     * 根据领域标识查询
     */
    Optional<Domain> findByDomainKey(String domainKey);

    /**
     * 查询所有领域
     */
    List<Domain> findAll();

    /**
     * 保存领域
     */
    /**
     * 保存领域
     */
    void save(Domain domain);

    /**
     * 删除领域
     */
    void delete(String id);
}
