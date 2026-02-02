package com.caliberhub.adapter.scene.web;

import com.caliberhub.domain.scene.model.Domain;
import com.caliberhub.domain.scene.support.DomainRepository;
import com.caliberhub.infrastructure.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 领域控制器
 */
@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainRepository domainRepository;

    /**
     * 获取领域列表
     */
    @GetMapping
    public ApiResponse<List<Domain>> list() {
        List<Domain> domains = domainRepository.findAll();
        return ApiResponse.success(domains);
    }

    /**
     * 创建领域
     */
    @PostMapping
    public ApiResponse<Domain> create(@RequestBody CreateDomainRequest request) {
        // Simple validation
        if (request.domainKey() == null || request.domainKey().isBlank() ||
                request.name() == null || request.name().isBlank()) {
            return ApiResponse.error("400", "Domain key and name are required");
        }

        Domain domain = Domain.create(request.domainKey(), request.name(), request.description(), "user"); // TODO: User
                                                                                                           // from
                                                                                                           // context
        domainRepository.save(domain);
        return ApiResponse.success(domain);
    }

    /**
     * 更新领域
     */
    @PutMapping("/{id}")
    public ApiResponse<Domain> update(@PathVariable String id, @RequestBody UpdateDomainRequest request) {
        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domain not found"));

        domain.update(request.name(), request.description(), "user"); // TODO: User
        domainRepository.save(domain);
        return ApiResponse.success(domain);
    }

    /**
     * 删除领域
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        domainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domain not found"));
        domainRepository.delete(id);
        return ApiResponse.success(null);
    }

    public record CreateDomainRequest(String domainKey, String name, String description) {
    }

    public record UpdateDomainRequest(String name, String description) {
    }
}
