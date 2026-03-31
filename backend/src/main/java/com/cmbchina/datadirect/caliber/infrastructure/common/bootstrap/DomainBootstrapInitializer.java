package com.cmbchina.datadirect.caliber.infrastructure.common.bootstrap;

import com.cmbchina.datadirect.caliber.application.service.command.DomainCommandAppService;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DomainBootstrapInitializer implements ApplicationRunner {

    private final CaliberDomainSupport caliberDomainSupport;
    private final DomainCommandAppService domainCommandAppService;

    public DomainBootstrapInitializer(CaliberDomainSupport caliberDomainSupport,
                                      DomainCommandAppService domainCommandAppService) {
        this.caliberDomainSupport = caliberDomainSupport;
        this.domainCommandAppService = domainCommandAppService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!caliberDomainSupport.findAllOrderBySortOrder().isEmpty()) {
            return;
        }
        domainCommandAppService.bootstrapFromBusinessCategories("system");
    }
}
