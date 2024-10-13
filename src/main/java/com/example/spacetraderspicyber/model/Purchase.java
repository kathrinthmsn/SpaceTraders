package com.example.spacetraderspicyber.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Purchase {

    private PurchaseData data;

    @Setter
    @Getter
    @NoArgsConstructor
    public static class PurchaseData {
        private Agent agent;
        private Cargo cargo;
        private Transaction transaction;
    }
}
