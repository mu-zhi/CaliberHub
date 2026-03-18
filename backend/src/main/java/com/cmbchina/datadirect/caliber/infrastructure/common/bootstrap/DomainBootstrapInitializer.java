package com.cmbchina.datadirect.caliber.infrastructure.common.bootstrap;

import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DomainBootstrapInitializer implements ApplicationRunner {

    private final CaliberDomainSupport caliberDomainSupport;

    public DomainBootstrapInitializer(CaliberDomainSupport caliberDomainSupport) {
        this.caliberDomainSupport = caliberDomainSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!caliberDomainSupport.findAllOrderBySortOrder().isEmpty()) {
            return;
        }
        CaliberDomain fallbackDomain = CaliberDomain.create(
                "UNCLASSIFIED",
                "未分类业务领域",
                "系统启动时自动创建的默认业务领域，建议在业务领域管理中维护正式业务领域后再迁移场景。",
                "",
                "",
                9999,
                "system"
        );
        caliberDomainSupport.save(fallbackDomain);
    }
}
