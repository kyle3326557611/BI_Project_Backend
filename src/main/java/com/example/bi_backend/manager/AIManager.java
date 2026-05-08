package com.example.bi_backend.manager;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.bi_backend.common.ErrorCode;
import com.example.bi_backend.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIManager {

    @Value("${kyle-ai.api-key}")
    private String apiKey;

    @Value("${kyle-ai.url}")
    private String url;

    @Value("${kyle-ai.model}")
    private String model;


    public String doChat(String AIPrompt, String message) {

        JSONObject param = getParam(AIPrompt, message);

        // 重定向3次
        int maxRetry = 3;
        for (int i = 1; i <= maxRetry; i++) {
            try {
                System.out.println(" 第 " + i + " 次向AI发起问询...");
                String responseStr = HttpRequest.post(url)  //POST请求
                        .header("Authorization", "Bearer " + apiKey) //将Key放入表头
                        .header("Content-Type", "application/json")  //设置JSON格式
                        .body(param.toString())  //放入参数
                        .timeout(30000)  //30s后超时
                        .execute()
                        .body();  //将回复放回Body（String）

                JSONObject jsonObject = JSONUtil.parseObj(responseStr);  //转换成JSON格式

                if (jsonObject.containsKey("error")) {
                    String errorMsg = jsonObject.getJSONObject("error").getStr("message");
                    // 如果是拥挤限流，主动抛出异常让外层捕获并重试
                    if (errorMsg.contains("访问量过大")) {
                        throw new RuntimeException("服务器拥挤：" + errorMsg);
                    }
                    // 如果是 API 错误（如欠费），直接死刑
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 致命报错: " + errorMsg);
                }

                // 直接返回（获取对应内容）（剥洋葱）
                return jsonObject.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");

            } catch (Exception e) {
                // 如果是最后一次也失败了，抛出异常
                if (i == maxRetry) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 网络极度拥挤，重试 3 次均失败：" + e.getMessage());
                }
                // 如果不是最后一次，睡 2 秒钟再试
                System.err.println("失败了，2s 后重试...原因：" + e.getMessage());
                cn.hutool.core.thread.ThreadUtil.sleep(3000);

            }

        }

        return null;
    }

    /**
     *
     * AI模型调教
     * @param AIPrompt 系统级提示词
     * @param message 用户级提示词
     * @return 调教后的AI模型
     */
    private JSONObject getParam(String AIPrompt, String message) {
        JSONObject param = new JSONObject();
        param.set("model", model); // 设置模型

        JSONArray messages = new JSONArray();

        if (AIPrompt != null && !AIPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", AIPrompt);
            messages.add(systemMsg);
        }
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");  //身份
        userMsg.set("content", message);  //内容

        messages.add(userMsg);

        param.set("messages", messages);
        param.set("stream", false); //将内容全部生成完成再发送给我
        return param;
    }

    /**
     * AI 调用接口
     */
    public String doBiChat(String goal, String chartType, String csvData) {

        // 1. 构造 System Prompt (规定人设和输出格式)
        final String systemPrompt = "你是一个首席数据分析师和前端开发专家。接下来我会给你一份数据，请你帮我生成 Echarts V5 的 option 配置对象 JSON 代码，并给出数据分析结论。\n" +
                "【极其重要，必须严格遵守，否则系统会崩溃】：\n" +
                "1. 你必须且只能按照以下规定格式输出，不允许输出任何额外的开头打招呼或解释性文字！\n" +
                "2. 必须用【【【【【作为两部分内容的分隔符！\n" +
                "3. 生成的 Echarts JSON 代码中，JSON 的键名（Key）必须极其标准，绝对不能带有前导空格！例如必须是 \"xAxis\" 和 \"yAxis\"，绝对不能写成 \" xAxis\"！\n" +
                "\n" +
                "【【【【【\n" +
                "{\n" +
                "  // 纯粹的 Echarts option JSON 代码，不要带 markdown 代码块标记\n" +
                "}\n" +
                "【【【【【\n" +
                "这里写基于数据的详细分析结论";


        // 2. 构造 User Prompt (干活材料：拼接目标、强制类型、CSV数据)
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        if (StringUtils.isNotBlank(chartType)) {
            goal += "。\n【极其重要】：请你务必、严格使用【" + chartType + "】来生成 Echarts 代码！JSON 配置中的 series.type 必须与之一致，绝对不允许使用其他图表类型！！！";
        }
        userInput.append(goal).append("\n");

        userInput.append("原始数据:").append(csvData).append("\n");

        // 3. 调用doChat，把 System 和 User 严格分开传给AI！
        return this.doChat(systemPrompt, userInput.toString());
    }

}