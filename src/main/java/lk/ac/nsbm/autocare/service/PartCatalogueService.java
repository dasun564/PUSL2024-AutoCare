package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.PartView;
import lk.ac.nsbm.autocare.entity.PartCategory;
import lk.ac.nsbm.autocare.exception.PartNotFoundException;
import lk.ac.nsbm.autocare.repository.PartCategoryRepository;
import lk.ac.nsbm.autocare.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only access to the parts inventory for any signed-in user.
 *
 * Kept separate from {@link PartAdminService} so that browsing does not sit
 * behind the ADMIN-only {@code @PreAuthorize} on that class - customers may
 * look up a part's price, but only staff may change one.
 */
@Service
public class PartCatalogueService {

    private final PartRepository partRepository;
    private final PartCategoryRepository categoryRepository;

    public PartCatalogueService(PartRepository partRepository, PartCategoryRepository categoryRepository) {
        this.partRepository = partRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PartView> search(String term) {
        List<lk.ac.nsbm.autocare.entity.Part> found = (term == null || term.isBlank())
                ? partRepository.findAllActiveWithCategory()
                : partRepository.searchActive(term.trim());
        return found.stream().map(PartView::from).toList();
    }

    @Transactional(readOnly = true)
    public PartView getById(Long id) {
        return partRepository.findByIdAndActiveTrue(id)
                .map(PartView::from)
                .orElseThrow(() -> new PartNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<PartCategory> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }
}
