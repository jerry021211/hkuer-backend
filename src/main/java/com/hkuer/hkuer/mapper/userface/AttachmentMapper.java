package com.hkuer.hkuer.mapper.userface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkuer.hkuer.entity.PO.Attachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
    @org.apache.ibatis.annotations.Update("UPDATE attachment SET owner_id=#{user},original_name=#{name},content_type=#{type},size_bytes=#{size} WHERE url=#{url}")
    void bindOwner(@org.apache.ibatis.annotations.Param("url")String url,@org.apache.ibatis.annotations.Param("user")String user,@org.apache.ibatis.annotations.Param("name")String name,@org.apache.ibatis.annotations.Param("type")String type,@org.apache.ibatis.annotations.Param("size")long size);
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM attachment WHERE url=#{url} AND owner_id=#{user} AND status=0")
    int pendingOwned(@org.apache.ibatis.annotations.Param("url")String url,@org.apache.ibatis.annotations.Param("user")String user);
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM attachment WHERE url=#{url} AND owner_id=#{user}")
    int owned(@org.apache.ibatis.annotations.Param("url")String url,@org.apache.ibatis.annotations.Param("user")String user);
}
