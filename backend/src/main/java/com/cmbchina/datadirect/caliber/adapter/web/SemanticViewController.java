package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSemanticViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.UpdateSemanticViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SemanticViewDTO;
import com.cmbchina.datadirect.caliber.application.service.command.SemanticViewCommandAppService;
import com.cmbchina.datadirect.caliber.application.service.query.SemanticViewQueryAppService;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic-views")
@Validated
public class SemanticViewController {

    private final SemanticViewCommandAppService semanticViewCommandAppService;
    private final SemanticViewQueryAppService semanticViewQueryAppService;

    public SemanticViewController(SemanticViewCommandAppService semanticViewCommandAppService,
                                  SemanticViewQueryAppService semanticViewQueryAppService) {
        this.semanticViewCommandAppService = semanticViewCommandAppService;
        this.semanticViewQueryAppService = semanticViewQueryAppService;
    }

    @PostMapping
    public ResponseEntity<SemanticViewDTO> create(@Valid @RequestBody CreateSemanticViewCmd cmd) {
        CreateSemanticViewCmd payload = new CreateSemanticViewCmd(
                cmd.viewCode(),
                cmd.viewName(),
                cmd.domainId(),
                cmd.description(),
                cmd.fieldDefinitionsJson(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(semanticViewCommandAppService.create(payload));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SemanticViewDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateSemanticViewCmd cmd) {
        UpdateSemanticViewCmd payload = new UpdateSemanticViewCmd(
                cmd.viewName(),
                cmd.domainId(),
                cmd.description(),
                cmd.fieldDefinitionsJson(),
                SecurityOperator.currentOperator(cmd.operator())
        );
        return ResponseEntity.ok(semanticViewCommandAppService.update(id, payload));
    }

    @GetMapping
    public ResponseEntity<List<SemanticViewDTO>> list() {
        return ResponseEntity.ok(semanticViewQueryAppService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemanticViewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(semanticViewQueryAppService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        semanticViewCommandAppService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
