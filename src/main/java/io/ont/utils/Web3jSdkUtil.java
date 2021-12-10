package io.ont.utils;

import io.ont.bean.EvmAccount;
import io.ont.exception.EvmDemoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
public class Web3jSdkUtil {

    @Autowired
    private ConfigParam configParam;

    private Web3j web3jSingleton;

    private Web3j getWeb3jSingleton() {
        if (web3jSingleton == null) {
            synchronized (Web3jSdkUtil.class) {
                if (web3jSingleton == null) {
                    web3jSingleton = Web3j.build(new HttpService(configParam.EVM_WEB3J_URL));
                }
            }
        }
        return web3jSingleton;
    }

    /**
     * 创建账户地址及私钥
     * @return
     * @throws Exception
     */
    public EvmAccount createAccount() throws Exception {
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        Credentials credentials = Credentials.create(ecKeyPair);
        String address = credentials.getAddress();
        BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
        BigInteger publicKey = credentials.getEcKeyPair().getPublicKey();
        String privateKeyStr = Numeric.toHexStringWithPrefix(privateKey);
        String publicKeyStr = Numeric.toHexStringWithPrefix(publicKey);
        EvmAccount account = new EvmAccount();
        account.setAddress(address);
        account.setPublicKey(publicKeyStr);
        account.setPrivateKey(privateKeyStr);
        return account;
    }

    /**
     * 构造交易
     * @param contract
     * @param name
     * @param address
     * @param params
     * @return
     * @throws Exception
     */
    public RawTransaction createEvmTransaction(String contract, String name, String address, List<Type> params) throws Exception {
        Web3j web3j = getWeb3jSingleton();
        List<TypeReference<?>> typeReferences = Arrays.asList(new TypeReference<Type<String>>() {
        });
        Function function = new Function(name, params, typeReferences);
        String transactionData = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
        web3j.shutdown();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        BigInteger gasLimit = configParam.EVM_GAS_LIMIT;
        BigInteger gasPrice = configParam.EVM_GAS_PRICE;
        RawTransaction transaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contract, transactionData);
        return transaction;
    }

    /**
     * 发送交易
     * @param txHex
     * @return
     * @throws Exception
     */
    public String sendEvmTransaction(String txHex) throws Exception {
        Web3j web3j = getWeb3jSingleton();
        EthSendTransaction send = web3j.ethSendRawTransaction(txHex).send();
        Response.Error error = send.getError();
        if (error != null) {
            int code = error.getCode();
            if (code != 0) {
                throw new EvmDemoException(error.getMessage());
            }
        }
        return send.getTransactionHash();
    }

    /**
     * 根据交易hash查询交易的收据
     * @param hash
     * @return
     * @throws IOException
     */
    public TransactionReceipt getReceiptByHash(String hash) throws IOException {
        Web3j web3j = getWeb3jSingleton();
        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(hash).send();
        web3j.shutdown();
        Optional<TransactionReceipt> transactionReceiptOptional = receipt.getTransactionReceipt();
        if (transactionReceiptOptional.isPresent()) {
            TransactionReceipt transactionReceipt = transactionReceiptOptional.get();
            return transactionReceipt;
        } else {
            return null;
        }
    }

    /**
     * 预执行查询合约中的信息
     * @param contract
     * @param name
     * @param address
     * @param params
     * @param outputParameters
     * @return
     * @throws Exception
     */
    public List<Type> sendPreTransactionAndDecode(String contract, String name, String address, List<Type> params, List<TypeReference<?>> outputParameters) throws Exception {
        Web3j web3j = getWeb3jSingleton();
        Function function = new Function(name, params, outputParameters);
        String transactionData = FunctionEncoder.encode(function);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(address, contract, transactionData);
        EthCall ethCall = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        web3j.shutdown();
        Response.Error error = ethCall.getError();
        if (error != null) {
            String errorMessage = error.getMessage();
            log.error("error when invoke contract:{},name:{},error:{}", contract, name, errorMessage);
            throw new EvmDemoException(name);
        }
        List<Type> result = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        return result;
    }

    /**
     * 查询最新区块高度
     * @return
     * @throws IOException
     */
    public BigInteger getLatestBlockNumber() throws IOException {
        Web3j web3j = getWeb3jSingleton();
        EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
        web3j.shutdown();
        return ethBlockNumber.getBlockNumber();
    }
}
