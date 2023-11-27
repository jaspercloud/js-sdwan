package io.jaspercloud.sdwan.adapter.controller.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result<T> {

    public static final Result OK = new Result(0, "ok", null);

    private Integer code;
    private String msg;
    private T data;
}
