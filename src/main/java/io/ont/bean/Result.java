package io.ont.bean;

import lombok.Data;

@Data
public class Result {
    public String action;
    public int error;
    public String desc;
    public Object result;
    public String version;

    public Result(String action, int error, String desc, Object result) {
        this.action = action;
        this.error = error;
        this.desc = desc;
        this.result = result;
        this.version = "v1";
    }

}
