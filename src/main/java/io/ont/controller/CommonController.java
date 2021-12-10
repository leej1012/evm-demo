package io.ont.controller;

import io.ont.bean.Result;
import io.ont.controller.vo.MintReq;
import io.ont.controller.vo.NftReq;
import io.ont.controller.vo.TransferReq;
import io.ont.controller.vo.UserReq;
import io.ont.service.CommonService;
import io.ont.utils.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/common")
@CrossOrigin
public class CommonController {
    @Autowired
    private CommonService commonService;

    @PostMapping("/bind-account")
    public Result bindAccount(@RequestBody UserReq req) {
        String address = commonService.bindAccount(req);
        return new Result("bindAccount", Constant.SUCCESS_CODE, Constant.SUCCESS_DESC, address);
    }

    @PostMapping("/mint")
    public Result mint(@RequestBody MintReq req) {
        String txHash = commonService.mint(req);
        return new Result("mint", Constant.SUCCESS_CODE, Constant.SUCCESS_DESC, txHash);
    }

    @PostMapping("/transfer")
    public Result transfer(@RequestBody TransferReq req) {
        String txHash = commonService.transfer(req);
        return new Result("transfer", Constant.SUCCESS_CODE, Constant.SUCCESS_DESC, txHash);
    }

    @PostMapping("/burned")
    public Result burned(@RequestBody NftReq req) {
        String txHash = commonService.burn(req);
        return new Result("burned", Constant.SUCCESS_CODE, Constant.SUCCESS_DESC, txHash);
    }

    @GetMapping("/meta-data")
    public Result getMetaData(String nftId) {
        String hash = commonService.getMetaData(nftId);
        return new Result("getMetaData", Constant.SUCCESS_CODE, Constant.SUCCESS_DESC, hash);
    }
}
