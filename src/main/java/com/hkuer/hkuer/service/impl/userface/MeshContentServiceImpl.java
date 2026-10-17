package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.*;
import com.hkuer.hkuer.entity.VO.MeshPage;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.mapper.userface.*;
import com.hkuer.hkuer.service.userface.*;
import com.hkuer.hkuer.exception.BusinessException;
import com.hkuer.hkuer.utils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.util.*;

@Service @RequiredArgsConstructor
public class MeshContentServiceImpl implements MeshContentService {
    private final MeshContentMapper mapper;
    private final UserMapper userMapper;
    private final PostService postService;
    private final MuteService muteService;
    private final ContentSecurityService contentSecurityService;
    private final SnowflakeIdGenerator ids;
    private final ObjectMapper json;
    private final ViewCountService views;
    private final AttachmentMapper attachments;
    private final OssService storage;
    private final MeshCourseMapper courseMapper;
    private void checkPage(String page){if(!"app-guide".equals(page))throw new BusinessException("404","页面不存在");}
    public Map<String,Object> pageFeedback(String page){checkPage(page);var result=mapper.pageFeedback(page,UserContext.getCurrentUserId());result.put("helpful",((Number)result.get("helpful")).intValue()>0);return result;}
    @Transactional public void pageFeedback(String page,boolean helpful){checkPage(page);String user=actor();if(helpful)mapper.addPageFeedback(page,user);else mapper.removePageFeedback(page,user);}

