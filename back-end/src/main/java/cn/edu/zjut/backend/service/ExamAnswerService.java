package cn.edu.zjut.backend.service;

import cn.edu.zjut.backend.dao.*;
import cn.edu.zjut.backend.dto.AddExamAnswerDTO;
import cn.edu.zjut.backend.po.*;
import cn.edu.zjut.backend.util.ChatGLM_N;
import cn.edu.zjut.backend.util.ExamContext;
import cn.edu.zjut.backend.util.HibernateUtil;
import cn.edu.zjut.backend.util.Jwt;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Claims;
import org.apache.poi.ss.formula.atp.Switch;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("examAnswerServ")
public class ExamAnswerService {
    public Session getSession() {

        return HibernateUtil.getSession();
    }

    public boolean addExamAnswer(AddExamAnswerDTO addExamAnswerDTO) throws Exception {

        Map<Long, Object> answers = addExamAnswerDTO.getAnswers();
        Long examId = addExamAnswerDTO.getExamId();

        Session session = this.getSession();
        ExamAnswerDAO examAnswerDAO = new ExamAnswerDAO();
        ExamRecordDAO examRecordDAO = new ExamRecordDAO();
        ExamSettingDAO examSettingDAO = new ExamSettingDAO();
        ExamDAO examDAO = new ExamDAO();
        StudentCourseDAO studentCourseDAO = new StudentCourseDAO();
        StudentGradeDAO studentGradeDAO = new StudentGradeDAO();
        examDAO.setSession(session);
        examAnswerDAO.setSession(session);
        studentCourseDAO.setSession(session);
        studentGradeDAO.setSession(session);

        Transaction tran = null;

        try {
            tran = session.beginTransaction();

            // 是否超时提交
            Exam exam = examDAO.getById(examId);
            ExamSetting examSetting = examSettingDAO.getByExamId(session, examId);
            Date now = new Date();  // 获取当前时间
            Date deadline = new Date(exam.getEndTime().getTime() + 3000);   // 考试结束时间多加3秒
            if(now.after(deadline)) throw new Exception("不在可提交时间范围内");

            List<ExamRecord> examRecords = examRecordDAO.getRecordsByExamId(session, examId);   // 获取对应的记录
//            ExamRecord examRecord = null;
            ExamRecord examRecord = new ExamRecord();
            for (ExamRecord e : examRecords) {
                if(e.getStudentId().equals(ExamContext.getStudentId())){

                    if(e.getStatus().equals("1")){  // 已经提交了（系统强制提交后，用户不可再次提交）
                        return false;
                    }

                    examRecord = e;
                    break;
                }
            }

            examRecord.setObjectiveScore(BigDecimal.ZERO);  // 设置分数
            examRecord.setSubjectiveScore(BigDecimal.ZERO);
            examRecord.setTotalScore(BigDecimal.ZERO);
            examRecord.setStatus("1");  // 考试记录状态，设置为已交
            examRecord.setSubmitTime(now);  // 设置提交时间

            for (Map.Entry<Long, Object> entry : answers.entrySet()) {
                Long questionId = entry.getKey();   // 题目ID
                Object value = entry.getValue();    // 对应的答案

                Gson gson = new Gson();

                ExamAnswer examAnswer = new ExamAnswer();
                examAnswer.setRecordId(examRecord.getRecordId());
                examAnswer.setQuestionId(questionId);
                examAnswer.setMyAnswer(gson.toJson(value));
                examAnswer.setMarkType("1");
                examAnswer.setMarkerId(exam.getTeacherId());    // 发布考试的老师ID

                correctAnswers(examAnswer, examRecord , examSetting , value, exam.getPaperId(), questionId);

                examAnswerDAO.save(examAnswer);
                examRecordDAO.updateExamRecord(session, examRecord);
            }

            // 插入成绩（适配数据库）
            StudentGrade studentGrade = new StudentGrade();
            studentGrade.setStudentId(ExamContext.getStudentId());
            studentGrade.setCourseId(1L);
            studentGrade.setSubjectId(1);
            studentGrade.setGradeType("ONLINE_EXAM");
            studentGrade.setSourceId(examId);
            studentGrade.setGradeName(exam.getTitle());
            studentGrade.setScore(examRecord.getTotalScore());
            studentGrade.setTeacherId(exam.getTeacherId());
            studentGrade.setRecordTime(new Date());
            studentGrade.setFullScore(BigDecimal.valueOf(100));
            studentGradeDAO.add(studentGrade);

            tran.commit();
            return true;
        } catch (Exception e) {
            if (tran != null) {
                tran.rollback();
            }
            throw e;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public void correctAnswers(ExamAnswer examAnswer, ExamRecord examRecord, ExamSetting examSetting, Object value, Long paperId, Long questionId) throws Exception {

        Gson gson = new Gson();

        Session session = this.getSession();
        QuestionDAO questionDAO = new QuestionDAO();
        ExamPaperQuestionDAO examPaperQuestionDAO = new ExamPaperQuestionDAO();
        ExamWrongBookDAO examWrongBookDAO = new ExamWrongBookDAO();
        questionDAO.setSession(session);
        examPaperQuestionDAO.setSession(session);
        examWrongBookDAO.setSession(session);

        try{
            Questions questions = questionDAO.query(questionId);    // 题目对象
            Set<QuestionComponents> components = questions.getQuestionComponents(); // 题目组件
            Set<QuestionItems> items = questions.getQuestionItems();

            ExamPaperQuestionId examPaperQuestionId = new ExamPaperQuestionId();
            examPaperQuestionId.setPaperId(paperId);
            examPaperQuestionId.setQuestionId(questionId);
            ExamPaperQuestion examPaperQuestion = examPaperQuestionDAO.query(examPaperQuestionId);  // 试卷题目对象

            System.out.println(value);

            switch(questions.getTypeId()){
                case 1: {   // 单选题
                    examAnswer.setMarkType("1");    // 机评
                    for(QuestionComponents component: components){
                        if(component.getComponentType().equals("answer")){
                            Map content = gson.fromJson(component.getContent(), Map.class);
                            if(content.get("correctOption").equals(value)){
                                examAnswer.setScore(examPaperQuestion.getScore());

                                examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore())); // 客观题加上对应的分
                                examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore())); // 总分加上对应的分数
                            }else{
                                examAnswer.setScore(BigDecimal.valueOf(0)); // 得分为0
                            }
                            break;
                        }
                    }
                    break;
                }

                case 2: {   // 多选
                    examAnswer.setMarkType("1");
                    for(QuestionComponents component: components){
                        if(component.getComponentType().equals("answer")){
                            String content = gson.fromJson(component.getContent(), Map.class).get("correctOption").toString();

                            Set<String> ans = Arrays.stream(content.split(",")) // 1. 分割成流
                                    .map(String::trim)       // 2. (可选) 去除每个元素前后的空格
                                    .collect(Collectors.toSet()); // 3. 收集为 Set

                            Type listType = new TypeToken<List<String>>(){}.getType();
                            List<String> valList = gson.fromJson(gson.toJson(value), listType);
                            Set<String> userSet = new HashSet<>(valList);

                            if(userSet.equals(ans)){    // 正确
                                examAnswer.setScore(examPaperQuestion.getScore());

                                examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore())); // 客观题加上对应的分
                                examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore())); // 总分加上对应的分数

                            }else if(ans.containsAll(userSet)){ // 部分正确
                                examAnswer.setScore(examPaperQuestion.getScore().multiply(examSetting.getMultiChoicePartialRatio()));   // 乘以多选题的比例

                                examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore().multiply(examSetting.getMultiChoicePartialRatio()))); // 客观题加上对应的分
                                examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore().multiply(examSetting.getMultiChoicePartialRatio()))); // 总分加上对应的分数
                            }else{  // 错误
                                examAnswer.setScore(BigDecimal.valueOf(0));   // 乘以多选题的比例
                            }
                        }
                    }
                    break;
                }

                case 3: {   // 判断题
                    examAnswer.setMarkType("1");
                    for(QuestionComponents component: components){
                        if(component.getComponentType().equals("answer")){
                            String content = gson.fromJson(component.getContent(), Map.class).get("correctResult").toString();
                            if(content.equals(value.toString())){
                                examAnswer.setScore(examPaperQuestion.getScore());

                                examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore())); // 客观题加上对应的分
                                examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore())); // 总分加上对应的分数

                            }else{
                                examAnswer.setScore(BigDecimal.valueOf(0));
                            }
                        }
                    }
                    break;
                }

                case 4: {   // 填空题
                    examAnswer.setMarkType("1");    // 机评
                    examAnswer.setScore(BigDecimal.valueOf(0));
                    for(QuestionComponents component: components){
                        if(component.getComponentType().equals("answer")){
                            // 正确答案
                            String jsonString = gson.fromJson(component.getContent(), Map.class).get("answers").toString();
                            Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
                            List<Map<String, String>> blankList = gson.fromJson(jsonString, listType);

                            // 学生答案
                            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
                            Map<String, String> valueMap = gson.fromJson(value.toString(), mapType);

                            int totalBlank = blankList.size();
                            for(int i=0; i<totalBlank; i++){

//                            String inputRes = examSetting.getFillIgnoreSymbols().toString().equals("1") ? valueMap.get(String.valueOf(i+1)).replaceAll("[\\p{P}\\p{S}]", "") : valueMap.get(String.valueOf(i+1));
                                String inputRes = valueMap.get(String.valueOf(i+1));

                                // 不区分大小写
                                if(examSetting.getFillCaseSensitive().toString().equals("0")){
                                    // 正确了累加分数
                                    if(inputRes.equalsIgnoreCase(blankList.get(i).get("correctAnswer"))){
                                        // 每一空均分
                                        examAnswer.setScore(examAnswer.getScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros()));

                                        examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros())); // 客观题加上对应的分
                                        examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros())); // 总分加上对应的分数
                                    }
                                }else{
                                    if(inputRes.equals(blankList.get(i).get("correctAnswer"))){
                                        // 每一空均分
                                        examAnswer.setScore(examAnswer.getScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros()));

                                        examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros())); // 客观题加上对应的分
                                        examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore().divide(BigDecimal.valueOf(totalBlank),10, RoundingMode.HALF_UP).stripTrailingZeros())); // 总分加上对应的分数
                                    }
                                }
                            }
                            // 最终的答案四舍五入
                            examRecord.setObjectiveScore(examRecord.getObjectiveScore().setScale(0, RoundingMode.HALF_UP));
                            examRecord.setTotalScore(examRecord.getTotalScore().setScale(0, RoundingMode.HALF_UP));
                        }
                    }
                    break;
                }

                case 6: // 论述题
                case 5: {   // 名词解析
                    String isManual = examSetting.getFillManualMark().toString();   // 是否人工批改
                    if(isManual.equals("0")){   // 自动修改
                        examAnswer.setMarkType("1");
                        for(QuestionComponents component: components){
                            if(component.getComponentType().equals("answer")){
                                String referenceAnswer = gson.fromJson(component.getContent(), Map.class).get("completeAnswer").toString();
                                String prompt = buildGradingPrompt(questions.getTitle(), referenceAnswer, value.toString(), examPaperQuestion.getScore());
//                                System.out.println(prompt);

                                BigDecimal aiScore = getAIScoreWithRetry(prompt, 2);

                                if (aiScore == null) {  // 如果没有办法生成，就转人工批改
                                    // 降级策略：给 0 分，或标记为需人工复核
                                    examAnswer.setMarkType("2"); // 改为人工评阅（可选）
                                }else{
                                    aiScore = aiScore.min(examPaperQuestion.getScore()).max(BigDecimal.ZERO);
                                    examAnswer.setScore(aiScore);
                                    examRecord.setSubjectiveScore(examRecord.getSubjectiveScore().add(aiScore));
                                    examRecord.setTotalScore(examRecord.getTotalScore().add(aiScore));
                                }
                            }
                        }
                    }else{
                        examAnswer.setMarkType("2");    // 设置为人工批改（师评）
                    }
                    break;
                }

                case 7: {   // 计算题
                    examAnswer.setMarkType("1");
                    for(QuestionComponents component: components){
                        if(component.getComponentType().equals("answer")){
                            String content = gson.fromJson(component.getContent(), Map.class).get("finalAnswer").toString();
                            if(content.equals(value.toString())){
                                examAnswer.setScore(examPaperQuestion.getScore());

                                examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(examPaperQuestion.getScore())); // 客观题加上对应的分
                                examRecord.setTotalScore(examRecord.getTotalScore().add(examPaperQuestion.getScore())); // 总分加上对应的分
                            }else{
                                examAnswer.setScore(BigDecimal.valueOf(0));
                            }
                        }
                    }
                    break;
                }

                case 8: {   // 分录题
                    examAnswer.setMarkType("1"); // 机评

                    List<Map<String, Object>> studentEntries = null;
                    try {
                        if (value != null) {
                            String jsonValue = value.toString().trim();
                            JsonObject root = JsonParser.parseString(jsonValue).getAsJsonObject();
                            JsonArray entriesArray = root.getAsJsonArray("entries");
                            studentEntries = gson.fromJson(entriesArray, new TypeToken<List<Map<String, Object>>>() {}.getType());
                        } else {
                            studentEntries = new ArrayList<>();
                        }
                    } catch (Exception e) {
                        System.err.println("解析学生分录失败：" + e.getMessage());
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    // 获取标准答案 entryList
                    List<Map<String, Object>> standardEntries = new ArrayList<>();
                    for (QuestionComponents component : components) {
                        if ("answer".equals(component.getComponentType())) {
                            Map answerMap = gson.fromJson(component.getContent(), Map.class);
                            if (answerMap.get("entryList") instanceof List) {
                                standardEntries = (List<Map<String, Object>>) answerMap.get("entryList");
                            }
                            break;
                        }
                    }

                    if (standardEntries.isEmpty()) {
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    int totalStandardLines = standardEntries.size();
                    BigDecimal fullScore = examPaperQuestion.getScore();
                    BigDecimal scorePerLine = fullScore.divide(BigDecimal.valueOf(totalStandardLines), 2, RoundingMode.HALF_UP);

                    // 如果学生未作答
                    if (studentEntries == null || studentEntries.isEmpty()) {
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    // 标准化学生答案（去重，避免重复计分）
                    List<String> normalizedStudentList = new ArrayList<>();
                    for (Map<String, Object> entry : studentEntries) {
                        try {
                            String norm = normalizeSingleEntry(entry);
                            if (norm != null) {
                                normalizedStudentList.add(norm);
                            }
                        } catch (Exception ex) {
                            // 忽略格式错误的条目
                        }
                    }

                    // 去重：防止学生重复写同一条分录刷分
                    Set<String> studentSet = new LinkedHashSet<>(normalizedStudentList);

                    // 统计答对条数
                    int correctCount = 0;
                    for (Map<String, Object> stdEntry : standardEntries) {
                        try {
                            String stdNorm = normalizeSingleEntry(stdEntry);
                            if (stdNorm != null && studentSet.contains(stdNorm)) {
                                correctCount++;
                                // 从集合中移除已匹配项，防止多对一（可选，但更严谨）
                                studentSet.remove(stdNorm);
                            }
                        } catch (Exception ex) {
                            // 标准答案异常？跳过
                        }
                    }

                    // 计算得分
                    BigDecimal finalScore = scorePerLine.multiply(BigDecimal.valueOf(correctCount))
                            .setScale(2, RoundingMode.HALF_UP)
                            .min(fullScore) // 不超过满分
                            .max(BigDecimal.ZERO);

                    examAnswer.setScore(finalScore);
                    examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(finalScore).setScale(0, RoundingMode.HALF_UP));
                    examRecord.setTotalScore(examRecord.getTotalScore().add(finalScore).setScale(0, RoundingMode.HALF_UP));
                    break;
                }

                case 9: {   // 连线题
                    examAnswer.setMarkType("1"); // 机评

                    // === 1. 解析学生答案 ===
                    Map<String, String> studentAnswerMap = new HashMap<>();
                    try {
                        if (value != null) {
                            String jsonValue = value.toString().trim();
                            // 兼容 JSON 对象字符串，如 {"L1":"R1", "L2":"R2"}
                            studentAnswerMap = gson.fromJson(jsonValue, new TypeToken<Map<String, String>>() {}.getType());
                        }
                    } catch (Exception e) {
                        System.err.println("解析学生连线答案失败：" + e.getMessage());
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    // === 2. 获取标准答案 matchPairs ===
                    List<Map<String, String>> standardPairs = new ArrayList<>();
                    for (QuestionComponents component : components) {
                        if ("answer".equals(component.getComponentType())) {
                            Map answerMap = gson.fromJson(component.getContent(), Map.class);
                            if (answerMap.get("matchPairs") instanceof List) {
                                standardPairs = (List<Map<String, String>>) answerMap.get("matchPairs");
                            }
                            break;
                        }
                    }

                    if (standardPairs.isEmpty()) {
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    int totalPairs = standardPairs.size();
                    BigDecimal fullScore = examPaperQuestion.getScore();
                    BigDecimal scorePerPair = fullScore.divide(BigDecimal.valueOf(totalPairs), 2, RoundingMode.HALF_UP);

                    // 构建标准答案映射：leftId -> rightId
                    Map<String, String> standardMap = new HashMap<>();
                    for (Map<String, String> pair : standardPairs) {
                        String leftId = pair.get("leftId");
                        String rightId = pair.get("rightId");
                        if (leftId != null && rightId != null) {
                            standardMap.put(leftId.trim(), rightId.trim());
                        }
                    }

                    // === 3. 统计答对数量 ===
                    int correctCount = 0;
                    for (Map.Entry<String, String> entry : standardMap.entrySet()) {
                        String leftId = entry.getKey();
                        String expectedRightId = entry.getValue();
                        String studentRightId = studentAnswerMap.get(leftId);

                        if (studentRightId != null && studentRightId.trim().equals(expectedRightId)) {
                            correctCount++;
                        }
                    }

                    // === 4. 计算得分 ===
                    BigDecimal finalScore = scorePerPair.multiply(BigDecimal.valueOf(correctCount))
                            .setScale(2, RoundingMode.HALF_UP)
                            .min(fullScore)
                            .max(BigDecimal.ZERO);

                    examAnswer.setScore(finalScore);
                    examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(finalScore).setScale(0, RoundingMode.HALF_UP));
                    examRecord.setTotalScore(examRecord.getTotalScore().add(finalScore).setScale(0, RoundingMode.HALF_UP));
                    break;
                }

                case 10: { // 排序题
                    examAnswer.setMarkType("1");
                    List<String> correctOrder = null;
                    List<String> studentOrder = null;

                    // 1. 解析正确答案
                    for (QuestionComponents component : components) {
                        if ("answer".equals(component.getComponentType())) {
                            try {
                                Map<String, Object> answerMap = gson.fromJson(component.getContent(), Map.class);
                                String correctOrderJson = gson.toJson(answerMap.get("correctOrder"));
                                Type listType = new TypeToken<List<String>>() {}.getType();
                                correctOrder = gson.fromJson(correctOrderJson, listType);
                            } catch (Exception e) {
                                System.err.println("解析排序题正确答案失败：" + e.getMessage());
                                examAnswer.setScore(BigDecimal.ZERO);
                                break;
                            }
                        }
                    }

                    // 2. 解析学生答案（value 应为 JSON 数组字符串，如 ["S2","S1",...])
                    try {
                        if (value != null) {
                            String studentJson = value.toString().trim();
                            if (studentJson.startsWith("[") && studentJson.endsWith("]")) {
                                Type listType = new TypeToken<List<String>>() {}.getType();
                                studentOrder = gson.fromJson(studentJson, listType);
                            } else {
                                throw new IllegalArgumentException("学生答案不是有效JSON数组");
                            }
                        } else {
                            studentOrder = new ArrayList<>();
                        }
                    } catch (Exception e) {
                        System.err.println("解析学生排序答案失败：" + e.getMessage());
                        examAnswer.setScore(BigDecimal.ZERO);
                        studentOrder = new ArrayList<>();
                    }

                    // 3. 校验并评分
                    if (correctOrder == null || correctOrder.isEmpty()) {
                        examAnswer.setScore(BigDecimal.ZERO);
                    } else {
                        // 如果学生答案长度不匹配，按实际交集位置判断（或视为全错）
                        int totalItems = correctOrder.size();
                        int correctCount = 0;

                        // 只比较相同索引位置是否一致，且长度至少覆盖该位置
                        for (int i = 0; i < totalItems; i++) {
                            if (i < studentOrder.size() &&
                                    correctOrder.get(i) != null &&
                                    correctOrder.get(i).equals(studentOrder.get(i))) {
                                correctCount++;
                            }
                        }

                        // 计算得分：(正确数 / 总数) * 题目总分
                        BigDecimal score = BigDecimal.ZERO;
                        if (totalItems > 0) {
                            // 使用高精度除法，保留足够小数位
                            score = examPaperQuestion.getScore()
                                    .multiply(new BigDecimal(correctCount))
                                    .divide(new BigDecimal(totalItems), 10, RoundingMode.HALF_UP);
                        }

                        examAnswer.setScore(score);

                        // 累加到记录（保持高精度，最终可统一四舍五入）
                        examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(score).setScale(0, RoundingMode.HALF_UP));
                        examRecord.setTotalScore(examRecord.getTotalScore().add(score).setScale(0, RoundingMode.HALF_UP));
                    }
                    break;
                }

                case 11: { // 完形填空题：按 questionItems 逐个子题评分
                    examAnswer.setMarkType("1");

                    if (items == null || items.isEmpty()) {
                        examAnswer.setScore(BigDecimal.ZERO);
                        break;
                    }

                    BigDecimal fullScore = examPaperQuestion.getScore();
                    int totalBlanks = items.size();
                    BigDecimal perBlankScore = fullScore.divide(new BigDecimal(totalBlanks), 10, RoundingMode.HALF_UP);
                    BigDecimal totalEarned = BigDecimal.ZERO;

                    // 解析学生答案：假设 value 是 JSON 字符串，如 {"1":"B","2":"A","3":"C"}
                    Map<String, String> studentAnswerMap = new HashMap<>();
                    try {
                        if (value != null && !value.toString().trim().isEmpty()) {
                            String jsonStr = value.toString().trim();
                            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                                studentAnswerMap = gson.fromJson(jsonStr, mapType);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析学生完形填空答案失败：" + e.getMessage());
                        // 即使解析失败，也继续评分（视为全错）
                    }

                    // 遍历每个子题（每个空）
                    for (QuestionItems item : items) {
                        Integer sequence = item.getSequence(); // 如 1, 2, 3
                        String correctKey = null;

                        // 从该子题的 components 中找 answer
                        for (QuestionComponents component : item.getQuestionComponents()) {
                            if ("answer".equals(component.getComponentType())) {
                                try {
                                    Map<String, Object> answerMap = gson.fromJson(component.getContent(), Map.class);
                                    correctKey = answerMap.get("correctKey").toString();
                                } catch (Exception e) {
                                    System.err.println("解析子题 " + sequence + " 的正确答案失败：" + e.getMessage());
                                }
                                break; // 每个子题只有一个 answer
                            }
                        }

                        // 获取学生对该空的答案
                        String studentKey = studentAnswerMap.get(String.valueOf(sequence));
                        if (studentKey != null && correctKey != null && studentKey.equals(correctKey)) {
                            totalEarned = totalEarned.add(perBlankScore);
                        }
                    }

                    examAnswer.setScore(totalEarned);
                    examRecord.setObjectiveScore(examRecord.getObjectiveScore().add(totalEarned).setScale(0, RoundingMode.HALF_UP));
                    examRecord.setTotalScore(examRecord.getTotalScore().add(totalEarned).setScale(0, RoundingMode.HALF_UP));

                    break;
                }
            }

            System.out.println(examAnswer);
            System.out.println(examRecord);


            // 如果没有拿满分，则记录错题
            if(examAnswer.getScore().compareTo(examPaperQuestion.getScore()) < 0){
                List<ExamWrongBook> examWrongBooks = examWrongBookDAO.queryByStudentId(ExamContext.getStudentId());
                boolean isExist = false;
                for (ExamWrongBook examWrongBook : examWrongBooks) {
                    if(examWrongBook.getQuestionId().equals(questionId)){
                        isExist = true;
                        examWrongBook.setErrorCount(examWrongBook.getErrorCount() + 1);
                        examWrongBook.setLastErrorTime(new Date());
                        examWrongBookDAO.update(examWrongBook);
                        break;
                    }
                }
                if(!isExist){
                    ExamWrongBook examWrongBook = new ExamWrongBook();
                    examWrongBook.setStudentId(ExamContext.getStudentId());
                    examWrongBook.setQuestionId(questionId);
                    examWrongBook.setErrorCount(1);
                    examWrongBook.setCreateTime(new Date());
                    examWrongBook.setLastErrorTime(new Date());
                    examWrongBookDAO.save(examWrongBook);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    public static String buildGradingPrompt(String title, String referenceAnswer, String studentAnswer, BigDecimal maxScore) {
        // 安全处理 null 或空值
        String ref = (referenceAnswer != null) ? referenceAnswer.trim() : "";
        String stu = (studentAnswer != null) ? studentAnswer.trim() : "";
        String t = (title != null) ? title.trim() : "题目";
        String score = (maxScore != null) ? maxScore.stripTrailingZeros().toPlainString() : "0";

        return String.format(
                "你是一名严谨的考试阅卷老师，请根据以下信息对学生的主观题答案进行评分。\n\n" +
                        "【题目】\n%s\n\n" +
                        "【参考答案】\n%s\n\n" +
                        "【学生答案】\n%s\n\n" +
                        "【评分规则】\n" +
                        "1. 本题满分为 %s 分。\n" +
                        "2. 请根据学生答案是否覆盖参考答案中的核心要点、逻辑是否清晰、表述是否准确进行评分。\n" +
                        "3. 如果学生答案完全未作答或与题目无关，得 0 分。\n" +
                        "4. 不必严格要求字词一致，但关键术语和概念必须正确。\n" +
                        "5. 请仅输出一个数字（如：3.5、5、0），不要包含任何其他文字、解释或单位。\n\n" +
                        "【你的评分】\n",
                t, ref, stu, score
        );
    }

    public static BigDecimal getAIScoreWithRetry(String prompt, int maxRetries) {
        Pattern numberPattern = Pattern.compile("^\\d+(\\.\\d+)?$"); // 匹配 "5" 或 "4.5" 等

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String response = ChatGLM_N.inquire(prompt, false);
                if (response == null) {
                    System.out.println("AI 返回 null，第 " + (attempt + 1) + " 次重试...");
                    continue;
                }

                // 清理响应：去空格、去中文标点、只保留数字和小数点
                String cleaned = response.trim()
                        .replaceAll("[^\\d.]", "") // 移除所有非数字和非小数点字符
                        .replaceAll("\\.{2,}", "."); // 防止多个点如 "4..5"

                // 处理空或无效
                if (cleaned.isEmpty()) {
                    System.out.println("AI 返回非数字内容: \"" + response + "\"，第 " + (attempt + 1) + " 次重试...");
                    continue;
                }

                // 验证是否为有效数字格式（防止 "12.34.56"）
                if (!numberPattern.matcher(cleaned).matches()) {
                    System.out.println("AI 返回格式错误: \"" + response + "\" -> cleaned: \"" + cleaned + "\"，第 " + (attempt + 1) + " 次重试...");
                    continue;
                }

                // 成功解析
                return new BigDecimal(cleaned);

            } catch (Exception e) {
                System.out.println("调用 AI 评分出错（第 " + (attempt + 1) + " 次）: " + e.getMessage());
                if (attempt == maxRetries) {
                    e.printStackTrace();
                }
            }
        }

        // 所有重试失败
        System.out.println("❌ AI 评分多次失败，无法获取有效分数。");
        return null; // 或者你可以抛异常：throw new RuntimeException("AI 评分失败");
    }

    // 标准化单条分录为唯一字符串（用于比对）
    private String normalizeSingleEntry(Map<String, Object> entry) {
        if (entry == null) return null;

        String debitAcc = getString(entry, "debitAccount");
        String creditAcc = getString(entry, "creditAccount");
        String debitAmtStr = getString(entry, "debitAmount");
        String creditAmtStr = getString(entry, "creditAmount");

        if (debitAcc == null || creditAcc == null || debitAmtStr == null || creditAmtStr == null) {
            return null;
        }

        // 账户名统一小写 + 去空格
        debitAcc = debitAcc.trim().toLowerCase();
        creditAcc = creditAcc.trim().toLowerCase();

        // 金额清洗并标准化
        BigDecimal debitAmt = parseAmount(debitAmtStr);
        BigDecimal creditAmt = parseAmount(creditAmtStr);

        // 借贷不平衡的单条分录视为无效（理论上不应出现）
        if (debitAmt.compareTo(creditAmt) != 0) {
            return null; // 或保留？根据业务决定
        }

        // 统一格式：去掉尾随零，确保 200000.00 == 200000
        debitAmt = debitAmt.stripTrailingZeros();
        creditAmt = creditAmt.stripTrailingZeros();

        return String.format("%s|%s|%s|%s", debitAcc, creditAcc, debitAmt.toPlainString(), creditAmt.toPlainString());
    }

    // 安全获取字符串
    private String getString(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        return (obj != null) ? obj.toString().trim() : null;
    }

    // 解析金额（支持 200,000.00）
    private BigDecimal parseAmount(String amountStr) {
        String clean = amountStr.replaceAll("[,，\\s]", "");
        return new BigDecimal(clean);
    }
}
