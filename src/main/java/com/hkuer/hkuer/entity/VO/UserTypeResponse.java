package com.hkuer.hkuer.entity.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserTypeResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer userType;
}
