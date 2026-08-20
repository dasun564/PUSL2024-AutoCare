package lk.ac.nsbm.autocare.controller;

import jakarta.validation.Valid;
import lk.ac.nsbm.autocare.dto.PartForm;
import lk.ac.nsbm.autocare.exception.DuplicatePartNumberException;
import lk.ac.nsbm.autocare.service.PartAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Inventory management screens (CRUD with Bean Validation).
 *
 * Layer: @Controller. It decides which view to render and re-displays the form
 * when validation rejects the input; it never decides what a valid part is -
 * those constraints live on PartForm and the uniqueness rule lives in
 * PartAdminService.
 */
@Controller
@RequestMapping("/admin/parts")
public class AdminPartController {

    private final PartAdminService partAdminService;

    public AdminPartController(PartAdminService partAdminService) {
        this.partAdminService = partAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("parts", partAdminService.listAll());
        model.addAttribute("lowStock", partAdminService.listLowStock());
        return "admin/part-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("partForm", new PartForm());
        model.addAttribute("categories", partAdminService.listCategories());
        model.addAttribute("editing", false);
        return "admin/part-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("partForm") PartForm partForm,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return backToForm(model, false);
        }
        try {
            var saved = partAdminService.create(partForm);
            redirect.addFlashAttribute("flash", "Added " + saved.partNumber() + " - " + saved.name() + ".");
            return "redirect:/admin/parts";
        } catch (DuplicatePartNumberException ex) {
            binding.rejectValue("partNumber", "duplicate", ex.getUserMessage());
            return backToForm(model, false);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("partForm", partAdminService.loadFormFor(id));
        return backToForm(model, true);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("partForm") PartForm partForm,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return backToForm(model, true);
        }
        try {
            var saved = partAdminService.update(id, partForm);
            redirect.addFlashAttribute("flash", "Updated " + saved.partNumber() + ".");
            return "redirect:/admin/parts";
        } catch (DuplicatePartNumberException ex) {
            binding.rejectValue("partNumber", "duplicate", ex.getUserMessage());
            return backToForm(model, true);
        }
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, RedirectAttributes redirect) {
        var part = partAdminService.getView(id);
        partAdminService.withdraw(id);
        redirect.addFlashAttribute("flash",
                "Withdrew " + part.partNumber() + " from the inventory. Job history has been preserved.");
        return "redirect:/admin/parts";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirect) {
        partAdminService.restore(id);
        redirect.addFlashAttribute("flash", "Part restored to the inventory.");
        return "redirect:/admin/parts";
    }

    @PostMapping("/{id}/stocktake")
    public String stocktake(@PathVariable Long id,
                            @RequestParam int countedQuantity,
                            RedirectAttributes redirect) {
        var updated = partAdminService.correctStock(id, countedQuantity);
        redirect.addFlashAttribute("flash",
                "Stocktake: " + updated.partNumber() + " now recorded at " + updated.stockQuantity() + ".");
        return "redirect:/admin/parts";
    }

    private String backToForm(Model model, boolean editing) {
        model.addAttribute("categories", partAdminService.listCategories());
        model.addAttribute("editing", editing);
        return "admin/part-form";
    }
}
