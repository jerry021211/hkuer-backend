package com.hkuer.hkuer.entity.DTO;

import lombok.Data;

import java.io.Serializable;

@Data
public class PostInsertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String content;
    private String boardName;
}
