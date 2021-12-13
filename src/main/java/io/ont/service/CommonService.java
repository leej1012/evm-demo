package io.ont.service;


import io.ont.controller.vo.MintReq;
import io.ont.controller.vo.NftReq;
import io.ont.controller.vo.TransferReq;
import io.ont.controller.vo.UserReq;


public interface CommonService {

    String bindAccount(UserReq req);

    String mint(MintReq req);

    String transfer(TransferReq req);

    String burn(NftReq req);

    String getMetaData(String nftId);

    String getNftOwner(String nftId);

}
