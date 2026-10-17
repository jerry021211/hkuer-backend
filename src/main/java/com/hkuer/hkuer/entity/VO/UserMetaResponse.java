package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserMetaResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String nickName;
}
