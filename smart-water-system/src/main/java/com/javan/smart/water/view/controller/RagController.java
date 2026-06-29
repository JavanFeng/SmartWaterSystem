package com.javan.smart.water.view.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author FengJ
 * @description init
 */
@RestController
@RequestMapping("api/rag/")
@Validated
public class RagController {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 初始化数据
     *
     * @return
     */
    @PostMapping("/init-data")
    public String initVectorData() {
        List<Document> documents = List.of(
                new Document("1. 生活饮用水卫生标准（GB5749-2022）规定，自来水的pH值应保持在6.5至8.5之间，低于或高于此范围可能意味着水体受到酸性或碱性污染。"
                        , Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("2. 溶解氧（DO）是评价水质自净能力的重要指标。一般要求地表水溶解氧不低于5mg/L；若低于2mg/L，说明水体已严重缺氧，可能导致水生生物死亡并产生黑臭现象。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("3. 化学需氧量（COD）反映了水中受还原性物质污染的程度。工业废水排放前必须经过生化处理，通常要求COD降至50mg/L以下才能排入市政管网。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("4. 总磷（TP）和氨氮（NH3-N）是导致水体富营养化的主要元凶。当湖泊或水库中的总磷浓度超过0.02mg/L时，极易引发蓝藻水华爆发。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("5. 浊度是衡量水中悬浮颗粒物含量的指标。自来水出厂时的浊度标准要求小于1 NTU，如果用户家中自来水突然变浑浊，可能是由于管道老化破裂或二次供水设施未定期清洗。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("6. 重金属超标是地下水污染的常见类型。铅、镉、铬等重金属无法通过自然降解消除，长期饮用会导致慢性中毒。发现重金属超标应立即切断水源并启动活性炭吸附或反渗透膜过滤工艺。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("7. 余氯的作用是抑制管网中细菌的滋生。国家标准规定管网末梢水中的游离余氯不得低于0.05mg/L。如果自来水有明显的刺鼻漂白粉味，说明余氯偏高，建议将水煮沸后饮用以挥发余氯。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("8. 针对突发性水质发黄事件，应急处理预案包括：立即关闭进水阀门，对受影响管段进行排水冲洗，直至出水浊度和色度恢复正常后方可恢复供水。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("9. 污水处理厂常用的A/O（厌氧/好氧）工艺，主要是利用微生物的新陈代谢作用来去除污水中的有机物和脱氮除磷。运行过程中需严格控制污泥沉降比（SV30）在15%-30%之间。", Map.of("category", "国家标准", "source", "GB5749-2022")),
                new Document("10. 水质在线监测仪需要定期进行校准和维护。pH探头通常每两周需要用标准缓冲液校准一次，溶解氧电极则需要每月更换一次电解液和透氧膜，以确保数据准确。", Map.of("category", "国家标准", "source", "GB5749-2022", "alc", "admin"))
        );
        vectorStore.add(documents);
        return "知识库初始化成功！";
    }
}
