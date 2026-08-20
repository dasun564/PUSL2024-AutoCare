package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.entity.AppUser;
import lk.ac.nsbm.autocare.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges AutoCare accounts into Spring Security.
 *
 * The polymorphic {@code AppUser.getRole()} means this class never tests which
 * subclass it loaded - each account type reports its own authority, so adding
 * a third kind of user would need no change here.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account named " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())      // becomes ROLE_CUSTOMER or ROLE_ADMIN
                .build();
    }
}
