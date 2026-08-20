package lk.ac.nsbm.autocare.controller;

import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.service.PartCatalogueService;
import lk.ac.nsbm.autocare.service.ServiceJobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Staff workshop screens: the job diary, planning a job's parts and labour,
 * and completing it.
 *
 * Reachable only with ROLE_ADMIN, enforced by SecurityConfig on /admin/** and
 * again by the service layer.
 */
@Controller
@RequestMapping("/admin/jobs")
public class AdminJobController {

    private final ServiceJobService jobService;
    private final PartCatalogueService partCatalogue;

    public AdminJobController(ServiceJobService jobService, PartCatalogueService partCatalogue) {
        this.jobService = jobService;
        this.partCatalogue = partCatalogue;
    }

    @GetMapping
    public String diary(Model model) {
        model.addAttribute("jobs", jobService.listAllJobs());
        return "admin/job-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobService.getJob(id));
        model.addAttribute("parts", partCatalogue.search(null));
        return "admin/job-detail";
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes redirect) {
        JobView job = jobService.beginWork(id);
        redirect.addFlashAttribute("flash", "Job " + job.jobNumber() + " is now in progress.");
        return "redirect:/admin/jobs/" + id;
    }

    @PostMapping("/{id}/lines")
    public String addPart(@PathVariable Long id,
                          @RequestParam Long partId,
                          @RequestParam int quantity,
                          RedirectAttributes redirect) {
        jobService.addPartToJob(id, partId, quantity);
        redirect.addFlashAttribute("flash", "Part added to the job plan.");
        return "redirect:/admin/jobs/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/remove")
    public String removePart(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes redirect) {
        jobService.removeLineFromJob(id, lineId);
        redirect.addFlashAttribute("flash", "Part removed from the job plan.");
        return "redirect:/admin/jobs/" + id;
    }

    @PostMapping("/{id}/labour")
    public String recordLabour(@PathVariable Long id,
                               @RequestParam BigDecimal hours,
                               RedirectAttributes redirect) {
        jobService.recordLabour(id, hours);
        redirect.addFlashAttribute("flash", "Labour hours updated.");
        return "redirect:/admin/jobs/" + id;
    }

    /**
     * Completes the job. Everything interesting happens in the service: the
     * parts are consumed from stock and the invoice is raised in one
     * transaction, or nothing changes at all.
     */
    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, Model model) {
        JobView job = jobService.completeJob(id);
        model.addAttribute("job", job);
        return "admin/job-invoice";
    }
}
