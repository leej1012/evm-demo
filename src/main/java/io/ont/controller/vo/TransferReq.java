package io.ont.controller.vo;

import lombok.Data;


@Data
public class TransferReq {
    private String fromPublicKey;
    private String toPublicKey;
    private String nftId;
}
