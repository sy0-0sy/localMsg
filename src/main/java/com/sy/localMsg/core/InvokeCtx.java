package com.sy.localMsg.core;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class InvokeCtx {

    private String className;

    private String methodName;

    private String paramTypes;

    private String args;


}
