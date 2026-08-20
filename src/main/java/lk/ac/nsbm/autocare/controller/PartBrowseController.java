package lk.ac.nsbm.autocare.controller;

import lk.ac.nsbm.autocare.service.PartCatalogueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only parts price list, open to any signed-in user so a customer can
 * check what a part costs before booking.
 */
@Controller
public class PartBrowseController {

    private final PartCatalogueService catalogueService;

    public PartBrowseController(PartCatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/parts")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        model.addAttribute("parts", catalogueService.search(query));
        model.addAttribute("categories", catalogueService.listCategories());
        model.addAttribute("query", query);
        return "parts/list";
    }
}
