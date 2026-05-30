package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.merchant.MerchantResponse;
import lombok.*;

/** Response of the dev auth endpoints: the minted access token plus the merchant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevAuthResponse {

    private String accessToken;
    private MerchantResponse merchant;
}
