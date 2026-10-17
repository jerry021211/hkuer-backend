package com.hkuer.hkuer.entity.DTO;
import lombok.Data;
import java.util.List;

@Data
public class MeshContentRequest {
    private String kind="qa";
    private String title;
    private String content="";
    private String category;
    private boolean anonymous;
    private List<String> images=List.of();
    private List<String> tags=List.of();
    private String region;
    private Integer rating;
    private String company;
    private String role;
    private Long courseId;
    private java.math.BigDecimal price;
    private boolean negotiable;
    private String brand;
    private String tradeLocation;
    private String conditionLabel;
    private String rentalType;
    private java.math.BigDecimal rent;
    private java.math.BigDecimal budgetMin;
    private java.math.BigDecimal budgetMax;
    private boolean approximate;
    private List<String> roomTypes=List.of();
    private String leaseTerm;
    private java.time.LocalDate moveIn;
    private String gender;
    private List<String> habits=List.of();
}
