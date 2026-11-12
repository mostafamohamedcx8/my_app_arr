package com.myapp.aqdeeha.security.access;

import com.myapp.aqdeeha.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.CUSTOMER;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.SUPER_ADMIN;

/** Test-only endpoints exercising every access-rule combination. */
final class AccessProbeControllers {

    private AccessProbeControllers() {
    }

    @RestController
    @RequestMapping("/api/test/access")
    public static class MethodLevel {

        @PublicEndpoint
        @GetMapping("/public")
        public String open() {
            return "public";
        }

        @Protect
        @GetMapping("/protected")
        public String anyLoggedIn() {
            return CurrentUser.get().role().name();
        }

        @AllowTo(ADMIN)
        @GetMapping("/admin")
        public String admin() {
            return CurrentUser.get().role().name();
        }

        @AllowTo(SUPER_ADMIN)
        @GetMapping("/super")
        public String superOnly() {
            return "super";
        }

        @AllowTo(CUSTOMER)
        @GetMapping("/customer")
        public String customer() {
            return "customer";
        }
    }

    /** Class-level rule with a method-level override. */
    @RestController
    @AllowTo(CUSTOMER)
    @RequestMapping("/api/test/class")
    public static class ClassLevel {

        @GetMapping("/inherited")
        public String inherited() {
            return "inherited";
        }

        @Protect
        @GetMapping("/override")
        public String override() {
            return "override";
        }
    }
}
