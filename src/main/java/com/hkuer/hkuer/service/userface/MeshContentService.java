package com.hkuer.hkuer.service.userface;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.MeshContentRequest;
import com.hkuer.hkuer.entity.VO.MeshPage;
import java.util.*;
public interface MeshContentService {
    MeshPage<Map<String,Object>> feed(String kind,String keyword,String tag,String category,String author,String region,Long courseId,String sort,int current,int size);
    Map<String,Object> detail(long id);
    Result create(MeshContentRequest request);
    Map<String,Object> editable(long id);
    Result update(long id, MeshContentRequest request);
    List<Map<String,Object>> answers(long id);
    String answer(long id,String content,boolean anonymous);
    void updateAnswer(long id,String content,boolean anonymous);
    void interact(long id,String kind,boolean enabled);
    void answerAction(long id,String kind,boolean enabled);
    Map<String,Object> home();
    Map<String,Object> pageFeedback(String page);
    void pageFeedback(String page,boolean helpful);
    void status(long id,String status);
    List<Result> batch(List<MeshContentRequest> requests,String requestKey);
    List<Map<String,Object>> matches(long id);
}
