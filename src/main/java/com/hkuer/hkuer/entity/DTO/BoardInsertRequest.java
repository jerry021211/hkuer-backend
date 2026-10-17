package com.hkuer.hkuer.entity.DTO;
import lombok.Data;
import java.io.Serializable;

@Data
public class BoardInsertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String slug;
    private String name;
}
