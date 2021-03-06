package io.ont.service.impl;

import com.alibaba.fastjson.JSON;
import io.ont.bean.EvmAccount;
import io.ont.controller.vo.MintReq;
import io.ont.controller.vo.NftReq;
import io.ont.controller.vo.TransferReq;
import io.ont.controller.vo.UserReq;
import io.ont.exception.EvmDemoException;
import io.ont.service.CommonService;
import io.ont.utils.ConfigParam;
import io.ont.utils.Constant;
import io.ont.utils.Web3jSdkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class CommonServiceImpl implements CommonService {
    private Map<String, EvmAccount> userAccountMap = new ConcurrentHashMap<>();

    @Autowired
    private ConfigParam configParam;
    @Autowired
    private Web3jSdkUtil web3jSdkUtil;

    @Override
    public String bindAccount(UserReq req) {
        String userId = req.getUserId();
        String publicKey = null;
        try {
            EvmAccount account = web3jSdkUtil.createAccount();
            account.setUserId(userId);
            publicKey = account.getPublicKey();
            // save in db
            userAccountMap.put(publicKey, account);
        } catch (Exception e) {
            log.error("bindAccount error", e);
        }
        return publicKey;
    }

    @Override
    public String mint(MintReq req) {
        String publicKey = req.getPublicKey();
        String hash = req.getHash();
        EvmAccount account = userAccountMap.get(publicKey);
        if (account == null) {
            throw new EvmDemoException("user not bind evm account");
        }
        // ??????????????????&??????
        String address = account.getAddress();
        String privateKey = account.getPrivateKey();
        Credentials credentials = Credentials.create(privateKey);
        try {
            // ????????????
            byte[] bytes = Numeric.hexStringToByteArray(hash);
            List<Type> params = Arrays.asList(new Bytes32(bytes));
            // ??????????????????????????????
            String txHash = makeAndSendTransaction(Constant.MINT, params, address, credentials);
            // ????????????receipt
            TransactionReceipt receipt = getReceipt(txHash);
            // ??????receipt,??????nftId
            String nftId = getNftIdFromReceipt(receipt);
            return nftId;
        } catch (Exception e) {
            log.error("mint nft error", e);
        }
        return null;
    }

    @Override
    public String transfer(TransferReq req) {
        String fromPublicKey = req.getFromPublicKey();
        String toPublicKey = req.getToPublicKey();
        String nftId = req.getNftId();

        EvmAccount fromAccount = userAccountMap.get(fromPublicKey);
        EvmAccount toAccount = userAccountMap.get(toPublicKey);
        if (fromAccount == null || toAccount == null) {
            throw new EvmDemoException("user not bind evm account");
        }
        // ??????????????????&??????
        String address = fromAccount.getAddress();
        String privateKey = fromAccount.getPrivateKey();
        Credentials credentials = Credentials.create(privateKey);
        String toAddress = toAccount.getAddress();
        try {
            // ????????????
            List<Type> params = Arrays.asList(new Address(toAddress), new Uint256(new BigInteger(nftId)));
            // ??????????????????????????????
            String txHash = makeAndSendTransaction(Constant.TRANSFER, params, address, credentials);
            // ????????????receipt???????????????????????????
            boolean transactionSuccess = checkTransactionStatus(txHash);
            return transactionSuccess ? txHash : null;
        } catch (Exception e) {
            log.error("transfer nft error", e);
        }
        return null;
    }

    @Override
    public String burn(NftReq req) {
        String publicKey = req.getPublicKey();
        String nftId = req.getNftId();
        EvmAccount account = userAccountMap.get(publicKey);
        if (account == null) {
            throw new EvmDemoException("user not bind evm account");
        }
        // ??????????????????&??????
        String address = account.getAddress();
        String privateKey = account.getPrivateKey();
        Credentials credentials = Credentials.create(privateKey);
        try {
            // ????????????
            List<Type> params = Arrays.asList(new Uint256(new BigInteger(nftId)));
            // ??????????????????????????????
            String txHash = makeAndSendTransaction(Constant.BURN, params, address, credentials);
            // ????????????receipt???????????????????????????
            boolean transactionSuccess = checkTransactionStatus(txHash);
            return transactionSuccess ? txHash : null;
        } catch (Exception e) {
            log.error("burned nft error", e);
        }
        return null;
    }

    @Override
    public String getMetaData(String nftId) {
        String hash = null;
        try {
            // ????????????
            List<Type> params = Arrays.asList(new Uint256(new BigInteger(nftId)));
            TypeReference<Bytes32> reserve = new TypeReference<Bytes32>() {
            };
            List<TypeReference<?>> outputParameters = Arrays.asList(reserve);
            List<Type> result = web3jSdkUtil.sendPreTransactionAndDecode(configParam.NFT_CONTRACT, Constant.GET_META_DATA, Constant.ETH_PRE_ADDRESS, params, outputParameters);
            byte[] value = ((Bytes32) result.get(0)).getValue();
            hash = Numeric.toHexStringNoPrefix(value);
        } catch (Exception e) {
            log.error("get nft meta data error", e);
        }
        return hash;
    }

    @Override
    public String getNftOwner(String nftId) {
        String owner = null;
        try {
            // ????????????
            List<Type> params = Arrays.asList(new Uint256(new BigInteger(nftId)));
            TypeReference<Address> reserve = new TypeReference<Address>() {
            };
            List<TypeReference<?>> outputParameters = Arrays.asList(reserve);
            List<Type> result = web3jSdkUtil.sendPreTransactionAndDecode(configParam.NFT_CONTRACT, Constant.OWNER_OF, Constant.ETH_PRE_ADDRESS, params, outputParameters);
            owner = result.get(0).getValue().toString();
        } catch (Exception e) {
            log.error("get nft meta data error", e);
        }
        return owner;
    }

    private String makeAndSendTransaction(String function, List<Type> params, String address, Credentials credentials) throws Exception {
        // ????????????
        RawTransaction transaction = web3jSdkUtil.createEvmTransaction(configParam.NFT_CONTRACT, function, address, params);
        // ????????????
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, configParam.EVM_CHAIN_ID, credentials);
        String txHex = Numeric.toHexString(signedMessage);
        // ??????????????????
        String txHash = web3jSdkUtil.sendEvmTransaction(txHex);
        return txHash;
    }

    private boolean checkTransactionStatus(String txHash) throws Exception {
        boolean success = false;
        TransactionReceipt receipt = getReceipt(txHash);
        String status = receipt.getStatus();
        if (Constant.STATUS_SUCCESS.equals(status)) {
            success = true;
        }
        return success;
    }

    private TransactionReceipt getReceipt(String txHash) throws Exception {
        TransactionReceipt receipt = null;
        int times = 0;
        while (times < 20) {
            receipt = web3jSdkUtil.getReceiptByHash(txHash);
            if (receipt == null) {
                // ????????????????????????,?????????????????????
                times++;
                Thread.sleep(1000);
                continue;
            }
            log.info("receipt:{}", JSON.toJSONString(receipt));
            break;
        }
        return receipt;
    }

    private String getNftIdFromReceipt(TransactionReceipt receipt) throws Exception {
        String nftId = null;
        if (receipt != null) {
            List<Log> logs = receipt.getLogs();
            for (Log txLog : logs) {
                String address = txLog.getAddress();
                if (configParam.NFT_CONTRACT.equalsIgnoreCase(address)) {
                    List<String> topics = txLog.getTopics();
                    String eventHash = topics.get(0);
                    if (Constant.TRANSFER_EVENT_HASH.equals(eventHash)) {
                        String value = topics.get(3);
                        BigInteger id = Numeric.toBigInt(value);
                        nftId = id.toString();
                    }
                }
            }
        }
        return nftId;
    }
}
