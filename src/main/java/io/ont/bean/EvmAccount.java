package io.ont.bean;

import lombok.Data;


@Data
public class EvmAccount {
    private String userId;
    private String address;
    private String publicKey;
    private String privateKey;
}
