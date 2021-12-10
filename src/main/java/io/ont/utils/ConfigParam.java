package io.ont.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;


@Service("ConfigParam")
public class ConfigParam {

    @Value("${evm.web3j.url}")
    public String EVM_WEB3J_URL;

    @Value("${evm.gas.limit}")
    public BigInteger EVM_GAS_LIMIT;

    @Value("${evm.gas.price}")
    public BigInteger EVM_GAS_PRICE;

    @Value("${nft.contract}")
    public String NFT_CONTRACT;

    @Value("${evm.chain.id}")
    public long EVM_CHAIN_ID;
}