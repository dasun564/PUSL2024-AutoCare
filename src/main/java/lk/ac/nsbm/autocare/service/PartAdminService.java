package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.PartForm;
import lk.ac.nsbm.autocare.dto.PartView;
import lk.ac.nsbm.autocare.entity.ConsumablePart;
import lk.ac.nsbm.autocare.entity.MechanicalPart;
import lk.ac.nsbm.autocare.entity.Part;
import lk.ac.nsbm.autocare.entity.PartCategory;
import lk.ac.nsbm.autocare.exception.DuplicatePartNumberException;
import lk.ac.nsbm.autocare.exception.PartNotFoundException;
import lk.ac.nsbm.autocare.repository.PartCategoryRepository;
import lk.ac.nsbm.autocare.repository.PartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inventory maintenance, restricted to garage staff.
 *
 * Layer: @Service. Holds the inventory rules - part-number uniqueness, how a
 * withdrawal is performed, how a stocktake corrects a count - so the Thymeleaf
 * screens and the REST API behave identically.
 *
 * {@code @PreAuthorize} repeats the ADMIN requirement SecurityConfig already
 * enforces on /admin/**. The duplication is deliberate defence in depth: if a
 * new controller is added and its URL is left unprotected, the service still
 * refuses.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class PartAdminService {

    private static final Logger log = LoggerFactory.getLogger(PartAdminService.class);

    private final PartRepository partRepository;
    private final PartCategoryRepository categoryRepository;

    public PartAdminService(PartRepository partRepository, PartCategoryRepository categoryRepository) {
        this.partRepository = partRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PartView> listAll() {
        return partRepository.findAllWithCategory().stream().map(PartView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PartView> listLowStock() {
        return partRepository.findLowStock().stream().map(PartView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PartCategory> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public PartView getView(Long id) {
        return PartView.from(requirePart(id));
    }

    @Transactional(readOnly = true)
    public PartForm loadFormFor(Long id) {
        Part part = requirePart(id);
        PartForm form = new PartForm();
        form.setId(part.getId());
        form.setPartNumber(part.getPartNumber());
        form.setName(part.getName());
        form.setCategoryId(part.getCategory().getId());
        form.setUnitPrice(part.getUnitPrice());
        form.setStockQuantity(part.getStockQuantity());
        form.setReorderLevel(part.getReorderLevel());

        if (part instanceof ConsumablePart consumable) {
            form.setPartType("CONSUMABLE");
            form.setShelfLifeMonths(consumable.getShelfLifeMonths());
            form.setHazardous(consumable.isHazardous());
        } else if (part instanceof MechanicalPart mechanical) {
            form.setPartType("MECHANICAL");
            form.setManufacturer(mechanical.getManufacturer());
            form.setWarrantyMonths(mechanical.getWarrantyMonths());
            form.setRequiresSpecialistFitting(mechanical.isRequiresSpecialistFitting());
        }
        return form;
    }

    @Transactional
    public PartView create(PartForm form) {
        String partNumber = form.getPartNumber().trim().toUpperCase();
        if (partRepository.existsByPartNumberIgnoreCase(partNumber)) {
            throw new DuplicatePartNumberException(partNumber);
        }

        PartCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("No category " + form.getCategoryId()));

        Part part = "CONSUMABLE".equals(form.getPartType())
                ? new ConsumablePart(partNumber, form.getName().trim(), category, form.getUnitPrice(),
                        form.getStockQuantity(), form.getReorderLevel(),
                        form.getShelfLifeMonths() == null ? 12 : form.getShelfLifeMonths(),
                        form.isHazardous())
                : new MechanicalPart(partNumber, form.getName().trim(), category, form.getUnitPrice(),
                        form.getStockQuantity(), form.getReorderLevel(),
                        form.getManufacturer() == null || form.getManufacturer().isBlank()
                                ? "Unspecified" : form.getManufacturer().trim(),
                        form.getWarrantyMonths() == null ? 0 : form.getWarrantyMonths(),
                        form.isRequiresSpecialistFitting());

        Part saved = partRepository.save(part);
        log.info("Staff created part {} ({})", saved.getPartNumber(), saved.getName());
        return PartView.from(saved);
    }

    /**
     * Updates an existing part.
     *
     * The part TYPE is not editable. It determines which subclass table holds
     * the row under the JOINED strategy, so changing it would mean deleting
     * and re-inserting the part under a new identity, orphaning every JobLine
     * that references it. The edit form renders it read-only.
     */
    @Transactional
    public PartView update(Long id, PartForm form) {
        Part part = requirePart(id);
        String partNumber = form.getPartNumber().trim().toUpperCase();

        if (!part.getPartNumber().equalsIgnoreCase(partNumber)
                && partRepository.existsByPartNumberIgnoreCase(partNumber)) {
            throw new DuplicatePartNumberException(partNumber);
        }

        PartCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("No category " + form.getCategoryId()));

        part.setPartNumber(partNumber);
        part.setName(form.getName().trim());
        part.setCategory(category);
        part.setUnitPrice(form.getUnitPrice());
        part.setReorderLevel(form.getReorderLevel());

        if (part instanceof ConsumablePart consumable && form.getShelfLifeMonths() != null) {
            consumable.setShelfLifeMonths(form.getShelfLifeMonths());
            consumable.setHazardous(form.isHazardous());
        } else if (part instanceof MechanicalPart mechanical) {
            if (form.getManufacturer() != null && !form.getManufacturer().isBlank()) {
                mechanical.setManufacturer(form.getManufacturer().trim());
            }
            if (form.getWarrantyMonths() != null) {
                mechanical.setWarrantyMonths(form.getWarrantyMonths());
            }
            mechanical.setRequiresSpecialistFitting(form.isRequiresSpecialistFitting());
        }

        log.info("Staff updated part {}", part.getPartNumber());
        return PartView.from(part);
    }

    /**
     * SOFT DELETE. The row is never physically removed, because JobLine rows
     * carry a foreign key to part_id: a hard delete would either violate that
     * constraint or, with a cascade, erase the line items of historic invoices.
     * Clearing {@code active} hides the part from the inventory pickers while
     * every past job keeps its evidence of what was fitted and what it cost.
     */
    @Transactional
    public void withdraw(Long id) {
        Part part = requirePart(id);
        part.setActive(false);
        log.info("Staff withdrew part {} from the inventory", part.getPartNumber());
    }

    @Transactional
    public void restore(Long id) {
        Part part = requirePart(id);
        part.setActive(true);
        log.info("Staff restored part {}", part.getPartNumber());
    }

    /** Stocktake correction - a separate action from editing a part's details. */
    @Transactional
    public PartView correctStock(Long id, int countedQuantity) {
        Part part = requirePart(id);
        int before = part.getStockQuantity();
        part.correctStockTo(countedQuantity);
        log.info("Stocktake adjusted part {} from {} to {}", part.getPartNumber(), before, countedQuantity);
        return PartView.from(part);
    }

    private Part requirePart(Long id) {
        return partRepository.findById(id).orElseThrow(() -> new PartNotFoundException(id));
    }
}
