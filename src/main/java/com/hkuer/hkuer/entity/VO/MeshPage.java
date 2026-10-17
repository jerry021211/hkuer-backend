package com.hkuer.hkuer.entity.VO;
import java.util.List;
public record MeshPage<T>(List<T> records,long total,int current,int size,long pages) {
    public MeshPage(List<T> records,long total,int current,int size) {this(records,total,current,size,(total+size-1)/size);}
}
