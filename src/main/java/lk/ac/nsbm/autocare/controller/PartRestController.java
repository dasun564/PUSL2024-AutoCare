package lk.ac.nsbm.autocare.controller;

import jakarta.validation.Valid;
import lk.ac.nsbm.autocare.dto.PartForm;
import lk.ac.nsbm.autocare.dto.PartView;
import lk.ac.nsbm.autocare.service.PartAdminService;
import lk.ac.nsbm.autocare.service.PartCatalogueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * RESTful API over the parts inventory, satisfying the requirement for CRUD
 * through REST controllers alongside the Thymeleaf interface.
 *
 * It calls the same service beans as the web screens, so the business rules -
 * part-number uniqueness, soft delete, stock integrity - cannot drift apart
 * between the two front ends. Failures become JSON via RestExceptionHandler;
 * there is no try/catch here either.
 *
 * Reads need any authenticated user; writes need ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/parts")
public class PartRestController {

    private final PartCatalogueService catalogueService;
    private final PartAdminService partAdminService;

    public PartRestController(PartCatalogueService catalogueService, PartAdminService partAdminService) {
        this.catalogueService = catalogueService;
        this.partAdminService = partAdminService;
    }

    @GetMapping
    public List<PartView> search(@RequestParam(value = "q", required = false) String query) {
        return catalogueService.search(query);
    }

    @GetMapping("/{id}")
    public PartView getOne(@PathVariable Long id) {
        return catalogueService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PartView> create(@Valid @RequestBody PartForm form,
                                           UriComponentsBuilder uriBuilder) {
        PartView created = partAdminService.create(form);
        return ResponseEntity
                .created(uriBuilder.path("/api/parts/{id}").build(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public PartView update(@PathVariable Long id, @Valid @RequestBody PartForm form) {
        return partAdminService.update(id, form);
    }

    /** Soft delete, matching the web interface. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        partAdminService.withdraw(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