    public MeshPage<Map<String,Object>> feed(String kind,String keyword,String tag,String category,String author,String region,Long courseId,String sort,int current,int size) {
        current=Math.max(1,Math.min(current,10000));size=Math.max(1,Math.min(size,50));
        var rows=mapper.feed(kind,keyword,tag,category,author,region,courseId,UserContext.getCurrentUserId(),sort,size,(current-1)*size);
        rows.forEach(this::prepare);
        return new MeshPage<>(rows,mapper.count(kind,keyword,tag,category,author,region,courseId),current,size);
    }
    public Map<String,Object> detail(long id) {
        var row=visible(id);
        if(((Number)row.get("status")).intValue()==1)views.incrementViewCountAsync(id,UserContext.getCurrentUserId());
        prepare(row);
        return row;
    }
    private Map<String,Object> visible(long id) {
        var row=mapper.detail(id,UserContext.getCurrentUserId(),isAdmin());
        if(row==null) throw new BusinessException("404","内容不存在或暂不可见");
        return row;
    }
    private boolean isAdmin(){String id=UserContext.getCurrentUserId(); User u=id==null?null:userMapper.selectById(id);return u!=null && Integer.valueOf(1).equals(u.getUsertype());}
    public String actor(){String user=UserContext.getCurrentUserId();if(user==null)throw new BusinessException("401","请先登录");if(muteService.isMuted(user))throw new BusinessException("403","禁言期间暂不能发布内容");return user;}
    private void prepare(Map<String,Object> row) {
        for(String field:List.of("images","tags","roomTypes","habits")) {
            try {row.put(field,json.readValue(Objects.toString(row.get(field),"[]"),List.class));}
            catch(Exception e){row.put(field,List.of());}
        }
        boolean anonymous=truth(row.get("anonymous"));
        row.put("own",Objects.equals(row.get("authorId"),UserContext.getCurrentUserId()));
        if(anonymous) {row.put("authorName","匿名用户");row.remove("authorAvatar");row.remove("authorId");}
        if(UserContext.getCurrentUserId()==null && "qa".equals(row.get("kind"))) {
            row.put("locked",true);row.put("content","");row.put("images",List.of());row.put("authorName","登录后查看");row.remove("authorAvatar");row.remove("authorId");
        }
    }
    private boolean truth(Object value){return Boolean.TRUE.equals(value) || (value instanceof Number n && n.intValue()!=0);}
    private void validate(MeshContentRequest r) {
        if(r==null||r.getKind()==null||!Set.of("qa","job","life","market","rental").contains(r.getKind()))throw new BusinessException("400","不支持的内容类型");
        if(r.getTitle()==null || r.getTitle().isBlank() || r.getTitle().length()>("qa".equals(r.getKind())?200:100))throw new BusinessException("400","请填写符合长度限制的标题");
        if(r.getContent()!=null && r.getContent().length()>5000)throw new BusinessException("400","正文不能超过 5000 字");
        if(Set.of("job","life","rental").contains(r.getKind())&&(r.getContent()==null||r.getContent().isBlank()))throw new BusinessException("400","请填写正文");
        if(r.getRoomTypes()==null)r.setRoomTypes(List.of());if(r.getHabits()==null)r.setHabits(List.of());
        if(r.getRoomTypes().size()>10||r.getHabits().size()>10||java.util.stream.Stream.concat(r.getRoomTypes().stream(),r.getHabits().stream()).anyMatch(v->v==null||v.isBlank()||v.length()>32))throw new BusinessException("400","房型或生活习惯格式不正确");
        checkLength(r.getRegion(),64);checkLength(r.getCategory(),32);checkLength(r.getCompany(),100);checkLength(r.getRole(),100);checkLength(r.getBrand(),64);checkLength(r.getTradeLocation(),100);checkLength(r.getConditionLabel(),32);checkLength(r.getLeaseTerm(),32);checkLength(r.getGender(),32);
        if(r.getTags()==null)r.setTags(List.of()); if(r.getImages()==null)r.setImages(List.of());
        if(r.getTags().size()>("life".equals(r.getKind())?10:5) || r.getTags().stream().anyMatch(t -> t==null || t.isBlank() || t.length()>30))throw new BusinessException("400","标签数量或长度不符合要求");
        if("qa".equals(r.getKind()) && r.getTags().isEmpty())throw new BusinessException("400","请选择至少一个标签");
        if(r.getImages().size()>("qa".equals(r.getKind())?2:9) || ("life".equals(r.getKind()) && r.getImages().isEmpty()))throw new BusinessException("400","图片数量不符合要求");
        for(String image:r.getImages())if(image==null || !image.matches("(/java/media/[a-f0-9-]+\\.(jpg|png|gif)|https://[^\\s<>\"']+)"))throw new BusinessException("400","图片地址不正确");
        if(r.getRating()!=null && (r.getRating()<1 || r.getRating()>5))throw new BusinessException("400","评分范围为 1 至 5");
        for(String image:r.getImages())if(storage.isManagedUrl(image)&&attachments.owned(image,UserContext.getCurrentUserId())==0)throw new BusinessException("403","请使用自己上传的图片");
        if(r.getCourseId()!=null&&courseMapper.detail(r.getCourseId(),UserContext.getCurrentUserId())==null)throw new BusinessException("404","关联课程不存在");
        if("market".equals(r.getKind())) {
            if(r.getImages().isEmpty())throw new BusinessException("400","请上传商品图片");
            if(!r.isNegotiable() && r.getPrice()==null)throw new BusinessException("400","请填写价格或选择面议");
            money(r.getPrice());r.setAnonymous(false);
        }
        if("rental".equals(r.getKind())) {
            if(!Set.of("listing","roommate","wanted").contains(Objects.toString(r.getRentalType(),"")))throw new BusinessException("400","请选择租房类型");
            if("listing".equals(r.getRentalType()) && (r.getRegion()==null || r.getRegion().isBlank() || r.getImages().isEmpty() || r.getImages().size()>3 || (!r.isNegotiable()&&r.getRent()==null)))throw new BusinessException("400","请填写地区、租金及 1–3 张图片");
            money(r.getRent());money(r.getBudgetMin());money(r.getBudgetMax());
            if(r.getBudgetMin()!=null&&r.getBudgetMax()!=null&&r.getBudgetMin().compareTo(r.getBudgetMax())>0)throw new BusinessException("400","最低预算不能高于最高预算");
            r.setCategory(r.getRentalType());r.setAnonymous(false);
        }
    }
    private PostInsertRequest postRequest(MeshContentRequest r) {
        var post=new PostInsertRequest();post.setTitle(r.getTitle().trim());
        // Use the existing sanitizer, moderation, attachment activation and payment/appeal status.
        post.setContent(Objects.toString(r.getContent(),"")+r.getImages().stream().map(url -> "<img src=\""+url+"\" />").reduce("",String::concat));
        post.setBoardName(Map.of("qa","问答","job","求职","life","生活","market","二手","rental","租房").get(r.getKind()));
        return post;
    }
    @Transactional public Result create(MeshContentRequest r) {
        actor();validate(r);
        Result result=postService.insertPost(postRequest(r));
        if(!Set.of("200","3000").contains(result.getCode())) return result;
        long id=((Number)result.getData()).longValue();
        try {mapper.insertMeta(id,r,json.writeValueAsString(r.getImages()),json.writeValueAsString(r.getTags()));
            if("market".equals(r.getKind()))mapper.market(id,r);
            if("rental".equals(r.getKind()))mapper.rental(id,r,json.writeValueAsString(r.getRoomTypes()),json.writeValueAsString(r.getHabits()));
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
        result.setData(Long.toString(id));return result;
    }
    public Map<String,Object> editable(long id) {
        String user=actor();var row=visible(id);
        if(!user.equals(row.get("authorId")))throw new BusinessException("403","只能编辑自己发布的内容");
        int status=((Number)row.get("status")).intValue();
        if(status!=1&&status!=3)throw new BusinessException("403","已下架的内容不能编辑");
        var body=Jsoup.parseBodyFragment(Objects.toString(row.get("content"),""));
        body.outputSettings().prettyPrint(false);body.select("img").remove();
        row.put("content",body.body().html());prepare(row);return row;
    }
    @Transactional public Result update(long id,MeshContentRequest r) {
        var original=editable(id);
        if(r==null||!Objects.equals(original.get("kind"),r.getKind()))throw new BusinessException("400","编辑不能更改内容板块");
        validate(r);Result result=postService.updatePost(id,postRequest(r));
        if(!Set.of("200","3000").contains(result.getCode()))return result;
        try {
            mapper.updateMeta(id,r,json.writeValueAsString(r.getImages()),json.writeValueAsString(r.getTags()));
            if("market".equals(r.getKind()))mapper.updateMarket(id,r);
            if("rental".equals(r.getKind()))mapper.updateRental(id,r,json.writeValueAsString(r.getRoomTypes()),json.writeValueAsString(r.getHabits()));
        } catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
        result.setData(Long.toString(id));return result;
    }
    public List<Map<String,Object>> answers(long id) {
        visible(id);if(UserContext.getCurrentUserId()==null)return List.of();
        var rows=mapper.answers(id,UserContext.getCurrentUserId());rows.forEach(this::prepare);return rows;
    }
    @Transactional public String answer(long id,String content,boolean anonymous) {
        String user=actor();var question=visible(id);
        if(!"qa".equals(question.get("kind")) || ((Number)question.get("status")).intValue()!=1)throw new BusinessException("400","仅可回答已发布的问题");
        if(content==null || content.isBlank() || content.length()>10000)throw new BusinessException("400","回答需为 1–10000 字");
        String safe=Jsoup.clean(content,Safelist.basic());if(Jsoup.parse(safe).text().isBlank())throw new BusinessException("400","回答不能为空");
        contentSecurityService.checkText(Jsoup.parse(safe).text());
        long answerId=ids.nextId();mapper.answer(answerId,id,user,safe,anonymous);
        if(!user.equals(question.get("authorId")))mapper.notify(ids.nextId(),question.get("authorId").toString(),anonymous?null:user,"answer","你的问题有了新回答",Jsoup.parse(safe).text().substring(0,Math.min(200,Jsoup.parse(safe).text().length())),"/question/"+id);
        return Long.toString(answerId);
    }
    @Transactional public void updateAnswer(long id,String content,boolean anonymous){
        String user=actor();var answer=mapper.lockAnswer(id);
        if(answer==null||((Number)answer.get("status")).intValue()!=1)throw new BusinessException("404","回答不存在");
        if(!user.equals(answer.get("authorId")))throw new BusinessException("403","只能编辑自己的回答");
        var question=visible(((Number)answer.get("questionId")).longValue());
        if(((Number)question.get("status")).intValue()!=1)throw new BusinessException("403","问题暂不可编辑回答");
        if(content==null||content.isBlank()||content.length()>10000)throw new BusinessException("400","回答需为 1–10000 字");
        String safe=Jsoup.clean(content,Safelist.basic());
        if(Jsoup.parse(safe).text().isBlank())throw new BusinessException("400","回答不能为空");
        contentSecurityService.checkText(Jsoup.parse(safe).text());mapper.updateAnswer(id,safe,anonymous);
    }
    @Transactional public void interact(long id,String kind,boolean enabled){String user=actor();var q=visible(id);if(!"qa".equals(q.get("kind")) || !Set.of("same","watch","dislike").contains(kind))throw new BusinessException("400","互动类型无效");if(enabled)mapper.follow(id,user,kind);else mapper.unfollow(id,user,kind);}
    @Transactional public void answerAction(long id,String kind,boolean enabled){
        String user=actor();var answer=mapper.answerById(id);if(answer==null || ((Number)answer.get("status")).intValue()!=1)throw new BusinessException("404","回答不存在");
        long questionId=Long.parseLong(answer.get("questionId").toString());var q=visible(questionId);
        switch(kind){
            case "like" -> {if(enabled)mapper.likeAnswer(id,user);else mapper.unlikeAnswer(id,user);}
            case "delete" -> {if(!user.equals(answer.get("authorId")) && !isAdmin())throw new BusinessException("403","无权删除");mapper.deleteAnswer(id);}
            case "accept" -> {if(!user.equals(q.get("authorId")))throw new BusinessException("403","仅提问者可以采纳回答");mapper.acceptAnswer(questionId,enabled?id:0);}
            default -> throw new BusinessException("400","无效操作");
        }
    }
    public Map<String,Object> home(){return Map.of("announcements",mapper.announcements(),"questionCount",mapper.count("qa",null,null,null,null,null,null),"answerCount",mapper.answerCount());}
    public List<Map<String,Object>> matches(long id){
        var listing=visible(id);if(!"rental".equals(listing.get("kind")))throw new BusinessException("400","仅支持租房匹配");prepare(listing);
        if(!"active".equals(listing.get("listingStatus")))return List.of();
        boolean sourceIsListing="listing".equals(listing.get("rentalType"));
        List<Map<String,Object>> candidates=new ArrayList<>();
        for(String targetType:sourceIsListing?List.of("roommate","wanted"):List.of("listing"))
            candidates.addAll(mapper.feed("rental","","",targetType,"","",null,UserContext.getCurrentUserId(),"latest",50,0));
        List<Map<String,Object>> result=new ArrayList<>();
        for(var candidate:candidates){if(candidate.get("id").equals(listing.get("id"))||Objects.equals(candidate.get("authorId"),listing.get("authorId"))||!"active".equals(candidate.get("listingStatus")))continue;prepare(candidate);int possible=0,score=0;List<String> reasons=new ArrayList<>();
            String region=Objects.toString(listing.get("region"),"");if(!region.isBlank()&&!Objects.toString(candidate.get("region"),"").isBlank()){possible+=30;if(region.equals(candidate.get("region"))){score+=30;reasons.add("意向地区一致");}}
            var home=sourceIsListing?listing:candidate;var seeker=sourceIsListing?candidate:listing;
            if(home.get("rent") instanceof Number rent&&(seeker.get("budgetMin") instanceof Number||seeker.get("budgetMax") instanceof Number)){possible+=40;double min=seeker.get("budgetMin") instanceof Number n?n.doubleValue():0,max=seeker.get("budgetMax") instanceof Number n?n.doubleValue():Double.MAX_VALUE;if(rent.doubleValue()>=min&&rent.doubleValue()<=max){score+=40;reasons.add("月租符合预算");}}
            List<?> habits=(List<?>)listing.get("habits"),other=(List<?>)candidate.get("habits");if(!habits.isEmpty()&&!other.isEmpty()){possible+=30;long common=habits.stream().filter(other::contains).count();score+=(int)(30*common/Math.max(habits.size(),other.size()));if(common>0)reasons.add("有 "+common+" 项共同生活习惯");}
            candidate.put("matchScore",possible==0?null:Math.round(score*100f/possible));candidate.put("matchReasons",reasons);result.add(candidate);
        }
        result.sort(Comparator.comparingInt((Map<String,Object> row)->row.get("matchScore") instanceof Number n?n.intValue():-1).reversed());return result.stream().limit(6).toList();
    }
    private void checkLength(String value,int max){if(value!=null&&value.length()>max)throw new BusinessException("400","字段长度超过限制 "+max);}
    private void money(java.math.BigDecimal amount){if(amount!=null&&(amount.signum()<0||amount.compareTo(new java.math.BigDecimal("99999999.99"))>0||amount.scale()>2))throw new BusinessException("400","金额不符合要求");}
    @Transactional public void status(long id,String status){String user=actor();var item=visible(id);if(!user.equals(item.get("authorId"))&&!isAdmin())throw new BusinessException("403","仅发布者可以修改状态");String kind=item.get("kind").toString();if(kind.equals("market")&&Set.of("selling","reserved","sold").contains(status))mapper.marketStatus(id,status);else if(kind.equals("rental")&&Set.of("active","closed").contains(status))mapper.rentalStatus(id,status);else throw new BusinessException("400","无效状态");}
    @Transactional public List<Result> batch(List<MeshContentRequest> requests,String requestKey){String user=actor();String key=requestKey==null?UUID.randomUUID().toString():requestKey;if(!key.matches("[a-zA-Z0-9-]{8,64}"))throw new BusinessException("400","无效批次标识");String hash;try{hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(requests)));}catch(Exception e){throw new IllegalStateException(e);}mapper.beginBatch(user,key,hash);var batch=mapper.lockBatch(user,key);if(!hash.equals(batch.get("hash")))throw new BusinessException("409","该批次已用于其他内容");if(batch.get("result")!=null){try{return json.readValue(batch.get("result").toString(),new com.fasterxml.jackson.core.type.TypeReference<List<Result>>(){});}catch(Exception e){throw new IllegalStateException(e);}}if(requests==null||requests.isEmpty()||requests.size()>20)throw new BusinessException("400","每批可发布 1–20 件商品");List<Result> results=new ArrayList<>();for(var r:requests){r.setKind("market");var result=create(r);if(!Set.of("200","3000").contains(result.getCode()))throw new BusinessException(result.getCode(),result.getMsg());results.add(result);}try{mapper.finishBatch(user,key,json.writeValueAsString(results));}catch(Exception e){throw new IllegalStateException(e);}return results;}
}
