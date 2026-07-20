package org.apache.fineract.los.api;

import org.apache.fineract.los.security.CustomerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/me")
public class CustomerProfileController {

  public record CustomerProfileResponse(Long clientId, String displayName) {}

  @GetMapping
  public CustomerProfileResponse me(@AuthenticationPrincipal CustomerPrincipal principal) {
    return new CustomerProfileResponse(principal.getClientId(), principal.getDisplayName());
  }
}
